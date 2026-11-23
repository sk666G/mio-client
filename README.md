# Schizo Client

Schizo is a client-only Fabric client for Minecraft 1.21.11, rebuilt on the Open Zio source architecture. This replaces the former minimal Schizo scaffold with the upstream module registry, event bus, managers, configuration and preset systems, ClickGUI/HUD editor, rendering pipeline, mixins, access widener, integrations and module implementations.

## Upstream and attribution

This is an independent rebrand and derivative of Open Zio. See UPSTREAM.md for the pinned snapshot. Original upstream source headers are retained. Third-party notices and known license gaps are recorded in THIRD_PARTY_NOTICES.md.

Schizo is not affiliated with or endorsed by Mio.

## Build

Use JDK 25 to run Gradle and have JDK 21 installed for the Java toolchain.

    ./gradlew --no-daemon clean build
    ./gradlew runClient

The remapped client JAR is written to build/libs/.

## Performance HUD

See [Performance HUD documentation](docs/PERFORMANCE_HUD.md) for frame-time metrics, 1% lows, hitch counting and the frame-time graph.

## Automatic Context Profiles

See [Context Profiles documentation](docs/CONTEXT_PROFILES.md) for server, dimension and game-mode preset switching.

## Command Palette

See [Command Palette documentation](docs/COMMAND_PALETTE.md) for global search and actions using Right Shift + K.

## Visual identity

See [Schizo visual identity](docs/VISUAL_IDENTITY.md) for the default palette and customization behavior.

## HUD Studio

See [HUD Editor documentation](docs/HUD_EDITOR.md) for layout editing, undo/redo shortcuts and named layout profiles.

## Original modules

The original upstream module sources are included in the active Schizo source tree. Their implementations and original source notices are retained during the rebrand.
