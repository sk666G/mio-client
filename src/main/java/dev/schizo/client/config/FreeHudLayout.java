package dev.schizo.client.config;

import com.google.gson.JsonObject;
import dev.schizo.client.render.HudHorizontalAlignment;
import dev.schizo.client.render.HudVerticalAlignment;

/** Decoded free-widget coordinates in the legacy module configuration format. */
public record FreeHudLayout(HudHorizontalAlignment horizontal, HudVerticalAlignment vertical, float x, float y) {
   public static FreeHudLayout read(JsonObject layout) {
      return new FreeHudLayout(
         alignment(layout, "h", HudHorizontalAlignment.values()),
         alignment(layout, "v", HudVerticalAlignment.values()),
         coordinate(layout, "x"), coordinate(layout, "y")
      );
   }

   private static <T> T alignment(JsonObject layout, String key, T[] values) {
      int ordinal = layout.has(key) ? layout.get(key).getAsBigDecimal().intValueExact() : 0;
      if (ordinal < 0 || ordinal >= values.length) {
         throw new IllegalArgumentException("Invalid HUD alignment: " + key);
      }
      return values[ordinal];
   }

   private static float coordinate(JsonObject layout, String key) {
      if (!layout.has(key)) {
         throw new IllegalArgumentException("Missing HUD coordinate: " + key);
      }
      float value = layout.get(key).getAsFloat();
      if (!Float.isFinite(value)) {
         throw new IllegalArgumentException("Invalid HUD coordinate: " + key);
      }
      return Math.clamp(value, 0.0F, 1.0F);
   }
}
