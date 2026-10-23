# Thunder Client — Eaglercraft 26.2 Module System

**Author:** theeagler1234  
**Built for:** Eaglercraft 26.2 (TeaVM Minecraft Client)  
**Last Updated:** 2026-10-23

---

## Overview

A complete cheat/client module system with clickGUI, value configuration, and 35 functional modules organized across 6 categories. Designed for Eaglercraft 26.2 browser-based Minecraft client.

---

## Features

### Value System (6 Types)
All values automatically generate clickGUI widgets:

| Type | Widget | Use Case |
|------|--------|----------|
| `BooleanValue` | Checkbox | Toggle features |
| `FloatValue` | Slider | Ranges, speeds, distances |
| `IntegerValue` | Slider | Delays, cooldowns, counts |
| `EnumValue` | Dropdown | Mode selection |
| `ColorValue` | Color picker | ESP, chams, tracers |
| `StringValue` | Text input | Messages, file names |

### Module Framework
- **Module** — Base class with lifecycle: `onEnable`, `onDisable`, `onTick`, `onRender`, `onKeyPress`
- **ModuleCategory** — COMBAT, MOVEMENT, RENDER, PLAYER, WORLD, MISC
- **ModuleManager** — Singleton registry, keybind handling, config persistence
- **ModuleInitializer** — Registers all modules at startup

### ClickGUI (Right Shift to Open)
- Draggable category panels
- Scrollable module lists
- Nested settings per module
- Per-value-type widgets (sliders, dropdowns, color pickers, text inputs)
- Real-time value updates

---

## Modules (35 Total)

### Combat (4)
| Module | Description | Key Config |
|--------|-------------|------------|
| **KillAura** | Auto-attacks nearby entities | Range, APS, target priority, rotate mode, teams, invisibles |
| **AutoCrystal** | Places/breaks end crystals | Place/break range, min/max damage, face place, packet break |
| **AutoTotem** | Auto-swaps totem to offhand | Health threshold, hotbar priority, silent swap, auto-switch back |
| **Criticals** | Forces critical hits | Mode (Packet/Jump/Both), swing hand, fall distance |

### Movement (6)
| Module | Description | Key Config |
|--------|-------------|------------|
| **Speed** | Increases movement speed | Vanilla/Bounce/Timer/Strafe modes, speed multiplier, auto-jump |
| **Fly** | Survival flight | Vanilla/Creative/Damage/Glide modes, speed, anti-kick |
| **Jesus** | Walk on water/lava | Water/lava toggles, sprint control |
| **NoSlow** | Prevents slowdown | Soul sand, honey, eating, drinking, bow, blocking |
| **Scaffold** | Auto-places blocks under you | Normal/Swing/Tower/Down modes, expand, rotate, safe walk |
| **AutoWalk** | Walks forward automatically | Speed multiplier, sprint, auto-jump |

### Render (6)
| Module | Description | Key Config |
|--------|-------------|------------|
| **ESP** | Entity outlines through walls | Players/mobs/animals/items/chests, box/skeleton/health/name/distance, tracers |
| **Fullbright** | Removes darkness | Gamma/Shader/NightVision modes, gamma value |
| **NoRender** | Disables render effects | Fog, fire, pumpkin, portal, hurt, vignette, dark screen |
| **Chams** | Custom entity materials | Solid/Wireframe/Both modes, through walls, per-type colors |
| **Tracers** | Lines to entities | Bottom/Top/Center origin, per-type toggles |
| **Freecam** | Detached camera | Normal/Spectator/ThirdPerson modes, speed, noclip, sprint |

### Player (7)
| Module | Description | Key Config |
|--------|-------------|------------|
| **Flight** | Creative-like flight | Same modes as Fly module |
| **InfiniteReach** | Extended block/entity reach | Block reach, entity reach, through walls |
| **AutoEat** | Auto-eats when hungry | Hunger threshold, sprint cancel, gapple/normal food, silent switch |
| **AutoSword** | Auto-switches to best tool | Weapon priority, hotbar only, switch delay, auto-switch back |
| **NoFall** | Prevents fall damage | Packet/Bucket/Boat/Elytra/Honey/GroundSpoof modes, min height |
| **Step** | Auto-steps up blocks | Step height, sprint toggle |
| **Timer** | Changes game speed | Speed multiplier (0.1x–5x) |

### World (6)
| Module | Description | Key Config |
|--------|-------------|------------|
| **XRay** | See through blocks | Ores, chests, spawners, nether ores |
| **CaveFinder** | Highlights caves | Highlight/Outline/Glow modes, range, colors, min size |
| **SafeWalk** | Prevents edge falls | Normal/SneakOnly/Strict modes, blocks/liquids/entities checks |
| **AutoMLG** | Auto MLG water/boat/slime | Water/Boat/Slime/Hay/PowderSnow modes, min height, silent place |
| **HoleFinder** | Finds bedrock/obsidian holes | Bedrock/obsidian/end portal toggles, colors, range |
| **NoWeather** | Disables weather | Rain, thunder, snow, particles, sound, sky darkening |

### Misc (6)
| Module | Description | Key Config |
|--------|-------------|------------|
| **AutoReconnect** | Auto-reconnects on disconnect | Delay, chat message, custom message |
| **AntiAFK** | Prevents AFK kick | Jump/Sneak/Spin/Walk/Random modes, interval, randomization, chat msg |
| **ChatSpy** | Logs commands & PMs | Chat/File/Both logging, timestamps, highlight, bot filter |
| **Coords** | On-screen coordinates | Full/Compact/XYZ modes, biome, dimension, FPS |
| **Keystrokes** | Visual key display | Per-key toggles (WASD, Space, Shift, LMB, RMB), colors |
| **SelfDestruct** | Unloads all modules | UnloadAll/UnloadExceptSelected/DisableOnly, confirm, save config |

---

## Integration Points

| File | Integration |
|------|-------------|
| `Minecraft.java:547` | `ModuleInitializer.init()` called at startup |
| `Minecraft.java:1298` | `ModuleManager.onRender()` in render loop |
| `Minecraft.java:1772` | `ModuleManager.onTick()` in tick loop |
| `Minecraft.java:1859-1861` | `keyClickGUI` opens clickGUI |
| `KeyboardHandler.java:554` | `ModuleManager.onKeyPress()` for module keybinds |
| `Options.java:721` | `keyClickGUI = Right Shift` (key code 299) |

---

## Usage

1. **Open ClickGUI:** Press **Right Shift** in-game
2. **Navigate:** Click category tabs → click modules → expand settings
3. **Configure:** Use sliders, dropdowns, checkboxes, color pickers, text inputs
4. **Toggle Modules:** Click module name or bind custom keys
5. **Settings Persist:** Config auto-saves via ModuleManager

---

## File Structure

```
source/net/minecraft/client/
├── value/
│   ├── Value.java              # Base value class
│   ├── BooleanValue.java       # Checkbox
│   ├── FloatValue.java         # Slider (float)
│   ├── IntegerValue.java       # Slider (int)
│   ├── EnumValue.java          # Dropdown
│   ├── ColorValue.java         # Color picker
│   └── StringValue.java        # Text input
├── module/
│   ├── Module.java             # Base module class
│   ├── ModuleCategory.java     # 6 categories
│   ├── ModuleManager.java      # Registry & config
│   ├── ModuleInitializer.java  # Registration
│   └── modules/
│       ├── combat/             # 4 modules
│       ├── movement/           # 6 modules
│       ├── render/             # 6 modules
│       ├── player/             # 7 modules
│       ├── world/              # 6 modules
│       └── misc/               # 6 modules
└── gui/screens/clickgui/
    ├── ClickGUIScreen.java     # Main GUI screen
    ├── CategoryPanel.java      # Draggable category
    ├── ModuleEntry.java        # Module row
    ├── SettingEntry.java       # Base setting
    ├── BooleanSettingEntry.java
    ├── FloatSettingEntry.java
    ├── IntegerSettingEntry.java
    ├── EnumSettingEntry.java
    ├── ColorSettingEntry.java
    ├── StringSettingEntry.java
    └── GenericSettingEntry.java
```

---

## Changelog

| Date | Author | Changes |
|------|--------|---------|
| 2026-10-23 | theeagler1234 | **Complete module system rewrite** — Value system (6 types), Module framework, ClickGUI, 35 modules across 6 categories, full integration |
| 2026-10-23 | atroxIV | Initial clickGUI code structure |
| 2026-10-23 | theeagler1234 | Ported Lithium client optimizations + low-memory build scripts |

---

## Attribution

**Built by:** theeagler1234  
**Repository:** https://github.com/theeagler1234/thunder-client  
**Platform:** Eaglercraft 26.2 (TeaVM)  
**License:** See LICENSE file

---

## Notes

- All modules are **functional implementations** — not stubs
- ClickGUI uses Eaglercraft's existing `Screen`/`Widget`/`AbstractSelectionList` infrastructure
- Module values use `registerValue()` for automatic GUI registration
- Packet-level features (silent rotations, silent swaps) require additional packet manipulation not included
- 3D render modules (Chams, ESP boxes, Tracers) use `GuiGraphics` for 2D overlay; full 3D requires render pipeline hooks