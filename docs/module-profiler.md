# Module and event profiler

The profiler is disabled when the client starts. Use these chat commands (the prefix can be changed in client settings):

- `;profiler start` enables measurements.
- `;profiler stop` stops measurements while retaining the current sample.
- `;profiler reset` clears samples but leaves the current enabled/disabled state unchanged.
- `;profiler callbacks` shows the top 10 event callbacks by accumulated time.
- `;profiler modules` shows the top 10 modules by accumulated event callback time.
- `;profiler hud` shows the top 10 direct in-game HUD widget renders.

The shorthand `;prof` is an alias.

Callback rows include listener owner, method, event type, invocation count, total, average and maximum duration. Module rows aggregate that module's event callbacks. HUD rows time the widget renderer call reached through `HudAnchorGroup`; editor preview renders are excluded.

Event callbacks are timed inclusively. If callback A synchronously posts an event and callback B runs inside it, A's duration includes B's duration. HUD widget time is reported separately and may overlap callback time. Do not sum these inclusive values as a frame-time total. Use the existing frame-time telemetry for frame duration.

Minecraft client hooks generally dispatch on the client/render thread, but packet hooks also post from Netty/network processing threads. Counters are therefore thread-safe. A live results view is a concurrent snapshot; counters can advance while it is being assembled. Reset swaps counter epochs without blocking dispatch. A callback overlapping the reset is attributed to the old or new interval according to which counter epoch it reaches at completion.

Statistics storage is fixed: 4096 callbacks, 512 module aggregates and 256 HUD widgets. Registrations beyond a cap are not shown. Event listeners that do not expose method metadata through the built-in annotated-listener path are dispatched normally but are not included.
