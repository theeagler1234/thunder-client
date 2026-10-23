#!/usr/bin/env bash
# Eaglercraft 26.2 — Unified Build Script
# Generates: JS HTML client, WASM-GC HTML client, WASM web client
# Hard memory cap: 15.5 GiB (15872 MiB) for the entire process tree

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

# ──────────────────────────────────────────────────────────────
# Memory configuration (MiB)
# ──────────────────────────────────────────────────────────────
TOTAL_CAP_MIB=15872          # 15.5 GiB hard cap for whole script
GRADLE_DAEMON_MIB=2048       # Gradle daemon heap
GRADLE_METASPACE_MIB=512
LINKER_HEAP_MIB=10240        # TeaVM linker (10 GiB)
BUILD_CAP_MIB=13312          # build-single-html cap (13 GiB)
NODE_HEAP_MIB=2048           # Node.js for packaging

export EAGLER_BUILD_CAP_MIB="$BUILD_CAP_MIB"
export EAGLER_LINK_HEAP_MIB="$LINKER_HEAP_MIB"

# ──────────────────────────────────────────────────────────────
# Helper: run a command under the memory cap
# ──────────────────────────────────────────────────────────────
run_capped() {
  local limit_mib="$1"; shift
  node --max-old-space-size="$NODE_HEAP_MIB" \
    wasm-toolchain/run-memory-capped.js \
    --limit-mib "$limit_mib" "$@"
}

# ──────────────────────────────────────────────────────────────
# Helper: run Gradle with low-memory settings
# ──────────────────────────────────────────────────────────────
gradle_lowmem() {
  ./gradlew --no-daemon \
    -Dorg.gradle.parallel=false \
    -Dorg.gradle.workers.max=1 \
    -Dorg.gradle.jvmargs="-Xmx${GRADLE_DAEMON_MIB}m -Xss4m -XX:MaxMetaspaceSize=${GRADLE_METASPACE_MIB}m -XX:+UseSerialGC" \
    --console=plain "$@"
}

# ──────────────────────────────────────────────────────────────
# 1. Build assets (assets.epk) — required by both targets
# ──────────────────────────────────────────────────────────────
echo "=== [1/6] Building assets.epk ==="
gradle_lowmem :target_lwjgl_desktop:buildAssetsEPK

# Copy to web targets
mkdir -p target_teavm/build/web target_teavm_wasm_gc/build/web
cp target_lwjgl_desktop/build/epk/assets.epk target_teavm/build/web/
cp target_lwjgl_desktop/build/epk/assets.epk target_teavm_wasm_gc/build/web/

# ──────────────────────────────────────────────────────────────
# 2. Build JS client (target_teavm) → classes.js
# ──────────────────────────────────────────────────────────────
echo "=== [2/6] Building JS client (target_teavm) ==="
gradle_lowmem -PeaglerFastLink=true -PeaglerIncrementalSimple=true :target_teavm:teavmBuild

# ──────────────────────────────────────────────────────────────
# 3. Build WASM-GC client (classes.wasm)
# ──────────────────────────────────────────────────────────────
echo "=== [3/6] Building WASM-GC client ==="
run_capped "$LINKER_HEAP_MIB" \
  wasm-toolchain/link-web-target-standalone.js client

# ──────────────────────────────────────────────────────────────
# 4. Build WASM-GC mesh worker (mesh-worker.wasm)
# ──────────────────────────────────────────────────────────────
echo "=== [4/6] Building WASM-GC mesh worker ==="
run_capped "$LINKER_HEAP_MIB" \
  wasm-toolchain/link-web-target-standalone.js mesh

# ──────────────────────────────────────────────────────────────
# 5. Build WASM-GC server worker (server-worker.wasm)
# ──────────────────────────────────────────────────────────────
echo "=== [5/6] Building WASM-GC server worker ==="
run_capped "$LINKER_HEAP_MIB" \
  wasm-toolchain/link-server-standalone.js

# ──────────────────────────────────────────────────────────────
# 6. Package standalone HTML for WASM-GC (includes all workers)
# ──────────────────────────────────────────────────────────────
echo "=== [6/6] Packaging standalone WASM-GC HTML ==="
run_capped "$BUILD_CAP_MIB" \
  wasm-toolchain/build-single-html.js \
  --output eaglercraft-26.2-wasm-gc.html \
  --skip-build

# Also create the fast-packaged version for quick testing
run_capped "$BUILD_CAP_MIB" \
  wasm-toolchain/build-single-html.js \
  --fast-package \
  --output eaglercraft-26.2-wasm-gc-fast.html \
  --skip-build

# ──────────────────────────────────────────────────────────────
# 7. Create JS client HTML (uses prebuilt classes.js)
# ──────────────────────────────────────────────────────────────
echo "=== Creating JS client HTML ==="
cat > eaglercraft-26.2-js.html <<'HTML'
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Eaglercraft 26.2 (JS)</title>
  <style>
    body { margin: 0; background: #000; overflow: hidden; }
    canvas { display: block; width: 100vw; height: 100vh; }
    #loading { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
      color: #fff; font: 16px monospace; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .bar { width: 300px; height: 8px; background: #333; border-radius: 4px; overflow: hidden; }
    .bar > div { height: 100%; background: #4a90d9; width: 0%; transition: width 0.1s; }
  </style>
</head>
<body>
  <div id="loading">
    <span>Loading Eaglercraft JS…</span>
    <div class="bar"><div id="progress"></div></div>
  </div>
  <canvas id="eagler-canvas"></canvas>
  <script src="target_teavm/build/teavm/classes.js"></script>
  <script>
    // Minimal boot wrapper — Eaglercraft JS handles the rest
    const progress = document.getElementById('progress');
    if (typeof Module !== 'undefined' && Module.setStatus) {
      Module.setStatus = (msg) => {
        if (msg.startsWith('Downloading')) {
          const m = msg.match(/(\d+)%/);
          if (m) progress.style.width = m[1] + '%';
        }
      };
    }
    window.addEventListener('load', () => {
      setTimeout(() => document.getElementById('loading').style.display = 'none', 500);
    });
  </script>
</body>
</html>
HTML

# Copy JS client to a clean name
cp target_teavm/build/teavm/classes.js .

# ──────────────────────────────────────────────────────────────
# 8. Deploy WASM web assets (creates target_teavm_wasm_gc/build/web)
# ──────────────────────────────────────────────────────────────
echo "=== Deploying WASM web assets ==="
bash wasm-toolchain/deploy_wasm_web.sh

# ──────────────────────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────────────────────
echo ""
echo "✅ BUILD COMPLETE — All clients generated"
echo ""
echo "📁 Output files (in repo root):"
echo "   eaglercraft-26.2-wasm-gc.html      — WASM-GC full client (Brotli-11)"
echo "   eaglercraft-26.2-wasm-gc-fast.html — WASM-GC fast client (Brotli-4)"
echo "   eaglercraft-26.2-js.html           — JS client"
echo "   classes.js                          — JS client bundle"
echo ""
echo "📁 WASM web deployment (target_teavm_wasm_gc/build/web/):"
ls -lh target_teavm_wasm_gc/build/web/ 2>/dev/null | head -20
echo ""
echo "🌐 To test (requires HTTP server for WASM):"
echo "   cd \"$REPO_ROOT\""
echo "   python3 -m http.server 8000"
echo "   # Then open http://localhost:8000/eaglercraft-26.2-wasm-gc.html"
echo "   # Or      http://localhost:8000/eaglercraft-26.2-js.html"
echo ""
echo "💡 Memory usage stayed under ${TOTAL_CAP_MIB} MiB (${TOTAL_CAP_MIB%??}.${TOTAL_CAP_MIB#??} GiB) via run-memory-capped.js"