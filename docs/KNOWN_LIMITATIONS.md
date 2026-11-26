# Known limitations

This file separates source-verified behavior from work that still needs live Minecraft, platform or controlled-server evidence.

## Runtime and multiplayer

- The v8/full-client static passes were followed by headless Fabric launch and local-world GUI tests on the runtime-validation line and PR #167. Automated screenshots cover ClickGUI, palette, Performance Center, HUD editor, Expedition and layout preview; they do not replace manual input/visual QA, packaged-JAR launch, resource reload, reconnect/respawn or long-session testing. See [Expedition workflow evidence and remaining gates](ANARCHY_WORKFLOWS.md).
- No controlled Minecraft server was available. No claim is made that every movement, combat, interaction or packet-facing module is accepted by vanilla servers or specific plugins. The source audit removed `GrimDisablerModule` and `RaytraceBypassModule`, including registration and rebuild-script reinjection; no new anti-cheat evasion mode was added by the v8 quality work.
- Context Profile baseline capture/partial-restore retry bookkeeping has isolated regression coverage, but connect/disconnect, server switch, dimension/game-mode changes and AutoPreset ordering still need initialized-client runtime validation.
- AnchorAura and AutoCrystal placement scans no longer read mutable live-world state from worker executors. Both use bounded deterministic client-thread cursors and revalidate results before publication. Live placement behavior and frame-time cost still require in-game profiling.
- Hole, Search and Tunnels scanning now keep live block/world reads on the Minecraft client thread in bounded slices. Search/Tunnels queue and deduplicate chunk work, discard stale/unloaded work and use runtime-generation ownership; Hole scanning no longer uses the fixed-rate background sweep. Scan freshness and frame-time behavior still need live validation.
- Deferred work in UI, AutoArmor, AntiAFK, Fireworks, AutoCraft, ChestStealer, SpeedMine and related lifecycle paths now carries module/session/player/world ownership checks before acting. Those guards are source-verified, but reconnect, disable-mid-action and container-transition scenarios still need live smoke testing.
- PhaseESP clears retained animation state across level replacement/disable and uses monotonic animation timing. Hitmarker uses monotonic expiry/cooldown timing, while Skeleton/Crosshair render-state restoration is guarded against draw failures. Visual parity still needs live rendering checks.
- Discord IPC has bounded module-side connection timing, tracked transport cleanup and frame validation. Its Windows named-pipe endpoint construction still cannot be forcibly interrupted before a pipe object exists; a real Windows Discord runtime test remains required.

## Performance

- Event profiling is disabled by default. Callback timings can include nested event dispatch; sums are inclusive and must not be treated as total frame time.
- No live benchmark has been run for allocation rate, GC pressure, entity-loop cost, render cost or scan latency. Static hot-path cleanups are not FPS claims.
- AutoCrystal, AnchorAura, Hole, Search and Tunnels use bounded client-thread scan work. The full-client hardening line scales AutoCrystal/Search/Tunnels scan throughput against measured frame pressure and converts VoidESP floor work into an incremental frame-aware scan cycle. These source changes still need profiler comparison in representative worlds.
- Non-fading Trails history is bounded and batch-pruned instead of growing for the entire session. Long-session retention limits still need runtime confirmation.
- The command palette now normalizes its query once per refresh, snapshots quick-access ranks once per refresh and fits its result window to short/narrow viewports. Extreme GUI scales and window sizes still need visual/input QA.
- The frame graph uses sample order horizontally and has no elapsed-seconds axis; pause/focus-loss behavior still needs runtime validation.
- Build #722 confirmed removal of Schizo's targeted Gradle-10 execution-time `Task.project` warning. Remapping warnings for Minecraft synthetic names and a Lithium hook remain external/mapper-facing warnings and have not been hidden.

## Persistence and data

- Atomic configuration writes, safe preset naming, directory migration, rebuild staging and transactional live-state rollback have unit coverage. Power-loss and cross-filesystem edge cases remain platform-test work.
- ClickGUI close captures configurable JSON on the owning client thread, deep-copies it and serializes asynchronous save snapshots in order so an older queued save cannot overwrite a newer capture. GUI close latency and shutdown ordering still need live testing.
- Preset refresh builds a complete temporary registry before publication, preserves the last complete in-memory set when directory iteration fails, rolls replacement failures back with the exact original bytes and removes replaced entries consistently from memory/disk.
- Map-cache decoding requires a regular no-follow file, bounds the map count, requires exact record payloads and preserves unreadable files instead of overwriting them. Map snapshots are bounded, session-isolated and written atomically. Real existing cache files and reconnect cycles still need runtime/platform validation.
- Context Profile restore failures remain pending for retry rather than discarding recovery state. Full event/registry ordering still needs initialized-client validation.
- The fresh-upstream rebuild workflow has security/staging coverage from earlier candidate runs, but it must be repeated from a clean dedicated staging checkout after the final v8 source head is settled.

## Release and artifact boundary

- GitHub Actions verifies clean Gradle tests/builds, access widener/remap tasks, exactly one runnable remapped JAR, artifact provenance and required third-party notices. The v8 integration lineage has green component and combined builds recorded on Draft PR #110.
- Build #845's uploaded artifact was inspected directly: its SHA-256 matched `SHA256SUMS.txt`, its `COMMIT_SHA.txt` pointed to GitHub's synthetic PR merge whose Git tree exactly matched the tested v8 branch tree, `NESTED_JARS.txt` was empty, and the runtime JAR contained `fabric.mod.json`, `THIRD_PARTY_NOTICES.md` and `LICENSES/DiscordIPC-APACHE-2.0.txt`.
- A successful CI build does not prove that the remapped JAR launches in the target Minecraft/Fabric environment. Final launch, visual/input QA, controlled-server scenarios and the fresh-upstream rebuild remain release gates.
- No public release has been created and no v8 integration branch has been merged into `main`.

- Reduced Motion now snaps the shared timed/interpolated GUI animation primitives, removes screen transition delay and suppresses seasonal screen motion. Individual render-module animations are not globally classified as accessibility UI motion and still require live review.
- Event listener failures are counted and attributed in bounded local diagnostics, but packet dispatch remains synchronous on the posting thread; the project still does not claim a universal client-thread event contract.
