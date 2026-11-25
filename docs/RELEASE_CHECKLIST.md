# Release checklist

Status is intentionally conservative. A checked item requires recorded evidence.

## Source and integration

- [x] Consolidate the reviewed GUI/HUD, profiler, module/reliability and runtime ownership stacks through verified candidate v6 (Build #708).
- [x] Integrate the final five Astra GUI interaction commits independently (Build #720).
- [x] Carry the stronger runtime #96 deltas and transactional preset/config rollback tests (#716/#717) into v7.
- [x] Remove GrimDisabler and RaytraceBypass implementations, registration and rebuild-script reinjection.
- [ ] Complete per-module multiplayer compatibility matrix and authorized controlled-server scenarios.
- [ ] Complete visual QA across ClickGUI, HUD Studio, command palette, integration/profile screens, Performance Center (profiler start/stop/reset and frame-history graph) and all theme presets.

## Build and tests

- [x] Run automated tests and clean Gradle builds for v6 (#708), transactional config rollback (#716/#717), artifact provenance (#718), license packaging (#719), and final Astra delta (#720).
- [ ] Obtain a successful GitHub Actions run for the complete v7 candidate after all components are integrated.
- [ ] Verify the final review artifact's `COMMIT_SHA.txt` and `SHA256SUMS.txt` against the tested runnable JAR.
- [ ] Verify remapped artifact starts in the target Minecraft/Fabric environment.
- [ ] Run the rebuild workflow from a clean dedicated staging checkout at the final v7 head and inspect the generated artifact.

## Security, data and documentation

- [x] Keep Telegram bot credentials runtime-only and redact them from persisted config.
- [x] Preserve atomic writes and safe path handling in the existing reliability stack.
- [x] Harden rebuild destination validation and archive-entry preflight.
- [x] Record architecture findings and known limitations.
- [x] Resolve DiscordIPC license status from upstream Apache-2.0 evidence, retain the full license, and package the notice/license in runtime and source artifacts.
- [ ] Review all remaining dependency versions and license metadata against authoritative sources.

## Release

- [ ] Complete the live client and controlled-server checks.
- [ ] Review the final v7 branch diff and generated JAR contents after the combined build.
- [ ] Confirm every checklist item required for release candidate review.
- [ ] Obtain explicit approval before any public release or merge.

No public release or merge is part of this branch task.
