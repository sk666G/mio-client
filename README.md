# Schizo Client

Schizo is a client-only Fabric client for Minecraft 1.21.11, rebuilt on the Open Zio source architecture. This replaces the former minimal Schizo scaffold with the upstream module registry, event bus, managers, configuration and preset systems, ClickGUI/HUD editor, rendering pipeline, mixins, access widener, integrations and module implementations.

## Upstream and attribution

This is an independent rebrand and derivative of Open Zio. See UPSTREAM.md for the pinned snapshot. Original upstream source headers are retained. Third-party notices and verified vendored-license information are recorded in THIRD_PARTY_NOTICES.md and LICENSING.md.

Schizo is not affiliated with or endorsed by Mio.

## Build

Use JDK 25 to run Gradle and have JDK 21 installed for the Java toolchain.

    ./gradlew --no-daemon clean build
    ./gradlew runClient

The remapped client JAR is written to build/libs/.

## Original modules

The original upstream module sources are included in the active Schizo source tree.
