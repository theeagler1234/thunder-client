# Building Eaglercraft 26.2

## Choose an output

- Source project extracts and patches the Java source without compiling the client.
- Standalone HTML creates the source project, then builds one playable HTML file. You do not need to extract source first.
- Isolated Web App builds a signed local app bundle. Installation is covered in `iwa/README.md`.

Choose your own project and output paths. A new project folder must be empty.
After a successful run, Open folder shows the result.

The complete Setup JAR unpacks the patch bundle, skeleton, resource overlay
and bundled audio into its installation folder. The patcher finds these
files automatically; you do not need to extract or link the internal ZIPs.
Supply your official Minecraft 26.2 JAR and choose the output paths.
Keep the installation files together. A bare GUI JAR is not a complete kit.

For an existing project, select Standalone HTML and choose its folder. Reuse is
automatic. Choose a new HTML filename. The patcher checks the
project's receipt and keeps your source edits, original receipt and existing
guide. The build receipt records whether the Java source changed.

## Tools, memory and time

Install build tools in the patcher to set up Java and Node locally. The patcher
uses Java 17 for extraction and Java 25 for compilation. Keep the supplied input
files together; their hashes identify the supported version.

If sounds or music are missing, Get game audio can fetch the official sound
objects and rebuild the archives. Install FFmpeg 6.1.1 and select it first;
the patcher does not install FFmpeg. Get official resources rebuilds the six
external files used by the overlay. These actions do not create the source
patch bundle, project skeleton, or overlay supplied with the patcher.

Allow about 30 minutes on a reasonably capable PC. One completed local build
took 41 minutes 46 seconds. Downloads and slower hardware can add time.

HTML and IWA builds need at least a 10 GiB build budget plus memory reserved for
the system. 16 GiB RAM or more is recommended. The patcher measures available
memory, sets Java limits and stops if too little is available. Smaller heaps can
take longer or still run out of heap. Source extraction uses a smaller budget.
Do not run two builds in the same project.

Running this project's own scripts yourself on a computer with less than 16 GiB
of RAM? See "Building with under 16 GiB of RAM" below.

## Building with under 16 GiB of RAM

`build-wasm-gc-html.sh` builds the standalone Wasm-GC HTML and sizes every
memory limit to the RAM that is free on the machine, instead of assuming 16 GiB.
It needs the source project, Java 25 and Node on PATH, `unzip`, and the audio
archives `target_teavm/build/web/sounds.epk` and `music.epk`. Linux only; on
Windows use WSL2.

```sh
./build-wasm-gc-html.sh --check   # inspect this machine and print the plan; builds nothing
./build-wasm-gc-html.sh           # writes eaglercraft-26.2-wasm-gc.html in the current folder
```

| Option | Effect |
| --- | --- |
| `--output PATH` | Where to write the HTML. A relative path is relative to where you run the script. |
| `--check` | Run the checks and show the memory plan only. |
| `--heap-mib N` | Force the Java heap for every link (4096 to 12544 MiB). See below before using it. |
| `--fast-package` | Faster, larger packaging. The HTML may exceed the 76 MB target. |
| `--with-music` | Embed the music instead of writing the separate optional ZIP. |

The script checks the things that would otherwise fail after the links, such as
a missing `sounds.epk`, then stops idle Gradle daemons, which each hold gigabytes,
and runs the normal build. Allow an hour or more on a small machine.

### How memory is sized

The three Wasm links (client, mesh worker, server worker) run one at a time, each
in its own Java process. Each one gets a heap based on the memory that is free
when the build starts (`available` in `free -m`), less about 2.5 GiB for the JVM,
the operating system and your other programs:

| Free memory | Java heap per link | Status |
| --- | --- | --- |
| 14,848 MiB or more | 12,288 MiB (server up to 12,544 MiB) | The original settings, unchanged. |
| 10,752 to 14,847 MiB | 8,192 to 12,287 MiB | Inside the range the original scripts accepted. |
| 9,216 to 10,751 MiB | 6,656 to 7,936 MiB | Experimental: below anything originally tested. |
| Under 9,216 MiB | Refused | Add swap, or build elsewhere. |

The heap only limits memory. It does not change the analysis or the optimizer, so
every row produces the same precise, release-quality link. Also lowered to match:
the process-tree memory cap (it follows the heap instead of a fixed 13,312 MiB),
Gradle's own heap (3 GiB with the serial collector) and Brotli compression, which
runs one file at a time when less than 12 GiB is free.

A link that runs out of heap now exits at once and says so, instead of grinding
through garbage collection for minutes.

What is and is not known: heaps from 8,192 MiB up are in the range the original
project accepted. The experimental rows are derived, not measured. The original
8,192 MiB heap leaves the dependency graph about 5.3 GiB of old-generation space
under the JVM's default split. The experimental rows cap the young generation at a
sixth of the heap so the old generation keeps that space at about 6.6 GiB in total.
After each link the build prints `[memory-cap] peak child-tree RSS=`, which is the
real figure for your machine.

### If it runs out of memory

1. Close browsers and IDEs, run `./gradlew --stop`, and run the same command again.
   Links that finished are cached, so only the failed one is redone. This caching
   is switched off while `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`,
   `JDK_JAVAC_OPTIONS`, `_JAVA_OPTIONS` or `NODE_OPTIONS` is set; unset them for
   the build.
2. Add swap, then force the tested heap. This works but is slow, because the
   dependency graph is read at random and pages move in and out of swap:

   ```sh
   sudo fallocate -l 8G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   ./build-wasm-gc-html.sh --heap-mib 8192
   sudo swapoff /swapfile && sudo rm /swapfile   # when the build is done
   ```

   On btrfs or ZFS, follow your distribution's swap file instructions instead.
3. On WSL2, raise the VM's limits in `%UserProfile%\.wslconfig`, then run
   `wsl --shutdown` and start WSL again:

   ```ini
   [wsl2]
   memory=12GB
   swap=8GB
   ```

4. Build on a machine with more RAM, such as a cloud VM, and copy the HTML back.

### Settings you can override

| Variable | Meaning |
| --- | --- |
| `EAGLER_LINK_HEAP_MIB` | Java heap for the client and mesh links, 4096 to 12544. |
| `EAGLER_SERVER_LINK_HEAP_MIB` | Java heap for the server link, 4096 to 12544. |
| `EAGLER_USABLE_MEM_MIB` | Treat this much memory as free. For containers whose `/proc/meminfo` is wrong. |
| `EAGLER_BUILD_CAP_MIB` | Memory cap for each build step. Defaults to the plan. |
| `EAGLER_COMPRESS_JOBS` | Compression jobs, 1 to 3. |

### Gradle settings

`gradle.properties` keeps the Gradle daemon at 3 GiB with the serial collector,
above the 1.4 to 2.3 GiB that compiling the game module needs. The configuration
cache is left off because the classpath-printing tasks read project state when
they run, and `eaglerFastLink` is not set by default because it turns the Gradle
Wasm-GC task into a non-release build. Do not add `-XX:-UseCompressedOops`: it
doubles the size of every reference and raises memory use.

## What is in the source folder?

| Folder or file | Contents |
| --- | --- |
| `game/src/main/java` | Patched Minecraft game code |
| `game/src/main/resources` | Game resources, when present |
| `platform` | Shared Eaglercraft platform code |
| `platform-teavm` | Browser platform implementation |
| `teavm-compat` | Java compatibility code for TeaVM |
| `target_teavm_wasm_gc` | Browser client target and web output |
| `target_teavm_wasm_gc_server` | Singleplayer server worker target |
| `target_teavm_wasm_gc_mesh` | Mesh worker target |
| `wasm-toolchain` | Linking and HTML packaging scripts |
| `source` | Extracted inputs and source-generation files |
| `receipt.json` | Source creation details and input hashes |
| `build.gradle.kts`, `settings.gradle.kts` | Gradle build configuration |

Keep backups before editing. Do not delete the receipt if you want to reuse
the project in the patcher.

## Compile Java only

Set `JAVA_HOME` and `PATH` to a Java 25 JDK, then run this from the project folder:

```sh
./gradlew :game:compileJava --console=plain --no-daemon
```

On Windows, use `gradlew.bat :game:compileJava --console=plain --no-daemon`.
This checks the game Java code; it does not produce the browser client.

## Build the normal HTML

Use Standalone HTML in the patcher. It supplies the tool paths, checked inputs
and memory limits. Advanced settings let you embed music in the HTML. Otherwise,
the build writes an optional music resource-pack ZIP beside it; import that ZIP
through Minecraft's Resource Packs menu.

The CLI equivalent is `build-standalone ... --output <this-folder> --reuse-project`
with the required input and output arguments. Run
`java -jar eaglercraft-26.2-java-cli.jar --help` for the argument list.

Reuse does not extract or patch Java again. It keeps existing dependencies and
may add the pinned Brotli decoder if missing. A different decoder is rejected
instead of being overwritten.

## Smaller files, faster packaging and raw web files

The normal standalone HTML uses lossless compression. There is no uncompressed
single-HTML option.

After a successful build, you can repackage its existing web output with faster
compression. From the project folder, with Node on PATH:

```sh
node wasm-toolchain/build-single-html.js --skip-build --fast-package --output fast-client.html
```

This uses faster compression when needed, or reuses valid compressed files.
The HTML may be larger and is still compressed. Use a new output filename.
Do not use `--skip-build` after source edits: it packages the old build.

The same build leaves a multi-file website in
`target_teavm_wasm_gc/build/web`. Its `.wasm` files are the raw WebAssembly output;
`.br` and `.gz` files are compressed copies. For ordinary raw-file hosting,
serve the folder without automatic Brotli/gzip rewrites. EPK asset archives keep
their own format, so this is not a promise that every asset is uncompressed.

To test that folder locally, if Python 3 is installed:

```sh
python3 -m http.server 8000 --directory target_teavm_wasm_gc/build/web
```

Open `http://localhost:8000/`. On Windows, `py -3` can replace `python3`.
Test the title screen, a world, and saving before sharing the build.

## Cloudflare hosting

Cloudflare is a hosting step after compilation, not another Java build mode.
The large HTML and raw Wasm files can exceed Cloudflare static-asset limits.
Do not assume that uploading the standalone HTML to Pages will work.

One option is an R2 bucket served by a Worker. This avoids packing the whole
game into the Worker code. The recipe below is a deployment example, not a
deployment performed by the patcher. Check Cloudflare's current limits and
pricing, and only upload files you are allowed to distribute.

1. Finish a normal HTML build.
2. Create a separate folder for the Worker configuration below.
3. Create an R2 bucket and upload the contents of
   `target_teavm_wasm_gc/build/web` to its root, preserving subfolders.
   Upload the raw `.wasm` files. The example does not use `.br`, `.gz`,
   `.meta` or `.sha256` sidecars.
4. Bind the bucket as `GAME` and deploy the Worker. Use your own project and
   bucket names, never another project's credentials.

Example `wrangler.toml`:

```toml
name = "my-eagler-client"
main = "worker.js"
compatibility_date = "2026-09-29"

[[r2_buckets]]
binding = "GAME"
bucket_name = "my-eagler-client-files"
```

Example `worker.js`:

```js
export default {
  async fetch(request, env) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405 });
    }
    const url = new URL(request.url);
    const key = url.pathname === "/" ? "index.html" : url.pathname.slice(1);
    const object = await env.GAME.get(key);
    if (!object) return new Response("Not found", { status: 404 });
    const types = {
      html: "text/html; charset=utf-8",
      js: "text/javascript; charset=utf-8",
      wasm: "application/wasm",
      json: "application/json",
      png: "image/png",
      ico: "image/x-icon"
    };
    const extension = key.split(".").pop();
    return new Response(request.method === "HEAD" ? null : object.body, {
      headers: {
        "Content-Type": types[extension] || "application/octet-stream",
        "Cache-Control": "no-cache",
        "X-Content-Type-Options": "nosniff"
      }
    });
  }
};
```

Install Wrangler in that separate folder and sign in to your account:

```sh
npm install --save-dev wrangler
npx wrangler login
npx wrangler deploy
```

The last command publishes the Worker. Do not run it until you want the site
online. Test the resulting URL in a fresh browser session. A signed IWA bundle
is a separate output; hosting this website does not install an IWA.
