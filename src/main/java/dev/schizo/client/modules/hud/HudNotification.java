/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import java.awt.Color;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class HudNotification {
   protected String text;
   protected final Color color;
   private final LongSupplier nanoTime;
   private final long startTimeNanos;
   private long endTimeNanos;

   public String getText() {
      return this.text;
   }

   public long getElapsedMillis() {
      long elapsedNanos = this.nanoTime.getAsLong() - this.startTimeNanos;
      return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, elapsedNanos));
   }

   public long getLifetimeMillis() {
      return TimeUnit.NANOSECONDS.toMillis(this.endTimeNanos - this.startTimeNanos);
   }

   public boolean isExpired() {
      return this.nanoTime.getAsLong() - this.endTimeNanos >= 0L;
   }

   public boolean hasEvenStartMillis() {
      long monotonicStartMillis = TimeUnit.NANOSECONDS.toMillis(this.startTimeNanos);
      return (monotonicStartMillis & 1L) == 0L;
   }

   protected void finishAfter(long durationMillis) {
      long durationNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, durationMillis));
      this.endTimeNanos = this.nanoTime.getAsLong() + durationNanos;
   }

   public HudNotification(String text, long durationMillis, Color color) {
      this(text, durationMillis, color, System::nanoTime);
   }

   HudNotification(String text, long durationMillis, Color color, LongSupplier nanoTime) {
      this.text = text;
      this.color = color;
      this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
      this.startTimeNanos = this.nanoTime.getAsLong();
      this.endTimeNanos = this.startTimeNanos
         + TimeUnit.MILLISECONDS.toNanos(Math.max(0L, durationMillis));
   }

   public Color getColor() {
      return this.color;
   }

   public void update() {
   }
}
