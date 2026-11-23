# Schizo Client

Schizo is a clean-room Fabric client project for Minecraft 1.21.11. It provides a modular foundation for legitimate client-side features.

## Included

- Client-only Fabric entrypoint
- Right Shift ClickGUI keybind (editable in Options > Controls)
- Category filters and module enable/disable buttons
- Boolean, bounded-number, and mode setting controls
- Feature lifecycle hooks and central registry
- Duplicate ID checks for modules and settings

## Build

- JDK 21
- Gradle 8.14+

```bash
gradle build
gradle runClient
```

The remapped mod JAR is produced under `build/libs/`.

## Project status

This is an early client foundation. The ClickGUI currently provides basic module and setting controls; settings are in-memory only. Persistent configuration, polished layouts, HUD rendering, and concrete gameplay modules remain future work. Network-protocol manipulation, stealth injection, and anti-cheat evasion are out of scope.

## License

Add the license appropriate to the code you contribute. Third-party code and assets must retain their original licenses and notices.
