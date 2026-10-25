#!/usr/bin/env node
"use strict";

// Memory planning for the standalone Wasm-GC links and packaging.
//
// The original scripts assumed a 16 GiB machine: fixed 12 GiB linker heaps, an
// 8 GiB minimum and a 13,312 MiB process-tree cap, whatever the machine had. On a
// 10-15 GiB computer that ends in the kernel OOM killer or heavy swapping after
// the first half hour. This module sizes those limits from the memory that is
// actually free, and explains a failed link instead of leaving a bare exit code.
//
// Output never depends on the heap size, so none of this changes the Wasm.
//
// Tiers, by Java heap size:
//   full          >= 12288 MiB  the original default; flags are unchanged.
//   supported     8192-12287    inside the range the original scripts accepted.
//                               Only the -Xmx number changes.
//   experimental  < 8192        below anything the original project tested. The
//                               young generation is capped so the old generation,
//                               which holds the dependency graph, keeps the
//                               capacity it has at 8192 MiB.
//
// Why the young-generation cap works. Parallel GC splits the heap one third young
// and two thirds old. At 8192 MiB the old generation is therefore about 5461 MiB.
// Capping young at one sixth of the heap gives the old generation the same 5461 MiB
// at about 6554 MiB total, so the automatic floor is 6656 MiB. This is derived
// from the original 8192 MiB floor; it has NOT been measured. The peak resident
// size printed by run-memory-capped.js after each link is the real number.

const fs = require("node:fs");
const os = require("node:os");

const MAX_CAP_MIB = 13312;        // the original hard cap for a process tree
const JVM_OVERHEAD_MIB = 2560;    // non-heap JVM memory plus room for the OS and other programs
const FULL_HEAP_MIB = 12288;
const TESTED_MIN_HEAP_MIB = 8192; // lowest heap the original scripts accepted
const AUTO_MIN_HEAP_MIB = 6656;   // lowest heap chosen automatically (see the note above)
const ABSOLUTE_MIN_HEAP_MIB = 4096;
const ROUND_MIB = 256;

// Defaults used by the link scripts. link-server-standalone.js keeps its own
// copy of the server value as a text anchor for external tooling.
const TARGETS = {
  client: { envName: "EAGLER_LINK_HEAP_MIB", defaultHeapMiB: 12288, maxHeapMiB: 12544 },
  mesh: { envName: "EAGLER_LINK_HEAP_MIB", defaultHeapMiB: 12288, maxHeapMiB: 12544 },
  server: { envName: "EAGLER_SERVER_LINK_HEAP_MIB", defaultHeapMiB: 12544, maxHeapMiB: 12544 },
};

const gib = mib => (mib / 1024).toFixed(1);

function cgroupLimitMiB(root = "/sys/fs/cgroup") {
  const candidates = [`${root}/memory.max`, `${root}/memory/memory.limit_in_bytes`];
  for (const file of candidates) {
    try {
      const text = fs.readFileSync(file, "utf8").trim();
      if (/^\d+$/.test(text) && Number(text) < 2 ** 40) return Math.floor(Number(text) / 1048576);
    } catch (_) { /* Not in a limited cgroup, or not Linux. */ }
  }
  return null;
}

// Returns null when memory cannot be measured (not Linux); callers then keep the
// original fixed defaults. EAGLER_USABLE_MEM_MIB overrides the measurement, for
// containers whose /proc/meminfo is wrong and for dry runs.
function readMemory(env = process.env, procRoot = "/proc", cgroupRoot = "/sys/fs/cgroup") {
  const forced = env.EAGLER_USABLE_MEM_MIB;
  if (forced !== undefined && forced !== "") {
    if (!/^\d+$/.test(forced) || Number(forced) < 1) {
      throw new Error("EAGLER_USABLE_MEM_MIB must be a positive integer");
    }
    return { usableMiB: Number(forced), totalMiB: null, swapTotalMiB: null, swapFreeMiB: null,
      source: "EAGLER_USABLE_MEM_MIB" };
  }
  let text;
  try { text = fs.readFileSync(`${procRoot}/meminfo`, "utf8"); } catch (_) { return null; }
  const kib = name => {
    const match = new RegExp(`^${name}:\\s+(\\d+)\\s+kB$`, "m").exec(text);
    return match ? Number(match[1]) : null;
  };
  const total = kib("MemTotal");
  if (total === null) return null;
  const available = kib("MemAvailable") ?? ((kib("MemFree") ?? 0) + (kib("Cached") ?? 0));
  let usableMiB = Math.floor(available / 1024);
  const limit = cgroupLimitMiB(cgroupRoot);
  if (limit !== null) usableMiB = Math.min(usableMiB, limit);
  return {
    usableMiB,
    totalMiB: Math.floor(total / 1024),
    swapTotalMiB: Math.floor((kib("SwapTotal") ?? 0) / 1024),
    swapFreeMiB: Math.floor((kib("SwapFree") ?? 0) / 1024),
    source: "/proc/meminfo",
  };
}

function refusalMessage(plan, memory) {
  const needHeap = TESTED_MIN_HEAP_MIB;
  const needTotal = needHeap + 1536;
  const swapGiB = Math.max(4, Math.ceil((needTotal - memory.usableMiB) / 1024));
  return [
    "Not enough free memory for the Wasm-GC link.",
    `  Free now : ${memory.usableMiB} MiB (${gib(memory.usableMiB)} GiB)` +
      (memory.swapFreeMiB ? `, plus ${memory.swapFreeMiB} MiB of free swap` : ""),
    `  Needed   : ${AUTO_MIN_HEAP_MIB + JVM_OVERHEAD_MIB} MiB free for the smallest automatic heap (${AUTO_MIN_HEAP_MIB} MiB),`,
    `             or ${needHeap + JVM_OVERHEAD_MIB} MiB for the ${needHeap} MiB heap the original project tested.`,
    `             (heap + ${JVM_OVERHEAD_MIB} MiB for the JVM, the OS and other programs)`,
    "Options:",
    "  1. Close browsers and IDEs, run './gradlew --stop', then try again.",
    `  2. Add about ${swapGiB} GiB of swap, then run './build-wasm-gc-html.sh --heap-mib ${needHeap}'`,
    "     (or set EAGLER_LINK_HEAP_MIB and EAGLER_SERVER_LINK_HEAP_MIB). Expect it to be slow:",
    "     the dependency graph is accessed at random, so pages move in and out of swap.",
    "  3. Build on a machine with more RAM (a cloud VM or CI runner is fine) and copy the HTML back.",
  ].join("\n");
}

function planLinkMemory(kind, options = {}) {
  const target = TARGETS[kind];
  if (!target) throw new Error(`unknown link target: ${kind}`);
  const env = options.env ?? process.env;
  const defaultHeapMiB = options.defaultHeapMiB ?? target.defaultHeapMiB;
  const maxHeapMiB = options.maxHeapMiB ?? target.maxHeapMiB;
  const minHeapMiB = options.minHeapMiB ?? ABSOLUTE_MIN_HEAP_MIB;
  const envName = options.envName ?? target.envName;
  const memory = options.memory !== undefined ? options.memory : readMemory(env);
  const plan = { kind, envName, memory, warnings: [], error: null };

  const raw = env[envName];
  if (raw !== undefined && raw !== "") {
    const value = Number(raw);
    if (!/^\d+$/.test(raw) || !Number.isSafeInteger(value) || value < minHeapMiB || value > maxHeapMiB) {
      plan.error = `${envName} must be an integer between ${minHeapMiB} and ${maxHeapMiB}`;
      return plan;
    }
    plan.heapMiB = value;
    plan.source = envName;
  } else if (memory) {
    const budget = Math.floor((memory.usableMiB - JVM_OVERHEAD_MIB) / ROUND_MIB) * ROUND_MIB;
    plan.heapMiB = Math.min(defaultHeapMiB, budget);
    plan.source = `auto from ${memory.usableMiB} MiB free`;
    if (plan.heapMiB < AUTO_MIN_HEAP_MIB) {
      plan.error = refusalMessage(plan, memory);
      return plan;
    }
  } else {
    plan.heapMiB = defaultHeapMiB;
    plan.source = "default (memory could not be measured)";
  }

  const heap = plan.heapMiB;
  plan.tier = heap >= FULL_HEAP_MIB ? "full" : heap >= TESTED_MIN_HEAP_MIB ? "supported" : "experimental";
  plan.youngMiB = plan.tier === "experimental"
    ? Math.min(2048, Math.max(768, Math.round(heap / 6 / 64) * 64)) : null;
  plan.capMiB = Math.min(MAX_CAP_MIB, heap + (heap >= FULL_HEAP_MIB ? 1024 : 1536));
  const cpus = typeof os.availableParallelism === "function" ? os.availableParallelism() : os.cpus().length;
  plan.gcThreads = Math.max(1, Math.min(4, cpus));

  if (plan.tier === "experimental") {
    plan.warnings.push(
      `Java heap ${heap} MiB is below the ${TESTED_MIN_HEAP_MIB} MiB the original project tested. ` +
      `The young generation is capped at ${plan.youngMiB} MiB to protect the old generation. ` +
      "This has not been measured; the peak RSS reported after the link shows the real need.");
  }
  if (memory && heap + 1024 > memory.usableMiB) {
    plan.warnings.push(
      `${heap} MiB of heap plus JVM overhead needs about ${heap + 1024} MiB, but only ` +
      `${memory.usableMiB} MiB is free. The link will lean on swap and may be very slow or be killed by the OS.`);
  }
  return plan;
}

// Brotli at quality 11 over a 100+ MiB module is memory hungry, and the original
// packaging runs three at a time. One at a time costs minutes, not gigabytes.
function planPackaging(options = {}) {
  const env = options.env ?? process.env;
  const memory = options.memory !== undefined ? options.memory : readMemory(env);
  if (env.EAGLER_COMPRESS_JOBS) return { compressJobs: null, memory };
  return { compressJobs: memory && memory.usableMiB < FULL_HEAP_MIB ? 1 : null, memory };
}

// The Gradle JVM that compiles the game module. The original used 4 GiB with G1.
function gradleJvmArgs(options = {}) {
  const memory = options.memory !== undefined ? options.memory : readMemory(options.env ?? process.env);
  return memory && memory.usableMiB < FULL_HEAP_MIB
    ? "-Xmx3072m -Xss4m -XX:MaxMetaspaceSize=768m -XX:+UseSerialGC"
    : "-Xmx4096m -Xss4m -XX:MaxMetaspaceSize=768m -XX:+UseG1GC";
}

// Cap for a whole step (Gradle, then the linker, then Node). The linker's own cap
// is inside this one, so this must be a little higher to avoid false kills.
function planBuildCap(options = {}) {
  const env = options.env ?? process.env;
  const memory = options.memory !== undefined ? options.memory : readMemory(env);
  let highest = 0;
  for (const kind of ["client", "mesh", "server"]) {
    const plan = planLinkMemory(kind, { env, memory });
    if (plan.error) return MAX_CAP_MIB; // the link script itself will refuse with a clear message
    highest = Math.max(highest, plan.capMiB);
  }
  return Math.min(MAX_CAP_MIB, highest + 512);
}

function readTail(file, bytes) {
  const fd = fs.openSync(file, "r");
  try {
    const size = fs.fstatSync(fd).size;
    const length = Math.min(size, bytes);
    const buffer = Buffer.alloc(length);
    fs.readSync(fd, buffer, 0, length, size - length);
    return buffer.toString("utf8");
  } finally {
    fs.closeSync(fd);
  }
}

// Called when a link exits non-zero. Silent unless memory was the cause.
function explainLinkFailure({ plan, status, logFile, label }) {
  let tail = "";
  try { tail = readTail(logFile, 4 * 1024 * 1024); } catch (_) { /* no log */ }
  const outOfHeap = /OutOfMemoryError/.test(tail);
  const overCap = status === 137;
  if (!outOfHeap && !overCap) return;
  const reason = outOfHeap
    ? `the Java heap of ${plan.heapMiB} MiB was too small`
    : `the process tree went over its ${plan.capMiB} MiB limit`;
  console.error([
    "",
    `[${label}] The link ran out of memory: ${reason}.`,
    "  - Links that already finished are cached (not while JAVA_TOOL_OPTIONS or NODE_OPTIONS is set).",
    "    Run the same command again after fixing memory; only the failed link is redone.",
    "  - Free memory: close browsers and IDEs and run './gradlew --stop'.",
    `  - Raise the heap if RAM allows: ${plan.envName}=<MiB> (currently ${plan.heapMiB}, source: ${plan.source}).`,
    "  - Otherwise add swap (slow), or build on a machine with more RAM. See GUIDE.md, 'Building with under 16 GiB of RAM'.",
    "",
  ].join("\n"));
}

function describe(plan) {
  if (plan.error) return plan.error;
  const parts = [`heap ${plan.heapMiB} MiB`];
  if (plan.youngMiB) parts.push(`young generation ${plan.youngMiB} MiB`);
  parts.push(`process-tree cap ${plan.capMiB} MiB`, `tier ${plan.tier}`, `source: ${plan.source}`);
  return parts.join(", ");
}

function report(env = process.env) {
  const memory = readMemory(env);
  if (memory) {
    console.log(`[memory-plan] free memory: ${memory.usableMiB} MiB (${gib(memory.usableMiB)} GiB), source ${memory.source}`);
    if (memory.totalMiB !== null) {
      console.log(`[memory-plan] total RAM ${memory.totalMiB} MiB, swap ${memory.swapTotalMiB} MiB ` +
        `(${memory.swapFreeMiB} MiB free)`);
    }
  } else {
    console.log("[memory-plan] free memory could not be measured here; the original fixed limits apply");
  }
  let refused = false;
  let printedRefusal = null;
  const printedWarnings = new Set();
  for (const kind of ["client", "mesh", "server"]) {
    const plan = planLinkMemory(kind, { env, memory });
    console.log(`[memory-plan] ${kind} link: ${plan.error ? "REFUSED" : describe(plan)}`);
    if (plan.error) {
      refused = true;
      // The reason is the same for every link; print it once.
      if (plan.error !== printedRefusal) {
        console.log(plan.error.split("\n").map(line => `    ${line}`).join("\n"));
        printedRefusal = plan.error;
      }
    }
    for (const warning of plan.warnings) {
      if (!printedWarnings.has(warning)) { printedWarnings.add(warning); console.log(`[memory-plan] warning: ${warning}`); }
    }
  }
  const packaging = planPackaging({ env, memory });
  console.log(`[memory-plan] packaging: ${packaging.compressJobs ? "1 compression job at a time" : "default compression jobs"}`);
  return refused ? 2 : 0;
}

module.exports = {
  readMemory, planLinkMemory, planPackaging, planBuildCap, gradleJvmArgs, explainLinkFailure, describe,
  TARGETS, MAX_CAP_MIB, TESTED_MIN_HEAP_MIB, AUTO_MIN_HEAP_MIB, JVM_OVERHEAD_MIB,
};

if (require.main === module) {
  if (process.argv[2] !== "--report") {
    console.error("usage: memory-plan.js --report");
    process.exit(2);
  }
  try {
    process.exit(report());
  } catch (error) {
    console.error(`[memory-plan] ${error.message}`);
    process.exit(2);
  }
}
