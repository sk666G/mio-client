# Expedition workflows and engineering evidence

This document describes Draft PR #167 (`codex/anarchy-workflows-v2`). It is based on
`lunyx/runtime-validation-v1` at `c2cc55d53dc86e125fcbb9dae54fd1bdcc0ba1a6`.
Its PR comparison base is pinned at that SHA through `codex/anarchy-workflows-base-c2cc55d`.
The frozen v8 branch is unchanged. The earlier PR #137 was closed as superseded;
its workflow changes were reapplied onto the newer runtime-validated implementation.

## Using the workflows

With the default command prefix, open `;expedition` (`;travel` and `;discoveries`
are aliases). The screen has four tabs:

| Tab | Behavior and limits |
| --- | --- |
| Discoveries | Search stash, waypoint and currently visible NewChunks snapshots. Defaults to the current server, with coordinates hidden. Stashes record first/last observation timestamps; old records have unknown timestamps. |
| Travel | Current player/level session distance, monotonic duration, rocket/food counts, hunger, elytra durability, destination distance/estimated arrival and existing travel-module status. |
| Profiles | Context rules selected in the same category/application order as the actual profile path. Shows missing presets and pending recovery. JSON field differences compare each selected preset independently against current state; this is not a simulation of the combined transaction. |
| Compatibility | Existing source-review classifications and reasons from `ModuleCompatibility`. These are not claims of server acceptance. |

Select a discovery and choose **Use as waypoint** while on its server and dimension.
A stash or chunk has no observed altitude: the waypoint uses your current altitude.
Repeated labels receive numeric suffixes; an existing waypoint at the same location
is reused. The selected discovery also becomes this screen's travel destination.
Destination selection is local to the screen instance and does not start pathing.

**Copy selected** requires confirmation. Discovery JSON contains exact coordinates
and server identity. It is blocked while CoordinatePrivacy is enabled. Periodic
refresh retains a selection only if the complete selected row still matches.
Page buttons and wheel scrolling page the snapshot; search is limited to 128 characters.
Profile JSON is captured on entering/refreshing that tab, rather than every frame or
search keystroke. Other views refresh at most once per 500 ms tick interval.

Travel distance resets on player or level identity replacement and disconnect.
Samples over 64 blocks are counted as discontinuities and excluded from distance.
ETA uses whole-session average distance/s; stationary time and vertical travel affect
it. It is an estimate, not route planning or a predicted arrival guarantee.

## Settings and diagnostics

| Area | New behavior |
| --- | --- |
| AutoWalk | `ManualOverride=true` yields to directional keys. `PauseUnloaded=true` pauses if the chunk four blocks ahead is absent. Existing item-use/mining pause settings remain available. This does not inspect hazards or plan a route. |
| AutoReconnect | `MaxAttempts=10` bounds a retry sequence (range 1–100). `Backoff=true` increases short delays up to 60 seconds and never shortens a longer configured interval. Toggle, a new server, or a successfully joined player tick starts a new sequence. |
| Mainhand | `EmergencyPriority=true` lets existing configured health/lethal protection take precedence over AutoEat's main-hand food selection when AutoSelect is enabled and its configured slot already contains a totem. Offhand food remains eligible. Existing inventory delays and health rules are retained. |
| AutoEat / Mainhand | Temporary selection restoration requires the same player, level, slot and selection revision. Another selection owner or a manual away-and-back selection invalidates restoration. AutoEat's container swap-back additionally checks menu identity and expected item contents/components. |
| Settings | `CoordinatePrivacy=false` by default. When enabled it suppresses the Position and Map HUD, waypoint coordinate details, automatic coordinate clipboard logging, stash coordinate notifications, coordinate commands and Expedition exports. Unsent webhook chunks/retries are also blocked. |
| AutoCrystal / AnchorAura | Runtime status explains existing pause conditions and reports scanning/no-placement/ready state and completed-scan age. Client-thread scans invalidate their owner on player as well as level replacement. |
| ClickGUI | Search also considers module descriptions and setting names. Module tooltips show runtime status. |
| Performance Center | Enable the existing event profiler and select **Scans** for processed cells and average/maximum/total slice time for AutoCrystal, AnchorAura, Holes, Search and Tunnels. Reset clears these counters too. |

CoordinatePrivacy is scoped to the listed Schizo outputs. It cannot retract already
sent messages or clipboard contents, does not redact user-supplied labels, chat
history, Minecraft F3 or other mods, and is not a promise of anonymous screenshots.
The delivery gate intentionally suppresses pending webhook messages rather than
trying to infer whether arbitrary text contains coordinates.

## HUD layout previews

Use `;hudlayout save "name"`, then `;hudlayout preview "name"`. **Keep layout**
commits one undo transaction. **Cancel**, Escape or screen replacement restores the
exact starting layout if this preview still owns the current layout. It preserves a
newer external layout instead of restoring over it. Invalid saved layouts leave the
previous layout intact and disable Keep. Preview rendering leaves the HUD unblurred.
Ownership compares editable grouping/alignment/scale and free coordinates; rendering's
recalculation of anchored/aligned coordinates cannot revoke the preview's ownership.
Existing layout loading, snapping, grouping, undo/redo and bounded history remain
provided by the underlying HUD editor.

## Recording controlled compatibility tests

`;compat evidence template AutoWalk "local-walk"` creates an **UNTESTED** JSON
template in `schizo/compatibility-evidence/`. Existing files are never replaced.
The template stores a SHA-256 of module JSON, not the settings payload itself.
Complete the exact 40-character build commit, server software/version, plugin
name/version list, scenario, observations and `PASSED` or `FAILED` result after a
controlled test, then use `;compat evidence validate "local-walk"`.

Validation checks the local receipt's format and required fields. It does not
authenticate the author, reproduce the test or update any module certification.
Files are limited to 256 KiB and use the existing safe-name/no-follow read policy.
Do not put credentials or private server identifiers in observations.

## Ownership ledger for this change

| System | Owner / thread | Mutable state and cleanup | Failure behavior / coverage |
| --- | --- | --- | --- |
| Reconnect | Captured disconnect-screen/server identity; client/render thread | Monotonic timer per screen; retry count in module; only current screen may auto-connect; resize preserves captured target | Bounded sequence and delay policy unit tests; real network retry still manual |
| Temporary hotbar | One module; local player and level identity on client thread | Revision invalidated by selected-slot events and native inventory selection changes; restore clears references in finally | Pure revision tests and real local-world away/back regression |
| AutoEat swap-back | Player + exact menu object/id | Copied expected stacks; cleanup on disable/disconnect/restore | Source review; item consumption/container transitions need controlled runtime tests |
| Travel | One player/level pair, client ticks | Constant-sized accumulator; disconnect/player/level replacement clears references | Deterministic distance/reset/discontinuity unit tests |
| Expedition | Screen instance; client thread | Immutable row snapshots, paging and cached profile snapshot; no executor/listener registration | CI screenshots of all four tabs; local-world stash identity assertion |
| Profile preview | Client thread; existing registry lock | Shared pure rule selector; at most 64 difference paths, 2,048 visited nodes and depth 32 per comparison; values excluded from labels | JSON preview bounds/escaping tests; actual profile transitions retain existing tests and live gates |
| HUD preview | Screen instance + immutable layout snapshots; client thread | Before/expected snapshots; one commit; cancel only if still owner; no deferred callback | Pure history/ownership test; real changed-layout cancellation and invalid-layout regression |
| Scan metrics | Fixed enum of five sources | Five primitive arrays, one record per profiled slice, explicit reset; no per-cell instrumentation | Aggregate/reset unit tests; disabled profiler skips timestamp/record calls |
| Combat scan publication | Exact player and level; client thread | Incremental cursor cleared when either identity changes; completed results revalidated before publication | Build/source review; placement and reconnect behavior need controlled runtime validation |
| Privacy delivery | Volatile flag; worker reads only the flag | Webhook future validates privacy and destination ownership before later chunks/retries | Held-future tests for privacy/destination replacement; no new worker pool |
| Evidence files | Synchronous explicit command action on client thread | Bounded file read, staged non-replacing creation, temporary-file cleanup | Invalid template/path and existing-file preservation tests |

No new scanner worker reads live Minecraft objects. Scan instrumentation preserves
the newer base's adaptive budgets. This branch adds no executor or periodic worker.
The existing HTTP path remains `client snapshot -> immutable payload -> HTTP future
-> destination/privacy validation -> next request`; its completion does not access
live world, player or inventory state.

## Verification and inherited work

The newer base was selected after inspecting PRs #160–#166 and their exact SHAs.
It already includes lifecycle rollback, bounded owner-scoped scheduling, frame-aware
scan budgets, registry/configuration hardening, responsive GUI work and the Fabric
client gametest harness. Those implementations were retained, rather than replaying
older manager implementations from PR #127 or the closed PR #137.

During final verification the runtime-validation branch advanced to
`66720b12dd1ca10b3f98361903ed469974e5cba1` (remaining mixin bindings and a new source
predicate). Its Builds #1921/#1922 failed; the predicate rejects direct module lookups
still present in 65 mixin sources in the existing code. The original PR-merge Build
#1924 also failed that new test. This delta is not integrated or silently reverted:
PR #167 compares against the pinned verified snapshot instead. Reconcile this upstream
gate separately before assembling the next combined candidate.

The first changed-layout gametest also exposed an invalid fixture: the first exported
widget does not necessarily support resizing. The regression now finds a real
`ResizableHudWidgetRenderer`, requires its preview to change the layout, and then
requires cancellation to restore the original. The assertion remains mandatory.

Local verification uses Java 25 for Gradle with the installed Java 21 toolchain:

```powershell
$env:GRADLE_USER_HOME = Join-Path $env:TEMP 'schizo-completion-gradle'
.\gradlew.bat --no-daemon clean build compileGametestJava
```

This passed locally, including Java tests, access-widener validation, remapJar and
remapSourcesJar. Earlier local Python rebuild-security runs encountered Windows
temporary-directory permission failures; they are not recorded as local passes.
Linux Build CI runs `python3 -m unittest discover -s tests -v` independently.

At intermediate head `110d466ac83895a2a0f121438cb5eec0045996ab`:

- [Build #1917](https://github.com/sk666G/mio-client/actions/runs/45799366030) passed.
- [Client launch smoke #36](https://github.com/sk666G/mio-client/actions/runs/45799366125) passed.
- [Client gametests #37](https://github.com/sk666G/mio-client/actions/runs/45799365973) passed; its nine screenshots were inspected. They exposed menu blur in the initial HUD preview, corrected in the subsequent change.

Final-head runs, artifact provenance/checksums and reconstruction evidence are recorded
in [Draft PR #167](https://github.com/sk666G/mio-client/pull/167). Historical runs above
must not be substituted for final-head evidence. The rebuild workflow now allows this
branch through its existing read-only pinned-upstream reconstruction path; archive,
path, symlink, staging, baseline and exact-source-overlay checks are unchanged.

## Module review and remaining gates

Changed paths were reviewed through bootstrap, command registration, native slot
selection, inventory restore, scans, GUI snapshots and teardown. Combat:
AutoCrystal/AnchorAura/Mainhand/AutoArmor; player: AutoEat; movement: AutoWalk;
misc: AutoReconnect/StashFinder/CoordLogger; render: Search/Tunnels/Waypoints;
HUD: Position/Map/editor; shared: Holes, profiles, stashes, waypoints, webhooks,
module status and profiling. Existing scheduling/event/configuration architecture
was retained from the reviewed base. This is not a claim of a fresh line-by-line
audit of every registered module.

| Remaining scenario | Exact operator check |
| --- | --- |
| Packaged distribution | Launch the remapped artifact with target Fabric/Minecraft, including existing user configs and a resource reload. Development-source CI launch is separate evidence. |
| Reconnect | Private server: refuse three connections, reach retry limit, toggle off/on, set a delay over one minute, resize disconnect screen, change selected server; confirm no stale-target attempt. |
| Player/level transitions | Repeated disconnect/reconnect, death/respawn and Nether/End transfers with representative modules active; no old placement/cursor/selection after replacement. |
| Inventory | AutoEat and Mainhand together: low-health protection, offhand food, finish consumption, change menu, manually select away/back, disable mid-action; only the current owner restores and no wrong stack moves. |
| GUI/HUD | Drag/resize/group, cancel/Keep preview, undo/redo, external replacement, viewport/GUI-scale/font change, Unicode paste, focus loss and release outside the window. Test tiny windows separately from the CI viewport. |
| Profiles | Multiple matching categories, missing/malformed preset, apply failure/recovery, dimension/game-mode change; preview does not mutate settings. |
| Privacy | Toggle with pending webhook request, clipboard action and stash notification; block subsequent delivery; confirm the explicitly excluded outputs remain understood. |
| Performance/long session | Profiler off/on in dense worlds; inspect scan counts and frame-time percentiles; run hours with trails/discoveries and repeated GUI opens. No FPS improvement has been measured here. |
| Platform | Windows Discord absent/present, media source changes and shutdown; CI Linux does not exercise these integrations. |
| Multiplayer | Record exact server/plugins/module settings and controlled scenarios in operator receipts; do not infer compatibility from source review or successful serialization. |

This branch does not supply autonomous route planning, an all-module shared inventory
arbiter, per-candidate combat rejection traces or proven compatibility with arbitrary
servers. Those are separate design/runtime tasks. No claim of bypassing any anti-cheat
is made. The removed GrimDisabler/RaytraceBypass modules remain absent.
