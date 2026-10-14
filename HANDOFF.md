# Fig Client Codespace Handoff

Read this before continuing client work in a new Codespace.

## Current state

- Repository: `Figz166/Classroom`
- Main working branch for the client: `pages-deploy`
- Latest client commit: `3aa38a6f` (`Redesign Fig Client interface and HUD`)
- The commit is pushed, and GitHub Pages deployment completed successfully:
  <https://figz166.github.io/Classroom/>
- Deployment run: <https://github.com/Figz166/Classroom/actions/runs/38501603608>
- A separate `docs/codespace-handoff` branch contains this handoff document, so the handoff itself does not trigger another Pages build.

## Work completed

The client already had SMP-focused modules and utilities, including waypoint management and death locations, inventory helpers, equipment/armor HUD, X-Ray, and loaded-chunk block search.

The latest redesign batch added or updated:

- Fig Client title branding and a locally created pixel-art waterfall menu background.
- A yaw-responsive compass and floored world coordinates, including negative coordinates.
- Drag-to-position HUD layout for the compass, equipment display, and hotbar.
- Fig Mods screen with horizontal category tabs, live module search, and links to tools.
- Auto Totem with a configurable health trigger (defaults to 4 hearts); it is disabled by default.
- Toggle keybinds for modules, editable in Controls.
- Filled translucent block-search highlights intended to show through cover, rather than edge-only outlines.
- A GitHub Actions cache for relevant Gradle/Wasm build outputs and linker receipts.

Relevant implementation files:

- `game/src/main/java/net/minecraft/client/gui/screens/TitleScreen.java`
- `game/src/main/java/net/minecraft/client/gui/Hud.java`
- `game/src/main/java/net/minecraft/client/gui/screens/options/FigModulesScreen.java`
- `game/src/main/java/net/minecraft/client/gui/screens/options/FigHudLayoutScreen.java`
- `game/src/main/java/net/minecraft/client/gui/screens/options/FigBlockSearch.java`
- `game/src/main/java/net/minecraft/client/Options.java`
- `game/src/main/java/net/minecraft/client/Minecraft.java`
- `game/src/main/java/net/minecraft/client/renderer/LevelRenderer.java`
- `.github/workflows/deploy-game-pages.yml`

## Validation and deployment notes

- `./gradlew :game:compileJava` passed after the changes.
- `en_us.json`, the Pages workflow YAML, the waterfall PNG, and `git diff --check` were validated.
- The Pages workflow succeeded and the live site returned HTTP 200.
- The cache restored successfully, but a Java/game-source change still required the self-contained WasmGC build. That build step took about 43 minutes. The cache is useful, but it did **not** make this source-changing deployment quick.
- No automated browser play-test was completed. Test the UI and gameplay behaviors in the deployed client before treating them as visually/runtime verified.

## How to continue

1. Open the repository in the new Codespace and switch to `pages-deploy` for the current client implementation.
2. Read this document first. Do not reset or overwrite unrelated working-tree changes.
3. Run the focused Java compile after code edits; validate JSON/YAML and whitespace when those files change.
4. Bundle related client edits into one deployment because the Pages Wasm build is lengthy. Tell the user when a deployment starts and report progress if it approaches 30 minutes.
5. Deploy only when requested; verify the Actions run and live Pages URL before saying it is deployed.

## User preferences and project context

- The user is building a Fig-branded Minecraft/Eaglercraft client for SMP use and values practical modules and clear categories, including Waypoints.
- For code requests, implement the requested change rather than only outlining a plan. Keep changes focused and avoid unrelated refactors.
- Keep explanations concise and direct. Be honest about limitations, especially deployment time; do not claim caching skips a Wasm rebuild when game sources change.
- The user wants to test deployed changes. Validate locally first and avoid repeated Pages deployments for small batches.
- Ask one focused question if a behavioral or scope decision materially changes the feature. If the user is unavailable, choose a conservative, clearly explained default.
- Initial publishing instructions asked not to inspect the Eaglercraft file contents; later the user explicitly requested client code changes. Continue to inspect only files needed for the work at hand.
- The user has been switching accounts/Codespaces. This repository is shared through GitHub; Codespaces do not automatically transfer between accounts. Ensure the other account has accepted repository access and check out the desired branch.
