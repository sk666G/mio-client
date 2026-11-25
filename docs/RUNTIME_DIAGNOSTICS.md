# Runtime diagnostics

Schizo keeps a bounded in-process record of recoverable runtime failures so failures are visible without retaining exception object graphs for the entire session.

## What is recorded

The diagnostics registry currently covers event callbacks, module lifecycle transitions, setting callbacks and scheduled client-thread tasks. Each record includes the area, owner, thread name, exception type, a bounded message, and a per-key occurrence count.

The buffer keeps the newest 128 records. Repeated identical failures are rate-limited in the log while their occurrence count continues to increase.

## Performance Center

Open **Performance Center** and select **DIAGNOSTICS**. The table shows newest failures first. The status line also reports queued/outstanding deferred tasks and the total recoverable failure count.

**RESET** clears profiler samples and runtime diagnostics for the current process.

## Failure semantics

Diagnostics do not make fatal VM errors recoverable. Ordinary runtime exceptions at explicit containment boundaries are recorded and contained; fatal errors continue to propagate. Module enable/disable also cancels owned deferred tasks so stale work does not run after the module lifecycle has moved on.
