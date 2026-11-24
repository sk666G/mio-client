# Known limitations

This file separates verified behavior from work that still needs runtime or architectural evidence.

## Runtime and multiplayer

- No live Minecraft client session was available during this implementation pass. ClickGUI, HUD editing, palette navigation, theme switching, startup/shutdown, resource reload and single-player lifecycle need manual runtime testing.
- No controlled Minecraft server was available. No claims are made about compatibility of remaining movement, combat, interaction or packet modules with vanilla servers or specific plugins.
- The source-level audit removed `GrimDisablerModule` and `RaytraceBypassModule`, including registration and rebuild-script reinjection. The remaining module set has not yet been reduced to a complete per-module protocol compatibility matrix.
- The runtime lifecycle of Context Profiles needs tests with initialized client services for connect, disconnect, server switch, dimension/game-mode switch, failed category restoration and AutoPreset ordering.

## Performance

- Discord IPC now has a 12-second module-side connection deadline, a cancellation state, tracked in-flight pipes and immediate raw transport abort for Unix and Windows after a pipe object exists. A blocking Unix handshake-read regression test verifies that cancellation unblocks the read. Windows `RandomAccessFile` endpoint construction happens before a pipe object can be tracked; if that native open itself blocks, the module can time out and turn off but cannot forcibly stop that native open. Windows runtime cancellation has not been tested.

- Event profiling is disabled by default. Callback timings can include nested event dispatch; sums are inclusive and are not total frame time.
- Frame timing and profiler data require in-game capture to diagnose actual performance. No benchmark or module hot spot is claimed from static inspection.
- GC pressure, allocation rates, render cost and entity-loop costs have not been benchmarked in a live client.
- The frame graph uses sample order horizontally and has no elapsed-seconds axis; pause and focus-loss behavior still needs runtime validation.
- The Gradle build reports deprecated Gradle features that will not be compatible with Gradle 10 without cleanup. The Schizo-generated `schizo$bind…` remapping warnings are resolved; other warnings remain for Minecraft synthetic lambda/method names and a Lithium hook, and have not been suppressed.

## Persistence and rebuild

- Atomic configuration writes, safe preset naming, directory migration and rebuild staging have unit coverage, but power-loss behavior and cross-filesystem edge cases need platform testing.
- The rebuild script now refuses a dirty or non-root destination checkout and requires `--replace-existing`. The dedicated rebuild workflow tests the script before import. The workflow has not been run against a fresh upstream checkout as part of this pass.
- Context profile restoration failures are retained and retried, but the retry path lacks an isolated regression test because category restoration depends on client configuration state.

## Release

- A GitHub Actions build is the available build verification; local runtime verification and artifact launch verification remain outstanding.
- The vendored DiscordIPC source carries Apache-2.0 attribution and the full license in `LICENSES/DiscordIPC-APACHE-2.0.txt`. Unix framing uses the original Schizo MIT codec; two helpers with unverified provenance were removed.
- No public release was created. The branch is not merged.
