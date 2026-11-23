# Performance HUD

The performance HUD is an opt-in set of frame-time measurements. It is intended to show frame pacing and stutters, not to identify the exact module responsible for a slowdown.

## Metrics widget

Enable the **Metrics** HUD widget, then enable whichever rows you want:

| Setting | Meaning |
| --- | --- |
| `FPS` | Number of rendered frames in the rolling one-second FPS window |
| `FrameTime` | Duration of the latest recorded frame in milliseconds |
| `AverageFrameTime` | Arithmetic mean of recorded frame durations in the rolling one-second history |
| `OnePercentLow` | FPS derived from the 99th-percentile frame duration in that history |
| `Hitches` | Number of frames longer than 50 ms in the rolling one-second history |

A frame longer than 50 ms takes more than three 60 FPS frame budgets and is counted as a hitch. The hitch threshold is currently fixed at 50 ms.

## Frame Time Graph

Enable **Frame Time Graph** from the HUD module list. Its width and height control the widget's dimensions, while **MaxFrameTime** controls the graph's vertical scale. The horizontal guide marks 16.67 ms, the approximate frame budget for 60 FPS.

Higher spikes indicate longer frames and therefore worse frame pacing. The graph keeps a bounded history and samples the latest recorded frame once per frame sequence.

## Reading the numbers

- 16.67 ms per frame is approximately 60 FPS.
- 33.33 ms per frame is approximately 30 FPS.
- 50 ms per frame is approximately 20 FPS.
- A low 1% value or repeated hitches indicates uneven frame pacing even when the average FPS looks high.

The 1% low is an estimate from the rolling one-second sample window, not a full-session benchmark. The frame-time graph and these metrics measure the whole rendered frame. They do not attribute time to a specific Schizo module, Minecraft subsystem, GPU driver or other mod. Module-level profiling is a separate follow-up feature.
