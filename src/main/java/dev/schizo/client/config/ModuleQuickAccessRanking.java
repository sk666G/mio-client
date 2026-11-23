/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.util.List;

/** Pure, deterministic ordering for favorites and recently enabled modules. */
public final class ModuleQuickAccessRanking {
   private ModuleQuickAccessRanking() {
   }

   public record Rank(int group, int index) implements Comparable<Rank> {
      @Override
      public int compareTo(Rank other) {
         int groupOrder = Integer.compare(this.group, other.group);
         return groupOrder != 0 ? groupOrder : Integer.compare(this.index, other.index);
      }
   }

   public static Rank rank(String moduleName, List<String> favorites, List<String> recent) {
      int favoriteIndex = favorites.indexOf(moduleName);
      if (favoriteIndex >= 0) {
         return new Rank(0, favoriteIndex);
      }

      int recentIndex = recent.indexOf(moduleName);
      if (recentIndex >= 0) {
         return new Rank(1, recentIndex);
      }

      return new Rank(2, Integer.MAX_VALUE);
   }
}
