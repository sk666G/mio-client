/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

   public static final class Snapshot {
      private static final Rank GENERAL = new Rank(2, Integer.MAX_VALUE);
      private final Map<String, Rank> ranks;

      private Snapshot(Map<String, Rank> ranks) {
         this.ranks = Map.copyOf(ranks);
      }

      public Rank rank(String moduleName) {
         return moduleName == null ? GENERAL : this.ranks.getOrDefault(moduleName, GENERAL);
      }
   }

   public static Snapshot emptySnapshot() {
      return new Snapshot(Map.of());
   }

   public static Snapshot snapshot(List<String> favorites, List<String> recent) {
      Map<String, Rank> ranks = new HashMap<>();
      if (recent != null) {
         for (int index = 0; index < recent.size(); index++) {
            String name = recent.get(index);
            if (name != null) {
               ranks.putIfAbsent(name, new Rank(1, index));
            }
         }
      }
      if (favorites != null) {
         for (int index = 0; index < favorites.size(); index++) {
            String name = favorites.get(index);
            if (name != null) {
               ranks.put(name, new Rank(0, index));
            }
         }
      }
      return new Snapshot(ranks);
   }

   public static Rank rank(String moduleName, List<String> favorites, List<String> recent) {
      return snapshot(favorites, recent).rank(moduleName);
   }
}
