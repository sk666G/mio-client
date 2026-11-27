package dev.schizo.client.profiling;

import java.util.List;

/** Immutable summary of a bounded frame capture; p99 is a frame-duration percentile. */
public record BenchmarkSession(long frames, double seconds, double meanMillis, double p99Millis,
                               double maxMillis, long hitches, List<Double> timeline) {
   public BenchmarkSession {
      if (frames <= 0 || frames > BenchmarkCapture.MAX_FRAMES || !Double.isFinite(seconds) || seconds <= 0
         || !Double.isFinite(meanMillis) || meanMillis <= 0 || !Double.isFinite(p99Millis) || p99Millis <= 0
         || !Double.isFinite(maxMillis) || maxMillis < p99Millis || maxMillis < meanMillis
         || hitches < 0 || hitches > frames || timeline == null || timeline.size() > 60 || timeline.isEmpty()) {
         throw new IllegalArgumentException("Invalid benchmark summary");
      }
      timeline = List.copyOf(timeline);
      for (double value : timeline) if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid timeline");
   }
   public double averageFps() { return 1000.0 / meanMillis; }
   public double p99DerivedFps() { return 1000.0 / p99Millis; }
}
