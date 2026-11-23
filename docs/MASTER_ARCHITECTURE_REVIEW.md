# Master architecture review

**Review branch:** `codex/schizo-master-integration`  
**Review base:** `lunyx/theme-presets` at `95335a877ee4b8d8472479f20db0f75381dcf6fb`  
**Scope:** architecture and static source review of the integrated Java/Fabric client, its build/rebuild workflows, persistence, networking, profiler and multiplayer-facing modules. This report records observed contracts and verified risks; static inspection is not runtime validation.

## System map

### Startup and ownership

`ClientInitializer` builds the shared services in dependency order: event bus and manager services, module registration, HUD module registration, command registration, event publication, then configuration manager setup and configuration loading. `ClientContext` exposes much of this state through mutable static references. That is a practical fit for this single-client process, but it makes construction order and test isolation important. GUI screens are created lazily from the same shared context.

Modules register their event handlers when enabled and unregister when disabled. HUD widgets are module-backed and use the same settings and persistence model as ordinary modules. There is no separate per-world module container; subscribed listeners live for the client lifetime or for the module's enabled lifetime.

### Events and threading

`EventBus` scans annotated one-argument, void methods during registration and caches listeners by event class. Instance lookups use identity semantics; static listener lookup is concurrent. Per-event arrays are immutable snapshots updated when subscriptions change. Dispatch is synchronous on the thread that posts the event. Priority order is descending, equal-priority insertion is stable, cancellation stops cancellable dispatch, and callback exceptions propagate from the bus.

Dispatch is not globally single-threaded. Minecraft tick/render hooks normally post from the client/render thread, while the connection channel read hook posts packet-receive events on Netty's I/O thread. Some packet callbacks are deliberately moved to the client thread by vanilla's packet handling. Consumers must therefore treat packet receive events as potentially concurrent with client events. The profiler uses bounded pre-registered statistics and concurrent counters; its disabled dispatch path bypasses timing.

### Modules, commands and settings

`ModuleRegistry` owns the module instances and explicit registration list. The command registry constructs Brigadier command nodes from the command declarations and keeps aliases in the shared tree. Context profiles, named HUD layouts, integrations, and the profiler are registered through this existing command path; the profiler integration was made additive to the newer command registry.

Settings/configuration are serialized through configurable objects and preset categories. Preset operations use validated names and atomic file writing from the reliability-hardening stack. The UI and commands call those same registries rather than maintaining alternate preset stores.

`TaskScheduler` uses two client-thread queues with one shared 512-task capacity. Cross-thread submissions reserve capacity before being sent to Minecraft's client executor. Each tick has a shared 64-task inspection/execution budget. The overflow policy rejects the newest task; legacy void APIs log rejection and `trySchedule…` returns an optional cancellable handle. Runtime task exceptions are reported and later work continues. Client shutdown cancels queued/reserved work and records that cancellation.

### HUD and screens

HUD modules render through the widget renderer and `HudAnchorGroup`. The profiler measures direct widget renderer calls reached through the HUD group; HUD editor previews are intentionally separate from ordinary in-game HUD timing. `HudEditorScreen` owns drag/resize input and delegates reversible layout changes to its history/layout components. Named layouts are stored through the layout store. ClickGUI, HUD Studio, palette, integration and profile screens share client theme/config services, but visual consistency and in-game layout still require human runtime review.

### Persistence and rebuild path

Main configuration objects are serialized as JSON and written through `FileUtils.writeUtf8Atomically`; presets and named layouts use their own validated stores. `ClientDataDirectory` stages legacy directory migration and avoids following links. `ContextProfileRegistry` snapshots preset-category JSON around server changes, restores the baseline before applying another matching profile, and selects rules through the pure `ContextProfileMatcher`.

The upstream rebuild script verifies a pinned source commit, preflights ZIP entries, maps paths, creates a staging tree and backs up replaceable destination entries before installation. This pass adds a clean Git-root requirement and explicit replacement confirmation; it keeps `.git`, `.github` and `tools` outside the replacement set.

### Networking and integrations

Webhook delivery uses asynchronous Java HTTP requests. Telegram credentials are held in runtime state rather than serialized; the persisted config retains integration type and chat ID, so a Telegram bot token must be re-entered after restart. Notification delivery history is bounded and payload-free. Network operations are not performed while holding the notification queue lock, and delivery tests cover retry/failure paths from the reliability branch. Integration status commands expose configuration and recent delivery state.

### Performance instrumentation

`FrameTiming` uses fixed primitive arrays for FPS buckets and bounded rolling frame samples. Its one-percent-low calculation sorts reusable scratch storage on a throttled read; it is not per-frame allocation. `EventProfiler` is off by default, times event callbacks only when enabled, and separately records direct HUD widget rendering. Callback/module totals can be inclusive when callbacks post nested events; they must not be summed as frame time.

## Review findings and disposition

| Finding | Evidence and impact | Disposition |
| --- | --- | --- |
| The profiler commit overlapped newer command and workflow files. | The profiler branch predates the theme stack's Context Profile, HUD layout and Integration commands. Replacing whole files would lose those registrations. | Integrated only profiler imports/registration and the branch trigger into the newer files; verified by the successful Actions run on the integration commit. |
| Two modules existed solely to interfere with server validation. | `GrimDisablerModule` delayed ping handling and manipulated player flight state; `RaytraceBypassModule` explicitly described bypassing NCP raytracing and generated fake rotations. Both were registered, and the rebuild script injected them. | Removed their source files and registrations; updated the rebuild importer to exclude source and registration on future rebuilds. No live server behavior was tested. |
| Legacy `autoez` migration ignored `File.renameTo()` failure. | `ConfigurationManager.prepareDirectories()` did not verify the move and swallowed all exceptions. Existing destination conflicts could leave stale data without explanation. | Replaced it with a no-follow checked NIO move, collision preservation and file-specific logging. |
| Profile restore failures discarded recovery state. | `restoreActiveProfile()` logged failed category restores, then cleared all modified-category state. Disable/disconnect paths also discarded the baseline unconditionally. | Failed categories now remain pending; applying a new context stops if restore fails; baseline capture is all-or-nothing and disconnect preserves failed recovery state. Existing tests exercise matcher precedence; this lifecycle still needs an initialized-client regression test. |
| Deferred client-thread tasks could accumulate without bound and task exceptions ran inside `removeIf`. | The scheduler accepted cross-thread callbacks before any capacity check and executed tasks while holding its collection lock. | Replaced with bounded reservations, FIFO queues, a shared per-tick work budget, cancellable handles, rejection reporting and shutdown cleanup. Deterministic queue tests cover capacity, dispatch, delays, cancellation, exceptions and work limits. Runtime game-loop timing remains unverified. |
| Profile rule selection has no runtime lifecycle test. | Matcher tests do not instantiate Fabric client globals or simulate connection, dimension and game-mode transitions. | Static behavior reviewed; runtime restoration and auto-preset ordering remain unverified. |
| The project still has client-global static ownership. | `ClientContext` and `ClientAccess` make service use convenient but couple lifecycle code and complicate isolated tests. | Retained to preserve architecture; no broad service-container rewrite. |
| Performance findings are not benchmarks. | Static scans identify hot-loop candidates but provide no measured call counts or frame-time attribution outside opt-in profiler results. | No performance claim is made. Capture in-game profiler data before changing entity/HUD loops. |

## Multiplayer compatibility audit

The two validation-circumvention modules are removed. This is a source-level compatibility correction, not proof of universal server compatibility. Remaining movement/combat modules can deliberately request client behavior that differs from vanilla expectations; whether a server accepts those packets depends on Minecraft version, server implementation, plugin and module settings. This pass does not claim that such behavior was tested against a controlled server.

The conservative result established here is limited: Schizo no longer ships the two named modules that exist to defeat validation, and the build importer cannot restore them. A full per-module protocol matrix and controlled local-server scenarios remain release work. No public server testing was performed.

## Verification boundary

The integration commit's GitHub Actions build ran Java tests and `./gradlew --no-daemon clean build` successfully. Later commits require their own Actions run before their code is considered build-verified. No live Minecraft launch, GUI interaction, controlled multiplayer session, benchmark, or release-JAR runtime test is implied by a successful CI build.
