# Schizo Client

Schizo is a client-side Fabric client for Minecraft Java Edition 1.21.11.

## Requirements

- Minecraft Java Edition 1.21.11.
- Fabric Loader 0.19.5 or newer.
- Fabric API for Minecraft 1.21.11.
- Java 21 or newer to run the client.
- JDK 25 to run the Gradle build; Gradle compiles with the configured Java 21 toolchain.

## Build

From the repository root:

```sh
./gradlew --no-daemon clean build
```

To launch a development client:

```sh
./gradlew runClient
```

The remapped client JAR is written to `build/libs/`. CI uploads a `schizo-client` review artifact containing the runnable remapped JAR, `COMMIT_SHA.txt`, `SHA256SUMS.txt`, and `NESTED_JARS.txt`; source/dev JARs are excluded. This is not a published release.

## Install

1. Create a Fabric installation for Minecraft 1.21.11 with Fabric Loader 0.19.5 or newer.
2. Install Fabric API for Minecraft 1.21.11 in that profile.
3. Place the remapped Schizo JAR in that profile's `mods` directory.
4. Start the Fabric profile.

Other mods listed under `suggests` in `fabric.mod.json` are optional.

## Guides

- [HUD Studio](docs/HUD_EDITOR.md)
- [Performance HUD](docs/PERFORMANCE_HUD.md)
- [Module and event profiler](docs/module-profiler.md)
- [Command Palette](docs/COMMAND_PALETTE.md)
- [Context Profiles](docs/CONTEXT_PROFILES.md)
- [Integrations](docs/INTEGRATIONS.md)
- [Visual identity and palettes](docs/VISUAL_IDENTITY.md)
- [Multiplayer compatibility review](docs/MULTIPLAYER_COMPATIBILITY.md)
- [Architecture review](docs/MASTER_ARCHITECTURE_REVIEW.md)
- [Startup Safe Mode](docs/SAFE_MODE.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Dependency and distribution review](docs/DEPENDENCY_REVIEW.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)

## Upstream and attribution

Schizo is an independent rebrand and derivative of Open Zio. See [UPSTREAM.md](UPSTREAM.md) for the pinned source snapshot. Upstream source headers are retained where the source remains in the project. [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) records vendor attribution and unresolved license questions.

Schizo is not affiliated with or endorsed by Mio. The two server-validation circumvention modules `GrimDisabler` and `RaytraceBypass` are excluded from the active source tree and rebuild import.
