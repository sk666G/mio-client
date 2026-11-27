# Release checklist

Status is intentionally conservative. A checked item means the capability or automated gate has recorded release-candidate evidence. Hands-on gates remain unchecked until they are actually exercised and accepted.

The authoritative release path is draft PR #171, `lunyx/final-main-integration-v1` -> `main`. PR #171 is also the source of truth for the exact final-head SHA and its last accepted workflow matrix. This checklist does not authorize a merge or public release.

See [ANARCHY_WORKFLOWS.md](ANARCHY_WORKFLOWS.md) for automated local-world workflow details. Automated launch, restart, gametest and screenshot evidence complements manual QA; it does not replace it.

## Source and integration

- [x] Consolidate the reviewed GUI/HUD, profiler, performance, module and reliability work onto the current release-candidate branch.
- [x] Preserve transactional module enable/disable rollback and owner-scoped scheduled work.
- [x] Scope deferred module/manager callbacks to their originating lifecycle, player, level or connection where required.
- [x] Harden macro and kit registry mutation against stale equal-named object references and re-entrant lifecycle callbacks.
- [x] Own pending input overrides across legitimate movement/combat module lifecycles and clear them on disable/failed-enable rollback.
- [x] Keep configuration, preset, HUD-layout, migration, evidence and backup publication create-only/no-replace where replacement would be unsafe.
- [x] Preserve custom dimension identifiers for waypoint/stash persistence without collapsing distinct custom namespaces.
- [x] Keep frame-pressure-aware incremental scan work and bounded long-session histories.
- [x] Keep profiler/diagnostic features disabled or bounded by default where they would otherwise add persistent overhead.
- [ ] Complete the supported per-module multiplayer compatibility matrix using authorized controlled-server scenarios.
- [ ] Complete hands-on visual/input QA across ClickGUI, HUD editor, command palette, Performance Center, theme presets, profile/integration screens and affected render modules.

## Automated verification lanes

- [x] Build lane runs rebuild-security tests, Gradle tests/build, remapped-artifact validation, provenance generation and verified JAR upload.
- [x] Pull-request and push Build lanes are kept separate so exact source heads can be verified in both contexts.
- [x] Headless Fabric client launch smoke is available on the release-candidate line.
- [x] Client gametests cover singleplayer lifecycle, representative GUI/HUD flows, level/player replacement and clean event-failure tracking.
- [x] Restart smoke exercises launch, shutdown and a second launch on the release-candidate line.
- [x] Windows storage tests exercise platform-sensitive persistence/no-replace behavior.
- [x] The Open-Zio rebuild workflow exercises clean staging, rebuild security, artifact provenance and remapped output checks.
- [ ] After the final source/documentation freeze, record one exact-head green matrix for all required lanes in PR #171 before release approval.

## Security, persistence and distribution

- [x] Keep Telegram bot credentials runtime-only and redact them from persisted configuration.
- [x] Preserve atomic writes, safe path handling, bounded reads and no-follow checks for configuration/preset/cache data.
- [x] Fail closed when a required pre-save configuration backup cannot be created.
- [x] Protect restored configuration files from later shutdown saves until restart.
- [x] Publish new backups, preset/HUD renames and other create-only files without replacing an existing destination.
- [x] Preserve primary failures when temporary-file cleanup also fails.
- [x] Harden rebuild destination validation, source/destination separation and archive-entry preflight.
- [x] Package DiscordIPC attribution/license and third-party notices in distributed artifacts.
- [x] Keep artifact checksum/provenance files self-verifiable from the uploaded artifact root.

## Manual release gates

- [ ] Launch the exact final remapped JAR in the intended Minecraft/Fabric and Java environment; verify startup, shutdown and clean restart.
- [ ] Exercise real multiplayer connect, disconnect, reconnect, server switch, respawn and dimension-change flows.
- [ ] Exercise GUI/HUD focus, cursor, keyboard, layout save/rename/restore and theme/profile transitions by hand.
- [ ] Exercise configuration backup/restore, preset/HUD copy/rename/import and collision/recovery behavior on target filesystems.
- [ ] Exercise optional sound/resource reload, Discord/media integration failure and recovery on normal desktop runtimes.
- [ ] Run representative module enable/disable, gameplay and long-session performance/stability smoke checks.
- [ ] Complete authorized controlled-server compatibility checks for the supported module matrix.
- [ ] Review the final exact-head diff/artifact evidence on PR #171 and explicitly approve any merge to `main`.
- [ ] Explicitly approve any public release.

No merge to `main` or public release is implied by this checklist.
