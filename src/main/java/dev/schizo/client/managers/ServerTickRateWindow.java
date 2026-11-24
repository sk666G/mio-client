package dev.schizo.client.managers;

import java.util.ArrayDeque;

/** Bounded rolling server tick-rate estimator driven by monotonic packet intervals. */
final class ServerTickRateWindow {
   static final int CAPACITY = 20;
   private final ArrayDeque<Float> samples = new ArrayDeque<>(CAPACITY);
   private float latest;
   private float average;

   void recordIntervalNanos(long elapsedNanos) {
      if (elapsedNanos <= 0L) {
         return;
      }
      float measured = Math.min(20.0F, Math.max(0.0F, 20_000_000_000.0F / (float)elapsedNanos));
      this.latest = measured;
      if (this.samples.size() >= CAPACITY) {
         this.samples.removeFirst();
      }
      this.samples.addLast(measured);

      float total = 0.0F;
      for (float sample : this.samples) {
         total += Math.min(20.0F, Math.max(0.0F, sample));
      }
      float computed = this.samples.isEmpty() ? 0.0F : total / this.samples.size();
      this.average = computed > 19.95F ? 20.0F : computed;
   }

   float latest() {
      return this.latest;
   }

   float average() {
      return this.average;
   }

   int sampleCount() {
      return this.samples.size();
   }

   void clear() {
      this.samples.clear();
      this.latest = 0.0F;
      this.average = 0.0F;
   }
}
