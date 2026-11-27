# Health diagnostics

Schizo's local health surface summarizes runtime state without printing configuration values, webhook destinations, tokens, packet payloads or other sensitive content.

## Command

Use:

```
;health
```

The summary reports:

- Startup Safe Mode state and marker health.
- The number of module configuration values that failed validation during the most recent load.
- The total event-listener failures retained by the current process.
- Scheduler pressure as queued and outstanding delayed tasks.
- Whether the opt-in event/module profiler is currently sampling.

Up to five configuration issues are previewed by module name, setting name and failure type. The invalid value itself is not printed.

## Event failure details

Use:

```
;health events
```

This shows the bounded recent event-failure window. Each row contains only:

- listener owner/method,
- event type,
- exception type,
- a bounded exception message,
- thread name.

The event object and packet payload are never serialized into this diagnostic.

Use:

```
;health reset
```

to clear the event-failure diagnostic window and total counter. This does not change modules, configuration, Safe Mode or profiler state.

## Performance Center

The Performance Center status line exposes the same aggregate event-error count plus scheduler pressure. Its Reset control resets profiler counters and the event-failure diagnostics together.

## Interpretation

A non-zero event-failure count means at least one callback threw during the current process. Schizo records the failure before preserving the existing dispatch semantics; some outer boundaries may intentionally catch the propagated exception to keep the client alive.

A non-zero outstanding task count is not automatically a fault. It means delayed work is reserved or queued. Module-owned tasks are cancelled automatically when that module exits its lifecycle.

Safe Mode being active is not itself classified as a failure. An unsafe marker or failed Safe Mode initialization is.

These diagnostics complement logs and live testing; they are not a substitute for a Minecraft runtime smoke test.
