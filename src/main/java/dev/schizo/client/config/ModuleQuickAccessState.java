/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Pure bounded state model for favorite and recently enabled module names. */
public final class ModuleQuickAccessState {
   public static final int MAX_FAVORITES = 128;
   public static final int MAX_RECENT = 20;

   private final LinkedHashSet<String> favorites = new LinkedHashSet<>();
   private final ArrayDeque<String> recentlyUsed = new ArrayDeque<>();

   public synchronized boolean isFavorite(String moduleName) {
      return moduleName != null && this.favorites.contains(moduleName);
   }

   public synchronized boolean addFavorite(String moduleName) {
      if (!isValidName(moduleName) || this.favorites.contains(moduleName)) {
         return false;
      }
      while (this.favorites.size() >= MAX_FAVORITES) {
         this.favorites.remove(this.favorites.iterator().next());
      }
      return this.favorites.add(moduleName);
   }

   public synchronized boolean removeFavorite(String moduleName) {
      return moduleName != null && this.favorites.remove(moduleName);
   }

   /** Returns the new favorite state. */
   public synchronized boolean toggleFavorite(String moduleName) {
      if (!isValidName(moduleName)) {
         return false;
      }
      if (this.favorites.remove(moduleName)) {
         return false;
      }
      while (this.favorites.size() >= MAX_FAVORITES) {
         this.favorites.remove(this.favorites.iterator().next());
      }
      this.favorites.add(moduleName);
      return true;
   }

   public synchronized List<String> getFavorites() {
      return List.copyOf(this.favorites);
   }

   public synchronized int getFavoriteIndex(String moduleName) {
      return indexOf(this.favorites, moduleName);
   }

   /** Most recently enabled module first. */
   public synchronized List<String> getRecentlyUsed() {
      return List.copyOf(this.recentlyUsed);
   }

   public synchronized int getRecentIndex(String moduleName) {
      return indexOf(this.recentlyUsed, moduleName);
   }

   public synchronized void recordRecentlyUsed(String moduleName) {
      if (!isValidName(moduleName)) {
         return;
      }
      this.recentlyUsed.remove(moduleName);
      this.recentlyUsed.addFirst(moduleName);
      while (this.recentlyUsed.size() > MAX_RECENT) {
         this.recentlyUsed.removeLast();
      }
   }

   public synchronized void replace(Collection<String> favorites, Collection<String> recent) {
      this.clear();
      if (favorites != null) {
         for (String name : favorites) {
            if (this.favorites.size() >= MAX_FAVORITES) {
               break;
            }
            if (isValidName(name)) {
               this.favorites.add(name);
            }
         }
      }
      if (recent != null) {
         for (String name : recent) {
            if (this.recentlyUsed.size() >= MAX_RECENT) {
               break;
            }
            if (isValidName(name)) {
               this.recentlyUsed.remove(name);
               this.recentlyUsed.addLast(name);
            }
         }
      }
   }

   public synchronized void clearFavorites() {
      this.favorites.clear();
   }

   public synchronized void clearRecentlyUsed() {
      this.recentlyUsed.clear();
   }

   public synchronized void clear() {
      this.favorites.clear();
      this.recentlyUsed.clear();
   }

   private static int indexOf(Iterable<String> names, String target) {
      if (target == null) {
         return -1;
      }
      int index = 0;
      for (String name : names) {
         if (name.equals(target)) {
            return index;
         }
         index++;
      }
      return -1;
   }

   private static boolean isValidName(String name) {
      return name != null && !name.isBlank() && name.length() <= 128
         && name.chars().noneMatch(Character::isISOControl);
   }
}
