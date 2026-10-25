#!/usr/bin/env bash
# Build the standalone WASM-GC HTML client, sized to the RAM this machine has free.
#
# This is the supported low-memory entry point. It checks everything that could
# fail late (Java, Node, audio archives, free memory) before the long links start,
# then runs wasm-toolchain/build-single-html.js, which links the client, mesh and
# server images one at a time, assembles the web folder, compresses and packages.
# The Java heap, process-tree cap and compression jobs come from
# wasm-toolchain/memory-plan.js, so a 12 GiB laptop and a 64 GiB workstation both
# get limits that fit. The output is the same release-quality precise link.
#
# Usage: ./build-wasm-gc-html.sh [options]
#   --output PATH      HTML file to write (default: eaglercraft-26.2-wasm-gc.html)
#   --check            Check the machine and print the memory plan; build nothing
#   --heap-mib N       Force the Java link heap (4096-12544 MiB); see GUIDE.md first
#   --fast-package     Faster, larger packaging (the HTML may exceed 76 MB)
#   --with-music       Embed the music in the HTML instead of a separate ZIP
#   -h, --help         Show this help
#
# If a link runs out of memory, free memory (or add swap) and run the same command
# again: links that already finished are cached, so only the failed one is redone.

set -euo pipefail

OUTPUT="eaglercraft-26.2-wasm-gc.html"
CHECK_ONLY=0
HEAP_MIB=""
PASSTHROUGH=()

usage() { sed -n '2,/^$/p' "$0" | sed 's/^# \{0,1\}//'; }
die() { printf 'error: %s\n' "$*" >&2; exit 2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --output)        [ $# -ge 2 ] || die "--output needs a path"; OUTPUT="$2"; shift 2 ;;
    --check)         CHECK_ONLY=1; shift ;;
    --heap-mib)
      [ $# -ge 2 ] || die "--heap-mib needs a number"
      case "$2" in ''|*[!0-9]*) die "--heap-mib must be a whole number of MiB" ;; esac
      HEAP_MIB="$2"; shift 2 ;;
    --fast-package)  PASSTHROUGH+=("--fast-package"); shift ;;
    --with-music)    PASSTHROUGH+=("--with-music"); shift ;;
    -h|--help)       usage; exit 0 ;;
    *)               die "unknown option: $1 (try --help)" ;;
  esac
done

# build-single-html.js resolves --output against the project folder, so make a
# relative path relative to where the user ran this, before changing directory.
case "$OUTPUT" in /*) ;; *) OUTPUT="$PWD/$OUTPUT" ;; esac

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

PROBLEMS=0
problem() { printf '  [x] %s\n' "$*" >&2; PROBLEMS=$((PROBLEMS + 1)); }
note()    { printf '  [!] %s\n' "$*" >&2; }
ok()      { printf '  [ok] %s\n' "$*"; }

echo "== Checking this machine =="

# The memory cap and plan read /proc, so this needs Linux (WSL2 on Windows works).
if [ "$(uname -s)" != "Linux" ]; then
  problem "This build needs Linux (it reads /proc). On Windows use WSL2; on macOS use a Linux VM."
fi

[ -f wasm-toolchain/build-single-html.js ] || problem "Run this from the project folder; wasm-toolchain/build-single-html.js is missing."
[ -d game/src/main/java ] || problem "game/src/main/java is missing. Create the source project with the patcher first (see GUIDE.md)."

# Node
if command -v node >/dev/null 2>&1; then
  NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
  if [ "${NODE_MAJOR:-0}" -ge 18 ]; then ok "Node.js $(node -v)"; else problem "Node.js 18 or newer is required (found $(node -v))."; fi
else
  problem "Node.js is not on PATH."
fi

# Java 25. The linker is started as plain 'java', so the one on PATH must be 25+.
java_major() {
  # JAVA_TOOL_OPTIONS makes the JVM print a "Picked up ..." line first, so match the version line only.
  java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\)[."].*/\1/p' | head -n 1
}
if command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1; then
  JAVA_MAJOR="$(java_major)"
  if [ "${JAVA_MAJOR:-0}" -ge 25 ]; then
    ok "Java ${JAVA_MAJOR}"
  else
    problem "Java 25 is required on PATH (found ${JAVA_MAJOR:-unknown}). Set JAVA_HOME and PATH to a JDK 25."
  fi
else
  problem "java and javac (JDK 25) must be on PATH."
fi

command -v unzip >/dev/null 2>&1 && ok "unzip" || problem "unzip is required (it extracts the TeaVM runtime). Install it with your package manager."

# The packager needs these two archives, but only notices after the links (30+ minutes).
for epk in sounds.epk music.epk; do
  if [ -s "target_teavm/build/web/$epk" ]; then
    ok "target_teavm/build/web/$epk"
  else
    problem "target_teavm/build/web/$epk is missing. Create it with the patcher's 'Get game audio' (needs FFmpeg 6.1.1) or copy it from an earlier build of this project."
  fi
done

# Brotli decoder used to embed the payloads.
if [ -d node_modules/brotli-dec-wasm/pkg ]; then
  ok "node_modules/brotli-dec-wasm"
else
  note "node_modules/brotli-dec-wasm is missing; it will be installed with 'npm ci --ignore-scripts'."
  command -v npm >/dev/null 2>&1 || problem "npm is required to install the Brotli decoder."
fi

# Disk. Approximate: Gradle caches, three Wasm images, their compressed copies and the HTML.
FREE_KIB="$(df -Pk "$REPO_ROOT" 2>/dev/null | awk 'NR==2 {print $4}')"
if [ -n "${FREE_KIB:-}" ] && [ "$FREE_KIB" -lt $((6 * 1024 * 1024)) ]; then
  note "Only $((FREE_KIB / 1024 / 1024)) GiB of disk is free here; about 6 GiB is a safe minimum (approximate)."
fi

# These variables make the link cache refuse to reuse or record results, so a
# retry after an out-of-memory failure would redo every link.
for name in JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS JDK_JAVAC_OPTIONS _JAVA_OPTIONS NODE_OPTIONS; do
  if [ -n "${!name:-}" ]; then
    note "$name is set. The link cache ignores results while it is set, so a retry would redo every link. Unset it for this build."
  fi
done

if [ -n "$HEAP_MIB" ]; then
  export EAGLER_LINK_HEAP_MIB="$HEAP_MIB"
  export EAGLER_SERVER_LINK_HEAP_MIB="$HEAP_MIB"
fi

echo
echo "== Memory plan =="
if command -v node >/dev/null 2>&1; then
  node wasm-toolchain/memory-plan.js --report || problem "Not enough free memory for the link (details above)."
fi

if [ "$PROBLEMS" -gt 0 ]; then
  echo >&2
  echo "Found $PROBLEMS problem(s) above. Fix them and run this again." >&2
  exit 1
fi

if [ "$CHECK_ONLY" -eq 1 ]; then
  echo
  echo "Checks passed. Nothing was built (--check)."
  exit 0
fi

if [ ! -d node_modules/brotli-dec-wasm/pkg ]; then
  echo
  echo "== Installing the Brotli decoder =="
  npm ci --ignore-scripts
fi

# Idle Gradle daemons from earlier commands each hold gigabytes. The build runs
# its own single-use Gradle, so stop the rest before the big link starts.
echo
echo "== Freeing memory from idle Gradle daemons =="
./gradlew --stop >/dev/null 2>&1 || true

echo
echo "== Building $OUTPUT =="
echo "Allow an hour or more on a small machine; the documented time on a capable PC is 30-42 minutes."
START="$(date +%s)"
if node wasm-toolchain/build-single-html.js --output "$OUTPUT" ${PASSTHROUGH[@]+"${PASSTHROUGH[@]}"}; then
  STATUS=0
else
  STATUS=$?
fi
ELAPSED=$(( $(date +%s) - START ))

echo
if [ "$STATUS" -ne 0 ]; then
  echo "Build failed after $((ELAPSED / 60)) min (exit $STATUS)." >&2
  echo "If it was memory: free some (close browsers, './gradlew --stop') or add swap, then run the same command." >&2
  echo "Links that finished are cached, so only the failed step is redone. See GUIDE.md, 'Building with under 16 GiB of RAM'." >&2
  exit "$STATUS"
fi

if [ ! -s "$OUTPUT" ]; then
  echo "The build reported success but $OUTPUT is missing or empty." >&2
  exit 1
fi

SIZE_BYTES="$(wc -c < "$OUTPUT" | tr -d ' ')"
echo "Done in $((ELAPSED / 60)) min: $OUTPUT ($((SIZE_BYTES / 1000000)) MB)"
case " ${PASSTHROUGH[*]:-} " in
  *" --with-music "*) ;;
  *) echo "Music is a separate optional pack: $REPO_ROOT/eaglercraft-26.2-optional-music.zip (import it from Minecraft's Resource Packs menu)." ;;
esac
echo "Open the HTML directly in a Chromium-based browser. Test the title screen, a world and saving before sharing it."
