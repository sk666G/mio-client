# Schizo Client

Schizo is a clean-room Fabric client project for Minecraft 1.21.11. It provides a small, modular foundation for legitimate client-side features.

## Included foundation

- Client-only Fabric entrypoint
- Central module registry and module categories
- Feature enable/disable lifecycle hooks
- Validated boolean, bounded-number, and mode settings
- Duplicate ID checks for modules and per-module settings

Modules should be opt-in, have clear descriptions, and avoid unauthorized interaction with servers or other clients.

## Build

- JDK 21
- Gradle 8.14+

```bash
gradle build
gradle runClient
```

The remapped mod JAR is produced under `build/libs/`.

## Project status

This is an early foundation, not a finished client. A ClickGUI, persistent configuration, keybind integration, HUD, and concrete gameplay modules have not yet been implemented. Network-protocol manipulation, stealth injection, and anti-cheat evasion are out of scope.

## License

Add the license appropriate to the code you contribute. Third-party code and assets must retain their original licenses and notices.
