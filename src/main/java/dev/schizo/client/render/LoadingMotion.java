package dev.schizo.client.render;

/** Pure bounded loading animation geometry. Never invents progress or delays a reload. */
public final class LoadingMotion {
   private LoadingMotion() {}

   public static float unit(float value) {
      return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
   }

   public static float opacity(long now, long fadeInStart, long fadeOutStart) {
      float in = fadeInStart < 0 ? 1 : unit((now - fadeInStart) / 500.0F);
      float out = fadeOutStart < 0 ? 1 : 1 - unit((now - fadeOutStart - 1000.0F) / 1000.0F);
      return Math.min(in, out);
   }

   public static float logoScale(float opacity, boolean reducedMotion) {
      if (reducedMotion) return 0.75F;
      float inverse = 1 - unit(opacity);
      return 0.75F * (0.85F + 0.15F * (1 - inverse * inverse * inverse));
   }

   public static int progressWidth(int width, float progress) {
      return Math.round(Math.max(0, width) * unit(progress));
   }
}
