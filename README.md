# Schizo Client

Schizo is a client-side Fabric mod for Minecraft 1.21.11. It provides a modular foundation for client-side gameplay utilities and configurable overlays.

## Reference baseline

Schizo targets Minecraft 1.21.11 and follows the JDK 25 runtime / Java 21 toolchain setup documented by Open Zio. It is an independent implementation: Zio is used as a compatibility and feature-direction reference, not as copied source code.

## Included

- Client-only Fabric entrypoint
- Right Shift ClickGUI keybind (editable in Options > Controls)
- Category filters and module enable/disable buttons
- Dedicated, unbound-by-default keybinds for every module under Options > Controls > Schizo Modules
- Boolean, bounded-number, and mode setting controls
- Feature lifecycle hooks and central registry
- Toggle Sprint module that uses normal Minecraft sprint input
- Auto Tool module that chooses the strongest existing hotbar tool for the targeted block while mining
- Information HUD for coordinates, facing direction, FPS, and enabled modules
- Target HUD for the living entity under the crosshair, with health bar and optional distance
- Armor HUD with equipment icons, durability percentages, optional empty slots, and left/right placement
- Effects HUD with active status effects, amplifier levels, durations, and configurable row count
- Keystrokes HUD showing live WASD and jump input states, with left/right placement
- Totem Counter HUD showing the number of totems in the inventory and offhand
- JSON configuration saved in the Minecraft config directory
- Module enabled states and supported setting values restored on startup
- GitHub Actions build workflow that compiles the mod and uploads the JAR artifact

## Build

- JDK 25 runtime with Java 21 toolchain
- Gradle 9.7.1

```bash
gradle build
gradle runClient
```

The remapped mod JAR is produced under `build/libs/`.

## Configuration

Schizo writes `config/schizo.json` in the active Minecraft game directory. Module toggles and Boolean, Number, and Mode settings are saved when changed and again during client shutdown. Module keybinds are managed by Minecraft's Controls screen and saved with the normal game options. Unknown module or setting IDs are ignored so configuration files can survive module updates.

## Project status

The ClickGUI provides module and setting controls with a dark panel theme. The Information HUD can show block coordinates, facing direction, FPS, and enabled modules. The Target HUD displays health and optional distance for the living entity under the crosshair. The Armor HUD shows equipped armor and remaining durability. The Effects HUD lists active status effects and durations. Keystrokes shows WASD and jump input. The Totem Counter displays carried totems. HUD settings are configurable in the GUI. Toggle Sprint uses normal Minecraft sprint input and leaves sprint eligibility to Minecraft. The project does not include stealth injection, anti-cheat circumvention, or detection-evasion mechanisms.

## License

Add the license appropriate to the code you contribute. Third-party code and assets must retain their original licenses and notices.
