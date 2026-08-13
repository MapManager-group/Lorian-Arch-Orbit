# Lorian’s Arch Orbit

A building-assistance mod for Creative builders, map makers, and modpack teams. It supports Minecraft 26.2 on Fabric and NeoForge, and each major feature can be disabled independently.

## Features

- Primary and secondary item palette wheels: hold `R`, then scroll to select. Press `P` for the visual editor and JSON/share-code import or export.
- Smart pick: tap middle mouse for vanilla Pick Block, or hold it for about 100 ms to open the candidate wheel.
- Block reach: hold `G` and scroll to select 5–128 blocks. The mod must be installed and authorized on both client and server.
- Connected-texture fixes: restores missing joining faces on standard wall, bed, and door models. Chests are not supported yet.
- Invisible-block display: press `V` to toggle barriers and light blocks; either type can be filtered in the configuration.

Press `O` to open the configuration screen. Every binding can be changed in Minecraft Controls.

## Installation

Install the JAR for your loader and Architectury API. Fabric also requires Fabric API. YACL is recommended on clients; Mod Menu is optional on Fabric. NeoForge exposes the configuration from its mod list.

Only block reach needs the server component. A dedicated server needs Architectury API and this mod, but not YACL, Mod Menu, or other client-only dependencies.

## Documentation

The project documentation is maintained in Chinese:

- [Configuration and troubleshooting](CONFIGURATION.md)
- [Forward migration guide](docs/UPWARD_MIGRATION.md)
- [Backport guide](docs/DOWNWARD_MIGRATION.md)
- [Update notes](UPDATE_NOTES.md)

## Modpack integration

A [Preliminary Art Form 26.2 preset](integration/preliminary-art-form-26.2/README.md) is included. Remove LotTweaks and Visual Barriers because their features overlap. The default `V` binding also conflicts with WorldEdit CUI, so one binding must be changed in the pack's `options.txt`.

## Credits

- [LotTweaks](https://github.com/aruma256/LotTweaks) for behavioral references around palettes, smart pick, and reach.
- [Visible Barriers](https://github.com/AmyMialeeMods/visiblebarriers) for invisible-block visibility behavior.
- [Preliminary Art Form](https://github.com/MapManager-group/Preliminary-Art-Form-Modpack) as the original target modpack and compatibility environment.
- Block-group and connection-face reproduction material in the development workspace was used only for behavioral analysis. This project's code, model definitions, and atlas configuration are independently implemented; Minecraft supplies the runtime block icons and base textures.

## License

Copyright © 2026 DavidBlackCN. Released under the [MIT License](LICENSE). Minecraft names, icons, and game assets belong to their respective rights holders.
