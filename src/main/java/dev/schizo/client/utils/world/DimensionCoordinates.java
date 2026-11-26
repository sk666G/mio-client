package dev.schizo.client.utils.world;

import java.util.Locale;

public final class DimensionCoordinates {
   private DimensionCoordinates() {}

   public static String canonical(String dimension) {
      String value = dimension.toLowerCase(Locale.ROOT).replace("minecraft:", "");
      return switch (value) {
         case "nether", "the_nether" -> "the_nether";
         case "end", "the_end" -> "the_end";
         default -> value;
      };
   }

   /** NaN means the dimensions have no supported coordinate conversion. */
   public static double scale(String from, String to) {
      String source = canonical(from), target = canonical(to);
      if (source.equals(target)) return 1.0;
      if (source.equals("overworld") && target.equals("the_nether")) return 0.125;
      if (source.equals("the_nether") && target.equals("overworld")) return 8.0;
      return Double.NaN;
   }
}
