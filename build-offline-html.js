#!/usr/bin/env node
"use strict";

/**
 * Eaglercraft 26.2 — Offline HTML Builder
 * Generates standalone HTML files for both WASM-GC and JS clients
 * Optimized for LOW memory usage
 *
 * Usage:
 *   node build-offline-html.js [options]
 *
 * Options:
 *   --target=wasm-gc|js|both    Build target (default: both)
 *   --output-dir=PATH           Output directory (default: ./dist)
 *   --fast                      Fast build (less optimization, lower memory)
 *   --skip-assets               Skip asset packaging (reuse existing)
 *   --memory-mib=N              Memory limit in MiB (default: 6144)
 */

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repo = path.resolve(__dirname);
const args = new Set(process.argv.slice(2));

// Parse arguments
const target = (() => {
  const t = process.argv.find(a => a.startsWith("--target="))?.split("=")[1];
  return t || "both";
})();

const outputDir = path.resolve(repo, process.argv.find(a => a.startsWith("--output-dir="))?.split("=")[1] || "dist");
const fastBuild = args.has("--fast");
const skipAssets = args.has("--skip-assets");
const memoryMiB = Number(process.argv.find(a => a.startsWith("--memory-mib="))?.split("=")[1] || "6144");

function run(command, commandArgs, options = {}) {
  console.log(`[build-html] ${command} ${commandArgs.join(" ")}`);
  const result = spawnSync(command, commandArgs, {
    cwd: repo,
    stdio: "inherit",
    env: { ...process.env, ...options.env },
    ...options
  });
  if (result.error) throw new Error(result.error.message);
  if (result.status !== 0) throw new Error(`${command} exited with code ${result.status}`);
  return result;
}

function runNode(script, scriptArgs, extraEnv = {}) {
  return run(process.execPath, [script, ...scriptArgs], { env: extraEnv });
}

// Memory-capped runner
function runCapped(command, commandArgs, limitMiB, extraEnv = {}) {
  const capRunner = path.join(repo, "wasm-toolchain", "run-memory-capped.js");
  return run(process.execPath, [
    capRunner,
    "--limit-mib", String(limitMiB),
    command,
    ...commandArgs
  ], { env: { ...process.env, ...extraEnv } });
}

async function main() {
  console.log(`[build-html] Building offline HTML for target: ${target}`);
  console.log(`[build-html] Output directory: ${outputDir}`);
  console.log(`[build-html] Fast build: ${fastBuild}`);
  console.log(`[build-html] Memory limit: ${memoryMiB} MiB`);
  console.log(`[build-html] Skip assets: ${skipAssets}`);

  fs.mkdirSync(outputDir, { recursive: true });

  const wasmGcOutput = path.join(outputDir, "eaglercraft-26.2-wasm-gc.html");
  const jsOutput = path.join(outputDir, "eaglercraft-26.2-js.html");

  try {
    // Step 1: Build assets (if not skipped)
    if (!skipAssets) {
      console.log("\n[build-html] === Building assets ===");
      run("./gradlew", [
        ":target_lwjgl_desktop:buildAssetsEPK",
        "--console=plain",
        "--no-daemon",
        "--max-workers=1",
        "-Dorg.gradle.parallel=false",
        "-Dorg.gradle.jvmargs=-Xmx1536m -Xss4m -XX:MaxMetaspaceSize=384m -XX:+UseSerialGC"
      ]);
    }

    // Step 2: Build WASM-GC client
    if (target === "wasm-gc" || target === "both") {
      console.log("\n[build-html] === Building WASM-GC client ===");

      // Build client WASM
      runNode("wasm-toolchain/link-web-target-standalone.js", [
        "client",
        fastBuild ? "--fast-analysis" : "",
        fastBuild ? "--diagnostic" : ""
      ].filter(Boolean), {
        EAGLER_LINK_HEAP_MIB: fastBuild ? "8192" : "10240"
      });

      // Build mesh worker
      runNode("wasm-toolchain/link-web-target-standalone.js", [
        "mesh"
      ], {
        EAGLER_LINK_HEAP_MIB: "8192"
      });

      // Build server worker
      runNode("wasm-toolchain/link-server-standalone.js", [], {
        EAGLER_LINK_HEAP_MIB: "8192"
      });

      // Package standalone HTML
      runNode("wasm-toolchain/build-single-html.js", [
        fastBuild ? "--fast-package" : "",
        "--output", wasmGcOutput,
        fastBuild ? "--reuse-client" : "",
        fastBuild ? "--reuse-mesh" : "",
        fastBuild ? "--reuse-server" : "",
        "--skip-build"
      ].filter(Boolean), {
        EAGLER_BUILD_CAP_MIB: String(memoryMiB)
      });

      console.log(`[build-html] WASM-GC HTML: ${wasmGcOutput}`);
    }

    // Step 3: Build JS client
    if (target === "js" || target === "both") {
      console.log("\n[build-html] === Building JS client ===");

      run("./gradlew", [
        ":target_teavm:teavmBuild",
        "--console=plain",
        "--no-daemon",
        "--max-workers=1",
        "-Dorg.gradle.parallel=false",
        fastBuild ? "-PeaglerFastLink=true" : "",
        fastBuild ? "-PeaglerIncrementalSimple=true" : "",
        "-Dorg.gradle.jvmargs=-Xmx1536m -Xss4m -XX:MaxMetaspaceSize=384m -XX:+UseSerialGC"
      ].filter(Boolean));

      // Copy JS output to dist
      const jsSource = path.join(repo, "target_teavm", "build", "teavm", "classes.js");
      const jsDest = path.join(outputDir, "classes.js");
      if (fs.existsSync(jsSource)) {
        fs.copyFileSync(jsSource, jsDest);
        console.log(`[build-html] JS client: ${jsDest}`);
      }

      // Create simple HTML wrapper for JS client
      const jsHtml = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Eaglercraft 26.2 (JS)</title>
  <style>
    body { margin: 0; background: #000; overflow: hidden; }
    canvas { display: block; width: 100vw; height: 100vh; }
  </style>
</head>
<body>
  <canvas id="eagler-canvas"></canvas>
  <script src="classes.js"></script>
  <script>
    // Eaglercraft JS client initialization
    if (typeof Module !== 'undefined') {
      Module.canvas = document.getElementById('eagler-canvas');
    }
  </script>
</body>
</html>`;
      fs.writeFileSync(jsOutput, jsHtml);
      console.log(`[build-html] JS HTML: ${jsOutput}`);
    }

    // Step 4: Copy assets to output directory
    if (!skipAssets) {
      console.log("\n[build-html] === Copying assets ===");
      const assetsSource = path.join(repo, "target_teavm", "build", "web");
      const assetsDest = path.join(outputDir, "assets");
      if (fs.existsSync(assetsSource)) {
        fs.cpSync(assetsSource, assetsDest, { recursive: true });
        console.log(`[build-html] Assets copied to: ${assetsDest}`);
      }
    }

    console.log("\n[build-html] === Build complete ===");
    console.log(`[build-html] Output directory: ${outputDir}`);
    if (target === "wasm-gc" || target === "both") {
      console.log(`[build-html]   WASM-GC: ${path.basename(wasmGcOutput)}`);
    }
    if (target === "js" || target === "both") {
      console.log(`[build-html]   JS: ${path.basename(jsOutput)}`);
    }
    console.log(`[build-html] Open the HTML files in a browser to play (requires HTTP server for WASM)`);

  } catch (error) {
    console.error(`[build-html] ERROR: ${error.message}`);
    process.exit(1);
  }
}

main();