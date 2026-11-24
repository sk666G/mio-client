# Release checklist

Status is intentionally conservative. A checked item requires recorded evidence.

Latest verified artifact: workflow run [45276730538](https://github.com/sk666G/mio-client/actions/runs/45276730538), artifact `schizo-client` (ID `14868960536`, 7,618,189 bytes, SHA-256 `7051f9e3cc2eea3cbf1f3cdd8142a7c6c6d0606943ac8ca1466e364e70894f18`).

## Source and integration

- [x] Start from the latest inspected `lunyx/theme-presets` head (`95335a877ee4b8d8472479f20db0f75381dcf6fb`).
- [x] Integrate the Module/Event Profiler while preserving newer command registrations.
- [x] Keep the reliability-hardening stack in branch ancestry.
- [x] Remove GrimDisabler and RaytraceBypass implementations, registration and rebuild-script reinjection.
- [ ] Complete per-module multiplayer compatibility matrix and authorized controlled-server scenarios.
- [x] Bound deferred client-thread task scheduling, expose rejection/cancellation, and add deterministic queue tests.
- [ ] Complete visual QA across ClickGUI, HUD Studio, command palette, integration/profile screens and all theme presets.

## Build and tests

- [x] Run automated tests and clean Gradle build in GitHub Actions for the profiler integration commit.
- [x] GitHub Actions full build, tests, Python security tests and remapped JAR verification passed on the latest code head before this documentation-only update (run 45276730538).
- [ ] Verify remapped artifact starts in the target Minecraft/Fabric environment.
- [x] Python rebuild security tests passed after the final rebuild-script and artifact-verifier changes (10 tests).
- [ ] Run the rebuild workflow from a clean dedicated staging checkout and inspect the generated artifact.

## Security, data and documentation

- [x] Keep Telegram bot credentials runtime-only and redact them from persisted config.
- [x] Preserve atomic writes and safe path handling in the existing reliability stack.
- [x] Harden rebuild destination validation and archive-entry preflight.
- [x] Record architecture findings and known limitations.
- [x] Verified the DiscordIPC upstream Apache-2.0 license from its authoritative repository, included the full license text, and updated notices; the exact vendored revision and local modification provenance remain a release blocker.
- [ ] Review all dependency versions and license metadata against authoritative sources.

## Release

- [ ] Complete the live client and controlled-server checks.
- [ ] Review the final branch diff and generated JAR contents.
- [ ] Confirm every checklist item required for release candidate review.
- [ ] Obtain explicit approval before any public release or merge.

No public release or merge is part of this branch task.


## Lifecycle hardening follow-up

- [ ] Exercise queue rejection, delayed execution, callback failures and shutdown in a live client.
- [ ] Verify Context Profile transitions after manually changing module enable states; profile switching must preserve those states.
- [ ] Force a preset apply/restore failure and confirm the original baseline remains pending and is retried.
- [ ] Verify recovery behavior after process termination while a Context Profile restoration is pending.



## Multiplayer compatibility follow-up

- [ ] Complete source assessment for every module and relevant setting; keep unreviewed entries as `UNVERIFIED`.
- [ ] Verify the conservative activation guard, chat warning, config persistence and keybind behavior in singleplayer and a controlled multiplayer server.
- [x] ClickGUI search matches module/setting compatibility classes, evidence labels, and explanations; the complete classification matrix remains open.


- [x] CI checks remapped JAR metadata, mixin/access-widener resources, all 17 module-binding helper/invoker classes, and absence of GrimDisabler/RaytraceBypass classes.
- [x] CI verifier inspected required metadata/resources, all 17 binding/target helper pairs, and removed-module absence in the remapped JAR. Live Mixin application remains unverified.
- [x] Traced and removed the Gradle 10 `Task.project` deprecation in `processResources`; the latest detailed-warning build reported no Gradle deprecation warning. Existing Java compiler notes about deprecated APIs remain.

- [x] Latest CI cleared the Gradle 10 and Node 20 action-runtime warnings after the build-script and action updates.

- [ ] Force Context Profile restore failure, verify the one-second retry and ensure recovery does not apply a new context before all pending categories restore.

- [ ] Verify conservative policy decisions for all seven classifications and confirm disabled mode/singleplayer do not block module activation.

- [ ] Confirm searching by classification, explanation and setting warning finds the owning module and that hover warning text is readable at supported UI scales.


## Manual in-game verification

- [ ] Normal startup and clean shutdown.
- [ ] Safe Mode startup, recovery, and return to normal mode.
- [ ] HUD editor open/close, selection, drag, resize, undo/redo, save, and restore.
- [ ] Theme and named palette changes, persistence, and reset.
- [ ] Context Profile switching, dimension/game-mode changes, disable/re-enable, disconnect, and failed-restore retry.
- [ ] Task scheduling under load, delayed tasks, task exceptions, queue overflow feedback, and shutdown cancellation.
- [ ] Multiplayer compatibility tooltips, classification search, conservative blocking, and singleplayer allowance on a locally controlled or explicitly authorized server.
- [ ] Configuration backup and restore with existing user data.
- [ ] Verify the released remapped JAR starts on Minecraft 1.21.11 with the target Fabric loader.

Automated CI did not run a Minecraft game session or a multiplayer server.
