# Ghost Blueprints

Build from a schematic on a **vanilla client**. Ghost Blueprints shows the blocks you still have to place as
small ghost blocks that only you can see, marks wrong blocks in red, and tells you which materials
you are missing. Think Litematica, but server-side: no client mod required.

> Status: early MVP (0.1.0). See [Roadmap](#roadmap).

## Features

- Loads Sponge `.schem` files (v1-v3) written by WorldEdit and FastAsyncWorldEdit.
- Ghosts are per-player: spawned invisible by default and revealed to the owner only.
- Missing blocks are shown as small ghost blocks; **wrong blocks** are outlined in red.
- Move, rotate (90 degree steps) and show a single layer at a time.
- Progress and material list with a comparison to your inventory.
- Only renders what is near you and visible; hidden interior blocks are skipped.
- Reads schematics from `plugins/WorldEdit/schematics` and `plugins/FastAsyncWorldEdit/schematics` too.

## Requirements

- Paper (or a fork) 1.21.4 or newer, Java 21

## Installation

1. Download the jar from the [releases](../../releases) or build it yourself (see below).
2. Put it into `plugins/` and restart the server.
3. Copy `.schem` files into `plugins/GhostBlueprints/blueprints/`.

## Usage

| Command | Description |
| --- | --- |
| `/bp list` | List available schematics |
| `/bp load <name>` | Load a schematic |
| `/bp place` | Anchor it on the block you look at (or at your feet) |
| `/bp move <dx> <dy> <dz>` | Shift the blueprint |
| `/bp rotate [90\|180\|270]` | Rotate clockwise |
| `/bp layer <n\|all\|up\|down>` | Show only one layer |
| `/bp status` | Progress in percent, missing and wrong blocks |
| `/bp materials` | Remaining materials vs. your inventory |
| `/bp hide` / `/bp show` | Toggle the ghosts |
| `/bp clear` | Unload the blueprint |
| `/bp reload` | Reload `config.yml` (admin) |

`/bp` is an alias of `/blueprint` (also `/ghost`).

### Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `ghostblueprints.use` | everyone | Use the commands |
| `ghostblueprints.admin` | op | Reload the config |

## Configuration

See [`config.yml`](src/main/resources/config.yml). The most relevant options:

- `render.radius` / `render.max-ghosts`: trade view distance against entity count.
- `match-mode`: `MATERIAL` (default) or `STATE` for strict block state comparison.
- `limits.max-volume`: largest schematic that can be loaded.

## How it works

Ghosts are `BlockDisplay` entities. They are spawned with `visibleByDefault = false` and shown to a single player via
`Player#showEntity`, so other players never receive them. They are non-persistent and removed on quit, clear and
shutdown. Every few ticks, the blocks around the player are compared with the blueprint and the entity set is
updated by diff.

## Build

```bash
./gradlew build        # jar in build/libs, runs tests
./gradlew runServer    # local Paper test server (accepts the Minecraft EULA only if you set it up)
```

## Project layout

```
src/main/java/dev/ghostblueprints/
  GhostBlueprintsPlugin.java   entry point, scheduler
  Settings.java                config snapshot
  BlueprintLibrary.java        finds .schem files
  schematic/                   Bukkit-free NBT and Sponge schematic parsing
  session/                     per-player state and rotation math
  render/                      ghost entities, block matching, progress scan
  command/                     /blueprint
  listener/                    quit and build events
```

## Roadmap

- [ ] `.litematic` import
- [ ] Mirror and fine-grained rotation
- [ ] Persist sessions across restarts
- [ ] Auto-pull materials from nearby chests
- [ ] Translations
- [ ] Share blueprints between players

## License

[MIT](LICENSE)
