# Fig Client Codespace Handoff

Read this first when continuing in a new Codespace.

## Repository and deployment

- Repository: `Figz166/Classroom`
- Client branch: `pages-deploy`
- Live site: <https://figz166.github.io/Classroom/>
- GitHub Actions deploys on pushes to `main` and `pages-deploy` using `.github/workflows/deploy-game-pages.yml`.
- The workflow builds a production standalone HTML/Wasm client. Do not copy `_site` from a local fast build into a release; the local preview is not the release artifact.
- The full source-changing Wasm link previously took about 43 minutes. CI has a cache, but it can still take a long time. Confirm the run in Actions and check the live site before claiming deployment succeeded.

## Current deployed work

The title screen uses the six-face rotating Otherside panorama from the Modrinth project by Karbin_Drem. Attribution and CC BY-SA 4.0 details are in `THIRD_PARTY_ASSETS.md`. The old static Fig waterfall image was removed. The title menu is a frameless left-side text rail with cyan Fig branding, and the vanilla yellow splash/fact is removed.

The existing Fig Mods/utilities include compass, waypoints/death locations, equipment HUD, X-Ray, block search, auto-totem, and related SMP helpers.

Recent module work in this branch:

- `Food Info`: optional saturation indication and food-item nutrition/saturation preview in tooltips. The user said the first HUD indicator was hard to understand; redesign it to be explicit/readable before treating it as done.
- `Inventory Sort`: a Sort button on inventory/chest/shulker screens, using normal container clicks. It simulates stack merging and queues clicks while waiting for server state updates. It has compiled, but has not been tested in an actual chest against a live server. Test singleplayer and a vanilla multiplayer server; stop safely on rejected/stale updates.
- Experimental Quick Stack, chest search, and chest rules were removed at the user's request. Do not re-add them without asking.
- A preview server was stopped when the user paused. `_site` is only a local fast-analysis menu build and is not a complete gameplay test artifact.

## Explicitly pending

- The user requested a small top-right profile card showing the selected skin and username.
- The user requested an Alt Manager for switching saved local profiles/accounts.
- Change the title footer to say `Rewritten by Notcher2000` and move `o_xer` to the credits screen.
- The user wants module changes first in the next work session; do not start these pending UI changes until they give the next task.
- AppleSkin can only be ported as native Fig code, not loaded as a Fabric/NeoForge JAR. Its source is Unlicense. Sorted is All Rights Reserved with no public source; only an independent implementation based on public behavior is allowed.
- Absolute Order's server-authoritative filters/hopper behavior cannot be enforced by a client-only Fig module on arbitrary vanilla servers/Realms.

## Validation from this work

- `./gradlew :game:compileJava --console=plain --no-daemon` passed after the current source edits.
- The last fast-analysis client link also succeeded before the final pruning/sorter changes; run it again before restarting a local preview.
- No production Pages deployment is verified until the current push's Actions workflow succeeds and the live page is checked.
- Generated build outputs, `.gradle`, `node_modules`, and `_site` are local artifacts; stage only source/assets, `THIRD_PARTY_ASSETS.md`, and this handoff.

## Workflow reminders

- Preserve unrelated working-tree changes; never use destructive git cleanup/reset commands.
- The user prefers that the assistant runs commands and does not ask them to run routine commands.
- They have a limited Copilot credit budget and asked to deploy the current state before continuing larger tasks.
- Keep progress reports honest and brief. A Java compile does not verify browser gameplay or multiplayer inventory behavior.
