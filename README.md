# Schizo Client

Schizo is a Fabric client project for Minecraft 1.21.11. It provides a modular foundation for client-side gameplay modifications and utilities.

## Included

- Client-only Fabric entrypoint
- Right Shift ClickGUI keybind (editable in Options > Controls)
- Category filters and module enable/disable buttons
- Boolean, bounded-number, and mode setting controls
- Feature lifecycle hooks and central registry
- Configurable information HUD with coordinates and facing direction
- JSON configuration saved in the Minecraft config directory
- Module enabled states and supported setting values restored on startup

## Build

- JDK 21
- Gradle 8.14+

```bash
gradle build
gradle runClient
```

The remapped mod JAR is produced under `build/libs/`.

## Configuration

Schizo writes `config/schizo.json` in the active Minecraft game directory. Module toggles and Boolean, Number, and Mode settings are saved when changed and again during client shutdown. Unknown module or setting IDs are ignored so configuration files can survive module updates.

## Project status

The ClickGUI provides basic module and setting controls. The Information HUD can show block coordinates and facing direction, with each display option configurable in the GUI. Further work includes polished layouts, HUD customization, and additional gameplay modules. This project does not include stealth injection, anti-cheat circumvention, or detection-evasion mechanisms.

## License

Add the license appropriate to the code you contribute. Third-party code and assets must retain their original licenses and notices.
