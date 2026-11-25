/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import java.util.Locale;

/** Pure search ranking shared by the command palette and its tests. */
final class PaletteMatcher {
   private PaletteMatcher() {
   }

   static String normalizeQuery(String requestedQuery) {
      return requestedQuery == null
         ? ""
         : requestedQuery.strip().toLowerCase(Locale.ROOT);
   }

   static int score(String title, String subtitle, String requestedQuery) {
      return scoreNormalized(title, subtitle, normalizeQuery(requestedQuery));
   }

   static int scoreNormalized(String title, String subtitle, String query) {
      if (query.isEmpty()) {
         return 0;
      }

      String normalizedTitle = title.toLowerCase(Locale.ROOT);
      String normalizedSubtitle = subtitle.toLowerCase(Locale.ROOT);
      if (normalizedTitle.equals(query)) {
         return 0;
      }
      if (normalizedTitle.startsWith(query)) {
         return 1;
      }
      if (normalizedTitle.contains(query)) {
         return 2;
      }
      if (normalizedSubtitle.contains(query)) {
         return 3;
      }
      return isSubsequence(query, normalizedTitle) ? 4 : -1;
   }

   private static boolean isSubsequence(String query, String candidate) {
      int queryIndex = 0;
      for (int index = 0; index < candidate.length() && queryIndex < query.length(); index++) {
         if (candidate.charAt(index) == query.charAt(queryIndex)) {
            queryIndex++;
         }
      }
      return queryIndex == query.length();
   }
}
