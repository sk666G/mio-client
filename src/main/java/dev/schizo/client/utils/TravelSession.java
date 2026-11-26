package dev.schizo.client.utils;

/** Distance within one player/level identity. Large discontinuities are reported as teleports. */
public final class TravelSession {
   private Object player;
   private Object level;
   private double x, y, z;
   private double distance;
   private long started;
   private int discontinuities;

   public void sample(Object player, Object level, double x, double y, double z, long now) {
      if (player == null || level == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
         this.clear();
         return;
      }
      if (this.player != player || this.level != level) {
         this.clear();
         this.player = player;
         this.level = level;
         this.started = now;
      } else {
         double dx = x - this.x, dy = y - this.y, dz = z - this.z;
         double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (step <= 64.0) this.distance += step;
         else this.discontinuities++;
      }
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Snapshot snapshot(long now) {
      long duration = this.player == null ? 0 : Math.max(0, now - this.started);
      return new Snapshot(this.distance, duration / 1_000_000_000.0, this.discontinuities);
   }

   public void clear() {
      this.player = null;
      this.level = null;
      this.distance = 0;
      this.discontinuities = 0;
   }

   public record Snapshot(double distance, double seconds, int discontinuities) {}
}
