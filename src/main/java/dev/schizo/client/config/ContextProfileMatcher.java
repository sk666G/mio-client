/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure context-rule matching and specificity ordering. */
public final class ContextProfileMatcher {
   private ContextProfileMatcher() {
   }

   public static <T> List<T> select(
      List<Entry<T>> entries, String server, String dimension, String gameMode
   ) {
      List<RankedEntry<T>> matches = new ArrayList<>();
      for (int index = 0; index < entries.size(); index++) {
         Entry<T> entry = entries.get(index);
         if ((entry.server().equals("*") || entry.server().equals(server))
            && (entry.dimension().equals("any") || entry.dimension().equals(dimension))
            && (entry.gameMode().equals("any") || entry.gameMode().equals(gameMode))) {
            int specificity = (entry.server().equals("*") ? 0 : 1)
               + (entry.dimension().equals("any") ? 0 : 1)
               + (entry.gameMode().equals("any") ? 0 : 1);
            matches.add(new RankedEntry<>(entry.value(), specificity, index));
         }
      }

      matches.sort(
         Comparator.comparingInt((RankedEntry<T> entry) -> entry.specificity)
            .thenComparingInt(entry -> entry.index)
      );
      return matches.stream().map(entry -> entry.value).toList();
   }

   public record Entry<T>(String server, String dimension, String gameMode, T value) {
   }

   private record RankedEntry<T>(T value, int specificity, int index) {
   }
}
