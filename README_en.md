# Lorian’s Arch Orbit

> [!NOTE]
> This is a project by AI. If you mind this, please do not use this mod.

<img src="https://cdn.jsdelivr.net/gh/MapManager-group/Lorian-Arch-Orbit@main/docs/image/icon.png" width="400" alt="The color wheel is moving on its own w~">

A building-assistance mod for Creative builders, map makers, and modpack teams. It supports Minecraft 26.2 on Fabric and NeoForge; each major feature can be toggled independently and hot-reloaded.

## Features

`O` opens the configuration screen by default. All keybindings can be changed in Minecraft's Controls settings.  
Except for **interaction reach modification**, all features are client-side.

### Primary / Secondary Item Color Wheels

- Hold `R` by default to open the **primary color wheel**; press `R` twice to open the **secondary color wheel**; use the scroll wheel to select items
- Press `P` by default to open the **visual color wheel editor**, which supports **JSON / share-code import and export**

![](./docs/image/1.png)

![](./docs/image/2.png)

### Smart Middle-Click Pick Block

- A short middle-click keeps the vanilla Pick Block behavior; holding for about 100 ms opens the **candidate wheel** to select nearby blocks
- The candidate wheel provides three modes: **adjacent mode, range mode, and context mode**; context mode is enabled by default

![](./docs/image/3.png)

### Interaction Reach Modification

- Hold `G` by default and use the mouse scroll wheel to dynamically adjust the interaction reach from 5 to 128 blocks
- This feature requires the mod to be installed on both the client and the server, and is authorized by the server configuration
- This feature is disabled by default, because on the Fabric side Axiom's `Infinite Reach` can replace it

![](./docs/image/4.png)

### Connected-Texture Fixes

- Completes connected faces for standard walls, beds, doors, extended pistons, the four narrow Nether Portal edge faces, and the five non-top End Portal faces
- All fixes can be changed and reloaded dynamically in the configuration

![](./docs/image/5.png)

### Invisible-Block Display

- Press `V` by default to toggle barriers and light blocks at the same time; the types can be filtered in the configuration

![](./docs/image/6.png)


## Block gradients

Press `P` to open the editor home, then choose **Gradient**. Add color or block stops, set each segment length (2–128 samples including endpoints), choose RGB or OkLAB, and filter by facing, upstream palette or the current wheel group. Inspect individual face textures before confirming a block. Generate a preview preserving every sample, including repeats. Choose Inventory, Temporary wheel, New group or Replace group, then Apply. Permanent group changes remain an undoable wheel draft until saved.

Both editors share the same shortcut. In creative mode, Inventory fills the hotbar first, then the main inventory (up to 36 results). Restore items rolls back overwritten slots, preserving original counts and components. Triple-tap the wheel key (default `R`), hold the third press, scroll to immediately replace the held block, then release to close the temporary wheel. The temporary wheel resets when the game restarts; inventory backups expire when the player instance changes.

Use the View menu to retain a result for comparison, and the candidate palette menu to include/exclude individual item/texture pairs. Plans & palettes explicitly exports or imports gradient parameters and candidate filters via JSON files or the clipboard. Files live in `config/lorian_arch_orbit/gradient-plans/`; generated results and positional locks are not saved. Face metadata comes from the vanilla 26.2 models and default block states, with legal facing/axis rotations. Resource packs change the displayed sprites, not the HueBlocks matching colors or the extracted model geometry.

Matching HueBlocks data is checked in the background on every launch and cached locally. Use Refresh to retry; valid cached data remains available offline. Only the eight built-in color groups were reordered; their original members, other presets and custom overrides are preserved. See [gradient details](docs/HUEBLOCKS.md).

## Dependencies

### Fabric

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [Architectury API](https://modrinth.com/mod/architectury-api)
- [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)

### NeoForge

- [Architectury API](https://modrinth.com/mod/architectury-api)
- [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)


## Documentation

- [Configuration and troubleshooting](CONFIGURATION.md)
- [Upward migration guide](docs/UPWARD_MIGRATION.md)
- [Downward migration guide](docs/DOWNWARD_MIGRATION.md)
- [Update notes](UPDATE_NOTES.md)


## TODO

1. Chest connected-texture fixes (unresolved: with block updates disabled, removing one double-chest half still requires opening the remaining chest before its exposed joining face updates)
2. Structure-block display
3. Custom tool & command wheel


## Credits

- [LotTweaks](https://github.com/aruma256/LotTweaks): behavioral reference for the color wheel, smart pick, and interaction reach.
- [Visible Barriers](https://github.com/AmyMialeeMods/visiblebarriers): behavioral reference for invisible-block display.
- [HueBlocks](https://github.com/1280px/hueblocks)：Gradient color matching feature reference.
- The lovely Jiu Hu (酒狐): contributed this project's icon.

## License

[MIT License](LICENSE).

Gradient functionality and downloaded block color/palette data are based on [HueBlocks](https://github.com/1280px/hueblocks).
