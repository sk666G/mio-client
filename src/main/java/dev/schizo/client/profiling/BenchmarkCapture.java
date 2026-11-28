package dev.schizo.client.profiling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Opt-in frame capture. Stops at 30 seconds or the sample cap; no frame-path allocations. */
public final class BenchmarkCapture {
   public static final int MAX_FRAMES = 120_000;
   public static final long DURATION_NANOS = 30_000_000_000L;
   public static final BenchmarkCapture GLOBAL = new BenchmarkCapture();

   private volatile boolean active;
   private long[] samples;
   private final double[] bucketTotals = new double[30];
   private final int[] bucketCounts = new int[30];
   private long start, lastObserved, sum, max, hitches;
   private int count;
   private boolean first;
   private BenchmarkSession completed;

   public synchronized void start(long now) {
      if (active) return;
      samples = new long[MAX_FRAMES];
      Arrays.fill(bucketTotals, 0);
      Arrays.fill(bucketCounts, 0);
      count = 0;
      sum = max = hitches = 0;
      start = now;
      lastObserved=now;
      first = true;
      completed = null;
      active = true;
   }

   public boolean isActive() { return active; }

   public void onFrame(long now, long duration) {
      if (!active) return;
      accept(now, duration);
   }

   private synchronized void accept(long now, long duration) {
      if (!active) return;
      long elapsed = now - start;
      if (elapsed < 0 || elapsed >= DURATION_NANOS || count >= MAX_FRAMES) {
         active = false;
         return;
      }
      // Only distinct, ordered frame timestamps can contribute disjoint intervals.
      long interval=now-lastObserved;
      if(interval<=0) return;
      lastObserved=now;
      // The first measured frame may have started before capture was requested.
      if (first) {
         first = false;
         return;
      }
      if (duration <= 0 || duration > interval) return;
      samples[count++] = duration;
      sum += duration;
      max = Math.max(max, duration);
      if (duration > 50_000_000L) hitches++;
      int bucket = (int)(elapsed / 1_000_000_000L);
      bucketTotals[bucket] += duration / 1_000_000.0;
      bucketCounts[bucket]++;
      if (count == MAX_FRAMES) active = false;
   }

   /** Stop and summarize on explicit UI action, never sort on the render hot path. */
   public synchronized BenchmarkSession finish() {
      active = false;
      if (samples == null) return completed;
      if (count > 0) {
         Arrays.sort(samples, 0, count);
         long p99 = samples[Math.min(count - 1, (int)Math.ceil(count * 0.99) - 1)];
         int last = bucketCounts.length - 1;
         while (last > 0 && bucketCounts[last] == 0) last--;
         List<Double> timeline = new ArrayList<>(last + 1);
         for (int i = 0; i <= last; i++) timeline.add(bucketCounts[i] == 0 ? 0.0 : bucketTotals[i] / bucketCounts[i]);
         completed = new BenchmarkSession(count, sum / 1_000_000_000.0,
            sum / (double)count / 1_000_000.0, p99 / 1_000_000.0, max / 1_000_000.0, hitches, timeline);
      }
      samples = null;
      return completed;
   }

   public synchronized int frameCount() { return count; }
   public synchronized boolean hasCapture() { return samples != null || completed != null; }
}
