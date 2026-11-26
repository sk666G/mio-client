# Release checklist

Status is intentionally conservative. A checked item requires recorded evidence; live-only gates remain unchecked even when source/CI work is green.

The post-runtime-validation workflow pass records its commands, ownership ledger,
headless client evidence and targeted remaining scenarios in
[ANARCHY_WORKFLOWS.md](ANARCHY_WORKFLOWS.md). Automated local-world GUI coverage is
distinct from manual QA and launching the remapped distribution artifact.

## Source and integration

- [x] Consolidate the reviewed GUI/HUD, profiler, module/reliability and runtime ownership stacks through verified candidate v7.
- [x] Remove `GrimDisablerModule` and `RaytraceBypassModule`, their registrations and rebuild-script reinjection.
- [x] Integrate the frozen green v8 runtime-ownership baseline from PR #103 rather than following its moving tip.
- [x] Integrate bounded Hole/Search/Tunnels work, ordered ClickGUI save snapshots, bounded Trails retention, preset/map-cache hardening, command-palette/quick-access hardening, registry truthfulness, module hot-path cleanup and render-state guards.
- [x] Integrate the later green lifecycle delta at PR #103 SHA `91761e3d9dcb2d3080f4f3efc213db4fa7c21e6f` (Build #853), PhaseESP lifecycle hardening (Build #850) and render guards (Build #855).
- [x] Add the first full-client hardening layer: transactional module lifecycle rollback, owner-scoped scheduled work, event-failure diagnostics, schema validation, reduced-motion UI policy, render-state guards and post-registry mixin bindings.
- [x] Add frame-pressure-aware incremental scan throughput for AutoCrystal/Search/Tunnels and incremental VoidESP floor scanning.
- [x] Add repository mixin/branding hygiene tests, active development-branch CI coverage and transient dependency-resolution retries.
- [ ] Complete the per-module multiplayer compatibility matrix and authorized controlled-server scenarios.
- [ ] Complete visual/input QA across ClickGUI, HUD Studio, command palette, integration/profile screens, Performance Center, theme presets and render modules touched by v8.

## Build, tests and artifacts

- [x] Preserve green component evidence for the v7 integration, transactional config rollback, artifact provenance/license packaging and final Astra GUI delta.
- [x] Pass a combined v8 integration build before the final lifecycle/render follow-ups: Build #845.
- [x] Inspect Build #845 artifact provenance directly: JAR SHA-256 matched the manifest, the synthetic merge tree equaled the branch-head tree, required notices/metadata were packaged and `NESTED_JARS.txt` was empty.
- [x] Pass independent CI for the later frozen runtime lifecycle delta (Build #853), PhaseESP lifecycle changes (Build #850) and render guards (Build #855).
- [x] Preserve green CI checkpoints through the post-v8 hardening branch while changes are layered.
- [ ] Verify the final frozen `lunyx/full-client-hardening-v1` exact head and inspect its uploaded artifact before release approval.
- [ ] Verify the remapped artifact starts in the target Minecraft/Fabric environment.
- [ ] Run the rebuild workflow from a clean dedicated staging checkout at the final frozen v8 source head and inspect its generated artifact.

## Security, persistence and distribution

- [x] Keep Telegram bot credentials runtime-only and redact them from persisted config.
- [x] Preserve atomic writes, safe path handling and bounded configuration/preset/map-cache reads.
- [x] Preserve transactional rollback for preset/startup JSON application and exact-byte rollback for failed preset replacement.
- [x] Harden rebuild destination validation and archive-entry preflight.
- [x] Package DiscordIPC Apache-2.0 attribution/license and third-party notices in runtime/source artifacts.
- [x] Record direct dependency roles, distribution expectations and upstream license evidence.
- [x] Confirm the inspected v8 Build #845 runtime artifact contains no nested JAR payloads under `META-INF/jars/`.

## Release

- [ ] Complete live client launch, reconnect/respawn, GUI/HUD, render and long-session smoke checks.
- [ ] Complete controlled-server compatibility checks for the supported module matrix.
- [ ] Complete the final fresh-upstream staging rebuild.
- [ ] Review the final frozen full-client-hardening diff and exact-head artifact evidence on Draft PR #160.
- [ ] Obtain explicit approval before any merge to `main` or public release.

No public release or merge is part of this checklist update.
