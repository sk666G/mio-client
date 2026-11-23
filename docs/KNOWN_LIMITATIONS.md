# Known limitations

This file separates verified behavior from work that still needs runtime or architectural evidence.

## Runtime and multiplayer

- No live Minecraft client session was available during this implementation pass. ClickGUI, HUD editing, palette navigation, theme switching, startup/shutdown, resource reload and single-player lifecycle need manual runtime testing.
- No controlled Minecraft server was available. No claims are made about compatibility of remaining movement, combat, interaction or packet modules with vanilla servers or specific plugins.
- The source-level audit removed `GrimDisablerModule` and `RaytraceBypassModule`, including registration and rebuild-script reinjection. The remaining module set has not yet been reduced to a complete per-module protocol compatibility matrix.
- The runtime lifecycle of Context Profiles needs tests with initialized client services for connect, disconnect, server switch, dimension/game-mode switch, failed category restoration and AutoPreset ordering.

## Performance

- Event profiling is disabled by default. Callback timings can include nested event dispatch; sums are inclusive and are not total frame time.
- Frame timing and profiler data require in-game capture to diagnose actual performance. No benchmark or module hot spot is claimed from static inspection.
- `TaskScheduler` now has a shared 512-task capacity and a 64-item per-tick budget. Overflow rejects new submissions; old void scheduling calls log the rejection, and new `trySchedule…` calls return a cancellable handle. The Minecraft executor API exposes no acceptance return value, so executor rejection is detected through thrown failures and the client shutdown hooks close the queue before teardown. Runtime shutdown races still need in-game validation.
- GC pressure, allocation rates, render cost and entity-loop costs have not been benchmarked in a live client.

## Persistence and rebuild

- Atomic configuration writes, safe preset naming, directory migration and rebuild staging have unit coverage, but power-loss behavior and cross-filesystem edge cases need platform testing.
- The rebuild script now refuses a dirty or non-root destination checkout and requires `--replace-existing`. The dedicated rebuild workflow tests the script before import. The workflow has not been run against a fresh upstream checkout as part of this pass.
- Context profile restoration failures are retained and retried, but the retry path lacks an isolated regression test because category restoration depends on client configuration state.

## Release

- A GitHub Actions build is the available build verification; local runtime verification and artifact launch verification remain outstanding.
- Third-party license terms for the vendored DiscordIPC source are still not established by repository evidence. `THIRD_PARTY_NOTICES.md` records that gap rather than inventing terms.
- No public release was created. The branch is not merged.
