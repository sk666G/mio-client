# Known limitations

This file separates source/automation evidence from work that still needs hands-on Minecraft, platform or controlled-server validation. Draft PR #171 is the authoritative release path and should be consulted for the exact current candidate SHA and workflow status.

## Runtime and multiplayer

- The release-candidate line has automated Fabric launch, local-world gametest and restart-smoke coverage. Those workflows exercise real client startup and selected lifecycle/GUI paths, but they do not replace hands-on packaged-JAR testing, real multiplayer transitions or long-session play.
- No claim is made that every movement, combat, interaction or packet-facing module is accepted by every vanilla/plugin/modded server. Supported behavior still needs an authorized controlled-server compatibility matrix.
- Context Profile recovery, deferred module work, pending input overrides and manager packet callbacks have increasingly strict lifecycle/player/level/connection ownership. Source tests and local-world automation cover many boundaries; real connect/disconnect/reconnect, server-switch and dimension-change ordering still needs manual verification.
- AutoCrystal, AnchorAura, Hole, Search, Tunnels and related scanners keep mutable live-world reads on bounded client-thread work and revalidate results before use. Live placement behavior, scan freshness and frame-time cost still require representative in-game profiling.
- PhaseESP, Hitmarker, Skeleton, Crosshair and related render-state paths have lifecycle/render cleanup guards, but visual parity still needs hands-on testing across resource packs, GUI scales, window sizes and representative hardware.
- Discord IPC has bounded connection timing, late-connection ownership checks, transport cleanup and frame validation. A real Windows Discord runtime test is still required for named-pipe startup/failure/recovery behavior.
- Portable media integration has backend lifecycle/error isolation, but real desktop MPRIS/Windows media-session behavior still requires platform testing.

## Performance

- Event/module profiling is disabled by default. Callback timings are inclusive of nested dispatch and must not be interpreted as total frame time.
- Static allocation/hot-path cleanups and frame-pressure-aware scan budgets are not FPS claims. No final live benchmark has yet been accepted for allocation rate, GC pressure, entity-loop cost, render cost or scan latency.
- Non-fading Trails and other retained histories are bounded/pruned, but long-session memory behavior still needs representative runtime observation.
- The command palette and GUI paths have short/narrow viewport safeguards and reduced-motion support, but extreme GUI scale/resolution combinations still need visual/input QA.
- The frame graph uses sample order horizontally rather than an elapsed-seconds axis; focus-loss/pause interpretation still needs runtime review.
- Mapper/remap warnings for external Minecraft synthetic names or third-party hooks may still appear; the project does not hide external warnings merely to make logs look clean.

## Persistence and data

- Atomic writes, no-follow path validation, bounded reads, create-only/no-replace publication and transactional rollback have unit/platform coverage. They cannot prove behavior under every power-loss, antivirus, network-drive or unusual filesystem failure.
- Windows storage CI exercises platform-sensitive persistence behavior, but target-user filesystems still need manual backup/restore, preset/HUD rename/copy/import and recovery checks.
- Configuration restore deliberately protects restored bytes from later client saves until restart. Manual restore + shutdown + restart behavior still needs acceptance on the final packaged build.
- Preset refresh preserves the last complete in-memory set when enumeration fails and avoids destructive replacement; real pre-existing user preset collections still need migration/rollback smoke testing.
- Map-cache decoding is bounded and preserves unreadable files rather than overwriting them. Real existing caches and reconnect cycles remain runtime/platform validation work.

## Automation and artifact boundary

- The Build workflow validates rebuild-security tests, Gradle tests/build, remapped output, required notices, checksum/provenance files and verified JAR upload.
- The release-candidate line also has launch smoke, client gametest, restart smoke, Windows storage and clean Open-Zio rebuild workflows. Each is useful evidence only for the exact source head it tested.
- A later commit, even a documentation-only commit, is not automatically qualified by an older exact-head workflow result. PR #171 records the accepted final-head matrix.
- Headless/local-world automation does not prove real multiplayer compatibility, real desktop integration recovery, subjective GUI quality or long-session performance.
- No public release has been approved and PR #171 remains draft until the manual gates are explicitly cleared.

## Other boundaries

- Reduced Motion snaps shared timed/interpolated GUI animation primitives and suppresses selected decorative motion. Individual render-module animations are not universally classified as accessibility UI motion and still require review.
- Event listener failures are counted/attributed in bounded diagnostics, but packet dispatch remains synchronous on the posting thread; the project does not claim a universal client-thread event contract.
- This hardening line focuses on reliability, lifecycle ownership, persistence, GUI/performance quality and compatibility diagnostics. It does not add anti-cheat detection-evasion techniques or new server-protection bypass modes.
