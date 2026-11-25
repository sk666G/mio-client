# Release checklist

Status is intentionally conservative. A checked item requires recorded evidence.

## Source and integration

- [x] Consolidate the reviewed GUI/HUD lineage through `lunyx/music-text-format` and the broad module/reliability stack through `lunyx/module-reliability-integration-v1` (Build #696).
- [x] Reconcile the newer runtime ownership/session hardening into `lunyx/module-reliability-integration-v2` (Build #712).
- [x] Keep the Module/Event Profiler and Performance Center work in branch ancestry while preserving command registrations.
- [x] Remove GrimDisabler and RaytraceBypass implementations, registration and rebuild-script reinjection.
- [x] Remove live-world worker execution from AutoCrystal and AnchorAura placement scans (Build #715).
- [ ] Profile AutoCrystal and AnchorAura client-thread scan cost in representative combat worlds before release.
- [ ] Complete per-module multiplayer compatibility matrix and authorized controlled-server scenarios.
- [ ] Complete visual QA across ClickGUI, HUD Studio, command palette, integration/profile screens, Performance Center (profiler start/stop/reset and frame-history graph) and all theme presets.

## Build and tests

- [x] Run automated tests and clean Gradle build in GitHub Actions for the consolidated runtime candidate (Build #712) and combat thread-safety candidate (Build #715).
- [ ] Obtain a successful GitHub Actions run for the final integrated branch head after Astra/Lunyx reconciliation.
- [ ] Verify the final review artifact's `COMMIT_SHA.txt` matches the tested commit and `SHA256SUMS.txt` matches the runnable JAR.
- [ ] Verify remapped artifact starts in the target Minecraft/Fabric environment.
- [ ] Verify Python rebuild security tests after the final rebuild-script changes.
- [ ] Run the rebuild workflow from a clean dedicated staging checkout and inspect the generated artifact.

## Security, data and documentation

- [x] Keep Telegram bot credentials runtime-only and redact them from persisted config.
- [x] Preserve atomic writes and safe path handling in the existing reliability stack.
- [x] Harden rebuild destination validation and archive-entry preflight.
- [x] Record architecture findings and known limitations.
- [ ] Resolve DiscordIPC license status from authoritative upstream license evidence and update notices.
- [ ] Review all dependency versions and license metadata against authoritative sources.

## Release

- [ ] Complete the live client and controlled-server checks.
- [ ] Review the final branch diff and generated JAR contents.
- [ ] Confirm every checklist item required for release candidate review.
- [ ] Obtain explicit approval before any public release or merge.

No public release or merge is part of this branch task.
