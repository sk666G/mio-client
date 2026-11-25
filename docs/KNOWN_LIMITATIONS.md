# Known limitations

This file separates verified behavior from work that still needs runtime or architectural evidence.

## Runtime and multiplayer

- No live Minecraft client session was available during this implementation pass. ClickGUI, HUD editing, palette navigation, theme switching, startup/shutdown, resource reload and single-player lifecycle need manual runtime testing.
- No controlled Minecraft server was available. No claims are made about compatibility of remaining movement, combat, interaction or packet modules with vanilla servers or specific plugins.
- The source-level audit removed `GrimDisablerModule` and `RaytraceBypassModule`, including registration and rebuild-script reinjection. The remaining module set has not yet been reduced to a complete per-module protocol compatibility matrix.
- The runtime lifecycle of Context Profiles still needs tests with initialized client services for connect, disconnect, server switch, dimension/game-mode switch and AutoPreset ordering. The pure baseline capture/partial-restore retry bookkeeping now has isolated regression coverage; the remaining gap is registry/event orchestration against initialized client services.
- **Combat placement worker thread-safety is resolved in source.** AnchorAura and AutoCrystal no longer run placement scans on background executors: both advance deterministic bounded client-thread scan cursors while keeping live world/entity/visibility/damage reads on the Minecraft client thread and revalidating results before publication. AnchorAura passed Build #699 and AutoCrystal passed Build #707. Live timing, responsiveness and placement behavior still require in-game validation. [Combat Scan Snapshot Design](COMBAT_SCAN_SNAPSHOT_DESIGN.md) remains the required contract if background combat calculation is ever reintroduced.
- Discord IPC now has a 12-second module-side connection deadline, a cancellation state, tracked in-flight pipes and immediate raw transport abort for Unix and Windows after a pipe object exists. Windows frame I/O uses `FileChannel`, so closing the tracked channel can interrupt a blocked channel read. Regression tests cover a blocked Unix handshake read and the Windows little-endian frame decoder using a temporary file; neither substitutes for a real Windows named-pipe runtime test. Windows `RandomAccessFile` endpoint construction happens before a pipe object can be tracked; if that native open itself blocks, the module can time out and turn off but cannot forcibly stop that native open.

## Performance

- Event profiling is disabled by default. Callback timings can include nested event dispatch; sums are inclusive and are not total frame time.
- Frame timing and profiler data require in-game capture to diagnose actual performance. No benchmark or module hot spot is claimed from static inspection.
- GC pressure, allocation rates, render cost and entity-loop costs have not been benchmarked in a live client.
- The frame graph uses sample order horizontally and has no elapsed-seconds axis; pause and focus-loss behavior still needs runtime validation.
- Diagnostic Build #709 traced the Gradle-10 `Task.project` deprecation to Schizo's `processResources` closure. The v7 candidate captures the mod version during configuration instead of reading `project` during task execution; the final v7 build must confirm that warning is gone. Remapping warnings for Minecraft synthetic lambda/method names and a Lithium hook remain and have not been suppressed.

## Persistence and rebuild

- Atomic configuration writes, safe preset naming, directory migration and rebuild staging have unit coverage. Preset application and each startup JSON configurable now snapshot and roll back on uncaught application failure, while tolerated per-setting/module issues retain their existing report-and-continue semantics. Power-loss behavior and cross-filesystem edge cases still need platform testing.
- The rebuild script now refuses a dirty or non-root destination checkout and requires `--replace-existing`. Dedicated fresh-upstream rebuild run #6 successfully fetched the pinned Open Zio commit, ran security tests, rebuilt the staging tree, built the remapped client and uploaded its JAR against the v5 precursor. The final v6 lineage still needs the same staging run repeated after candidate integration settles.
- Context profile restoration failures are retained and retried. `ContextProfileRestoreStateTest` now covers all-or-nothing capture, partial restore, retained failed categories, retry success and missing-baseline behavior. Full registry/event ordering still needs initialized-client runtime coverage.

## Release

- GitHub Actions is the available build verification; local runtime verification and artifact launch verification remain outstanding. The v7 workflow verifies exactly one runnable remapped JAR, records `COMMIT_SHA.txt` and `SHA256SUMS.txt`, and verifies required license/notice files inside both runtime and source JARs.
- The vendored DiscordIPC source carries Apache-2.0 attribution and the full license in `LICENSES/DiscordIPC-APACHE-2.0.txt`; `THIRD_PARTY_NOTICES.md` and that license are packaged with runtime/source artifacts. Unix framing uses the original Schizo MIT codec; two helpers with unverified provenance were removed.
- No public release was created. The branch is not merged.
