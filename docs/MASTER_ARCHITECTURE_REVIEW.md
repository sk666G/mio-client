# Master architecture review

**Current review branch:** `lunyx/verified-candidate-v7`  
**Historical integration base:** `lunyx/safe-mode` at `684333a2edce978cc7673ae3277c4d65d92f1d63`  
**Scope:** architecture and static source review of the integrated Java/Fabric client, its build/rebuild workflows, persistence, networking, profiler and multiplayer-facing modules. This report records observed contracts and verified risks; static inspection is not runtime validation.

## System map

### Startup and ownership

`ClientInitializer` builds the shared services in dependency order: event bus and manager services, module registration, HUD module registration, command registration, event publication, then configuration manager setup and configuration loading. `ClientContext` exposes much of this state through mutable static references. That is a practical fit for this single-client process, but it makes construction order and test isolation important. GUI screens are created lazily from the same shared context.

Modules register their event handlers when enabled and unregister when disabled. HUD widgets are module-backed and use the same settings and persistence model as ordinary modules. There is no separate per-world module container; subscribed listeners live for the client lifetime or for the module's enabled lifetime.

### Events and threading

`EventBus` scans annotated one-argument, void methods during registration and caches listeners by event class. Instance lookups use identity semantics; static listener lookup is concurrent. Per-event arrays are immutable snapshots updated when subscriptions change. Dispatch is synchronous on the thread that posts the event. Priority order is descending, equal-priority insertion is stable, cancellation stops cancellable dispatch, and callback exceptions propagate from the bus.

Dispatch is not globally single-threaded. Minecraft tick/render hooks normally post from the client/render thread, while the connection channel read hook posts packet-receive events on Netty's I/O thread. Some packet callbacks are deliberately moved to the client thread by vanilla's packet handling. Consumers must therefore treat packet receive events as potentially concurrent with client events. The profiler uses bounded pre-registered statistics and concurrent counters; its disabled dispatch path bypasses timing. The two previously unsafe combat placement workers have been removed: AnchorAura uses a deterministic bounded client-thread scan (maximum 64 candidates and roughly a 2 ms slice per tick), and AutoCrystal uses the same cursor model with a 128-candidate ceiling and roughly a 2 ms slice. Their live world/entity/visibility/damage reads therefore remain on the Minecraft client thread, followed by final result revalidation. [Combat Scan Snapshot Design](COMBAT_SCAN_SNAPSHOT_DESIGN.md) is retained as the contract for any future off-thread implementation.

### Modules, commands and settings

`ModuleRegistry` owns the module instances and explicit registration list. The command registry constructs Brigadier command nodes from the command declarations and keeps aliases in the shared tree. Context profiles, named HUD layouts, integrations, and the profiler are registered through this existing command path; the profiler integration was made additive to the newer command registry.

Settings/configuration are serialized through configurable objects and preset categories. Preset operations use validated names and atomic file writing from the reliability-hardening stack. V7 adds failure-atomic JSON application: preset categories and each startup configurable capture a defensive JSON snapshot and restore it after uncaught application failures while preserving the existing per-setting/module report-and-continue behavior. The UI and commands call those same registries rather than maintaining alternate preset stores.

### HUD and screens

HUD modules render through the widget renderer and `HudAnchorGroup`. The profiler measures direct widget renderer calls reached through the HUD group; HUD editor previews are intentionally separate from ordinary in-game HUD timing. `HudEditorScreen` owns drag/resize input and delegates reversible layout changes to its history/layout components. Named layouts are stored through the layout store. ClickGUI, HUD Studio, palette, integration and profile screens share client theme/config services, but visual consistency and in-game layout still require human runtime review.

### Persistence and rebuild path

Main configuration objects are serialized as JSON and written through `FileUtils.writeUtf8Atomically`; presets and named layouts use their own validated stores. `ClientDataDirectory` stages legacy directory migration and avoids following links. `ContextProfileRegistry` snapshots preset-category JSON around server changes, restores the baseline before applying another matching profile, and selects rules through the pure `ContextProfileMatcher`.

The upstream rebuild script verifies a pinned source commit, preflights ZIP entries, maps paths, creates a staging tree and backs up replaceable destination entries before installation. The workflow keeps a clean Git-root requirement and explicit replacement confirmation; it keeps `.git`, `.github` and `tools` outside the replacement set. V7 modernizes the staging workflow to the maintained Node-24 GitHub Action lines, pushes the successfully rebuilt staging commit before recording artifact provenance, then writes the exact staging commit SHA, runnable-JAR SHA-256 and nested-JAR inventory into the uploaded artifact.

### Networking and integrations

Webhook delivery uses asynchronous Java HTTP requests. Telegram credentials are held in runtime state rather than serialized; the persisted config retains integration type and chat ID, so a Telegram bot token must be re-entered after restart. Notification delivery history is bounded and payload-free. Network operations are not performed while holding the notification queue lock, and delivery tests cover retry/failure paths from the reliability branch. Integration status commands expose configuration and recent delivery state.

### Build and distribution evidence

The ordinary build workflow and the fresh-upstream staging workflow both require one runnable remapped JAR and one source JAR, verify `fabric.mod.json` plus the packaged DiscordIPC Apache-2.0 license/third-party notice, and record artifact provenance. The normal workflow records the tested GitHub commit; the rebuild workflow records the exact successfully pushed staging commit. Both generate `NESTED_JARS.txt` from `META-INF/jars/` so release review can classify what Schizo actually redistributes instead of inferring distribution from Gradle configuration. Direct dependency roles and upstream license evidence are tracked in [Dependency and distribution review](DEPENDENCY_REVIEW.md).

Diagnostic Build #709 traced the Gradle-10 `Task.project` warning to execution-time access of `project.version` in `processResources`. V7 captures the version during configuration, and Build #722 confirmed the targeted warning is absent. V7 also moves checkout, Java setup, Gradle setup and artifact upload to the maintained Node-24 action lines.

### Performance instrumentation

`FrameTiming` uses fixed primitive arrays for FPS buckets and bounded rolling frame samples. Rolling average FPS and population frame-time variance use maintained sums; 1% and 0.1% percentile calculations sort reusable scratch storage on a throttled read. The graph has fixed 60 FPS and configurable target/average guides. `EventProfiler` is off by default, times event callbacks only when enabled, and separately records direct HUD widget rendering. Callback/module totals can be inclusive when callbacks post nested events; they must not be summed as frame time.

## Review findings and disposition

| Finding | Evidence and impact | Disposition |
| --- | --- | --- |
| The profiler commit overlapped newer command and workflow files. | The profiler branch predates the theme stack's Context Profile, HUD layout and Integration commands. Replacing whole files would lose those registrations. | Integrated only profiler imports/registration and the branch trigger into the newer files; verified by the successful Actions run on the integration commit. |
| Two modules existed solely to interfere with server validation. | `GrimDisablerModule` delayed ping handling and manipulated player flight state; `RaytraceBypassModule` explicitly described bypassing NCP raytracing and generated fake rotations. Both were registered, and the rebuild script injected them. | Removed their source files and registrations; updated the rebuild importer to exclude source and registration on future rebuilds. No live server behavior was tested. |
| Legacy `autoez` migration ignored `File.renameTo()` failure. | `ConfigurationManager.prepareDirectories()` did not verify the move and swallowed all exceptions. Existing destination conflicts could leave stale data without explanation. | Replaced it with a bounded no-follow migration helper that rejects symlinks and special entries, preserves an existing destination and logs outcomes. |
| Profile restore failures discarded recovery state. | `restoreActiveProfile()` logged failed category restores, then cleared all modified-category state. Disable/disconnect paths also discarded the baseline unconditionally. | Failed categories now remain pending; applying a new context stops if restore fails; baseline capture is all-or-nothing and disconnect preserves failed recovery state. `ContextProfileRestoreStateTest` covers capture failure, partial restore, retained failures and retry success. Registry/event orchestration still needs initialized-client coverage. |
| Deferred client-thread tasks previously had no backpressure. | The current `TaskScheduler.ClientThreadTaskQueue` delegates to `BoundedCountdownTaskQueue`; reservations count against capacity before dispatch, each queue accepts at most 4,096 outstanding tasks, and each pass runs at most 256. | Resolved in source. Queue shutdown rejects new work and cancels queued/reserved work. `BoundedCountdownTaskQueueTest` covers capacity reservations, cancellation, bounded draining, FIFO order, failure isolation, shutdown and concurrent producers. |
| Profile rule selection has no runtime lifecycle test. | Matcher tests do not instantiate Fabric client globals or simulate connection, dimension and game-mode transitions. | Static behavior reviewed; runtime restoration and auto-preset ordering remain unverified. |
| The project still has client-global static ownership. | `ClientContext` and `ClientAccess` make service use convenient but couple lifecycle code and complicate isolated tests. | Retained to preserve architecture; no broad service-container rewrite. |
| AutoCrystal and AnchorAura placement workers read live world state off-thread. | Their former executor paths called placement/damage/visibility helpers over mutable `ClientLevel`, player, entity and block-state data; cancellation did not provide snapshot isolation. | **Resolved in source:** both executor paths were removed and replaced with deterministic bounded client-thread scan cursors, preserving publication-time revalidation. AnchorAura passed Build #699 and AutoCrystal passed Build #707. Live performance/gameplay validation remains required. |
| Preset/startup configuration application could leave partial live state after an uncaught loader failure. | File writes were atomic, but live `fromJson` application had no rollback boundary. | V7 adds `JsonStateTransaction`; preset categories and individual startup configurables restore a defensive pre-apply snapshot after uncaught failures. Standalone Builds #716/#717 passed before integration. |
| Review artifacts previously did not identify the exact tested source commit or redistributed nested libraries. | A green build alone did not prove artifact provenance or distribution contents. | V7 records commit SHA, runnable-JAR SHA-256 and `NESTED_JARS.txt`; CI verifies packaged license/notice files. Build #722 passed the combined provenance/license checks. |
| The normal CI and rebuild workflow used deprecated Node-20 GitHub Action majors. | GitHub runner logs warned about setup-java v4 and Node 20 action runtimes. | V7 moves the affected actions to maintained Node-24 major lines; Build #722 has no targeted old-action warning. |
| Performance findings are not benchmarks. | Static scans identify hot-loop candidates but provide no measured call counts or frame-time attribution outside opt-in profiler results. | No performance claim is made. Capture in-game profiler data before changing entity/HUD loops. |

## Multiplayer compatibility audit

The two validation-circumvention modules are removed. This is a source-level compatibility correction, not proof of universal server compatibility. Remaining movement/combat modules can deliberately request client behavior that differs from vanilla expectations; whether a server accepts those packets depends on Minecraft version, server implementation, plugin and module settings. This pass does not claim that such behavior was tested against a controlled server.

The source review disposition and exact unsupported groups are recorded in [Multiplayer Compatibility Review](MULTIPLAYER_COMPATIBILITY.md). The conservative result established here is limited: Schizo no longer ships the two named modules that exist to defeat validation, and the build importer cannot restore them. A full per-module protocol matrix, compatibility activation profile and controlled local-server scenarios remain release work. No public server testing was performed.

## Verification boundary

Component branches passed their own GitHub Actions builds before consolidation. V7's integrated runtime/config/GUI/distribution baseline passed Build #722; commits after that build are documentation/workflow-evidence refinements and require one final head build before release-candidate sign-off. No live Minecraft launch, GUI interaction, controlled multiplayer session, benchmark, or release-JAR runtime test is implied by a successful CI build.
