package dev.schizo.client.profiling;

import java.util.ArrayList;
import java.util.List;

/** Fixed-cardinality scan counters. Callers record once per slice, including failed slices. */
public final class ScanWorkTracker {
   public static final ScanWorkTracker GLOBAL = new ScanWorkTracker();

   public enum Source { AUTO_CRYSTAL, ANCHOR_AURA, HOLES, SEARCH, TUNNELS }

   private final long[] slices = new long[Source.values().length];
   private final long[] cells = new long[Source.values().length];
   private final long[] total = new long[Source.values().length];
   private final long[] maximum = new long[Source.values().length];

   public synchronized void record(Source source, long elapsed, int processed) {
      int index = source.ordinal();
      this.slices[index]++;
      this.cells[index] += Math.max(0, processed);
      long duration = Math.max(0, elapsed);
      this.total[index] += duration;
      this.maximum[index] = Math.max(this.maximum[index], duration);
   }

   public synchronized List<Snapshot> snapshot() {
      List<Snapshot> result = new ArrayList<>();
      for (Source source : Source.values()) {
         int i = source.ordinal();
         if (this.slices[i] != 0) {
            result.add(new Snapshot(source, this.cells[i], this.total[i], this.total[i] / this.slices[i], this.maximum[i]));
         }
      }
      return List.copyOf(result);
   }

   public synchronized void reset() {
      java.util.Arrays.fill(this.slices, 0);
      java.util.Arrays.fill(this.cells, 0);
      java.util.Arrays.fill(this.total, 0);
      java.util.Arrays.fill(this.maximum, 0);
   }

   public record Snapshot(Source source, long cells, long totalNanos, long averageSliceNanos, long maximumSliceNanos) {}
}
