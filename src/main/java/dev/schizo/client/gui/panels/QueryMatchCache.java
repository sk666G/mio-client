package dev.schizo.client.gui.panels;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/** Current-query cache for stable module metadata; discarded on query change or screen init. */
final class QueryMatchCache<T> {
   private final Map<T, Boolean> matches = new IdentityHashMap<>();
   private String query;

   boolean matches(String next, T element, BiPredicate<T, String> matcher) {
      if (!next.equals(query)) {
         matches.clear();
         query = next;
      }
      Boolean result = matches.get(element);
      if (result == null) {
         result = matcher.test(element, next);
         matches.put(element, result);
      }
      return result;
   }

   void clearIfPresent() {
      if (query != null) clear();
   }

   void clear() {
      matches.clear();
      query = null;
   }
}
