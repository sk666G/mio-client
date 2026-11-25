# Performance HUD

The Performance HUD is opt-in. It describes measured frame pacing; it does not attribute cost to a module. Use the Event Profiler separately for event callback and direct HUD widget timings.

## Metrics widget

Enable the **Metrics** HUD widget, then turn on the rows you want:

| Setting | Meaning |
| --- | --- |
| `FPS` | Rendered frame count in the rolling one-second FPS bucket window. |
| `AverageFPS` | Harmonic FPS from frame intervals retained in the rolling one-second sample. |
| `FrameTime` | Duration of the latest recorded frame in milliseconds. |
| `AverageFrameTime` | Arithmetic mean of retained frame durations in the rolling one-second sample. |
| `OnePercentLow` | FPS derived from the 99th-percentile frame duration in that sample. |
| `OneTenthPercentLow` | FPS derived from the 99.9th-percentile frame duration. It reports zero until at least 10,000 frame intervals are present, so the 0.1% tail has at least ten samples. |
| `FrameVariance` | Population variance of frame intervals in milliseconds squared. |
| `Hitches` | Number of intervals longer than 50 ms in the rolling one-second sample. |

FPS is a rolling count over the bucket window. Duration statistics use frame intervals whose timestamps are within one second of the newest recorded interval. The 1% low is an estimate from the 99th percentile, not the average of the slowest 1% frames. The 0.1% value uses the same percentile method and is withheld until the tail sample is large enough to be useful. A frame longer than 50 ms is counted as a hitch; that threshold is fixed.

## Frame Time Graph

Enable **Frame Time Graph** from the HUD module list. **Width** and **Height** set its dimensions; **MaxFrameTime** sets its vertical scale. The horizontal guide marks 16.67 ms (60 FPS). **TargetFPS** draws an additional frame-budget guide when it differs from 60 FPS. **AverageLine** shows the rolling one-second average frame duration.

Samples are stored in a bounded primitive ring sized to the graph width. The graph's horizontal coordinate is sample order, not elapsed wall-clock time, so it does not yet provide a seconds axis. Paused or unfocused-window behavior has not been validated in a live client.

## Reading the numbers

- 16.67 ms per frame is approximately 60 FPS.
- 33.33 ms per frame is approximately 30 FPS.
- 50 ms per frame is approximately 20 FPS.
- A low percentile value or repeated hitches indicates uneven frame pacing even when average FPS looks high.
- Variance is expressed in `ms²`; a larger value means frame durations are more spread out, not that the average frame is slower by that number.

Frame timing covers the whole rendered frame. It does not identify Minecraft, GPU, other-mod or Schizo module cost. Profile in game before changing hot loops; static inspection is not a benchmark.


## Performance Center diagnostics

The Performance Center status line also reports the bounded event-listener failure count and scheduler pressure as `TASKS queued/outstanding`. Reset clears profiler counters and the event-failure diagnostic window. These values are diagnostics, not frame-time components and must not be added to callback timings.

Incremental AutoCrystal, Search, Tunnels and VoidESP scanning uses a conservative frame-pressure budget. Healthy frame pacing retains the configured base throughput; lower FPS, slower average frame time or repeated hitches reduce work per client tick. The candidate order and scoring rules are unchanged, so the tradeoff under pressure is scan freshness/latency rather than different selection logic.
