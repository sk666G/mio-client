# Schizo Client

Schizo is a clean-room Fabric client project for Minecraft 1.21.11. This repository starts with a modular client foundation: lifecycle/bootstrap, feature registration, configuration, and rendering hooks.

## Build

- JDK 21
- Gradle wrapper (or Gradle 8.14+)

```bash
./gradlew build
./gradlew runClient
```

The remapped mod JAR is produced under `build/libs/`.

## Project status

This is the initial scaffold. Features are intentionally added as isolated modules with explicit settings and safe defaults. Network-protocol manipulation, stealth injection, and anti-cheat evasion are not part of the project.

## License

Add the license appropriate to the code you contribute. Third-party code and assets must retain their original licenses and notices.
