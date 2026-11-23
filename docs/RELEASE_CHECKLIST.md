# Release checklist

Status is intentionally conservative. A checked item requires recorded evidence.

## Source and integration

- [x] Start from the latest inspected `lunyx/theme-presets` head (`95335a877ee4b8d8472479f20db0f75381dcf6fb`).
- [x] Integrate the Module/Event Profiler while preserving newer command registrations.
- [x] Keep the reliability-hardening stack in branch ancestry.
- [x] Remove GrimDisabler and RaytraceBypass implementations, registration and rebuild-script reinjection.
- [ ] Complete per-module multiplayer compatibility matrix and authorized controlled-server scenarios.
- [ ] Complete visual QA across ClickGUI, HUD Studio, command palette, integration/profile screens and all theme presets.

## Build and tests

- [x] Run automated tests and clean Gradle build in GitHub Actions for the profiler integration commit.
- [ ] Obtain a successful GitHub Actions run for the latest branch head after all final changes.
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
