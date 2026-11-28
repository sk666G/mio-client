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
public final class PaletteMatcher {
   private PaletteMatcher() {
   }

   static String normalizeQuery(String requestedQuery) {
      return requestedQuery == null
         ? ""
         : requestedQuery.strip().toLowerCase(Locale.ROOT);
   }

   public static int score(String title, String subtitle, String requestedQuery) {
      return scoreNormalized(title, subtitle, normalizeQuery(requestedQuery));
   }

   /** Module filtering keeps short queries literal and allows longer fuzzy name matches. */
   public static boolean matchesModule(String title, String metadata, String requestedQuery) {
      String query = normalizeQuery(requestedQuery);
      int score = scoreNormalized(title, metadata, query);
      return score >= 0 && (score < 4 || query.length() >= 4);
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
      if (query.length() > 64) return -1;
      if (query.indexOf(' ') >= 0 && allTermsMatch(query, normalizedTitle + " " + normalizedSubtitle)) return 4;
      if (isSubsequence(query, normalizedTitle)) return 4;
      if (query.length() >= 4 && query.indexOf(' ') < 0 && typoMatches(query, normalizedTitle)) return 5;
      return -1;
   }

   private static boolean allTermsMatch(String query, String combined) {
      for (String term : query.split("\\s+")) {
         if (!term.isEmpty() && !combined.contains(term)) return false;
      }
      return true;
   }

   private static boolean typoMatches(String query, String title) {
      if (oneEditApart(query, title)) return true;
      for (String word : title.split("[^\\p{L}\\p{N}]+")) {
         if (oneEditApart(query, word)) return true;
      }
      return false;
   }

   /** One insertion, deletion, substitution or adjacent transposition. No DP allocation. */
   private static boolean oneEditApart(String a, String b) {
      int n = a.length(), m = b.length();
      if (Math.abs(n - m) > 1) return false;
      if (n == m) {
         int first = 0;
         while (first < n && a.charAt(first) == b.charAt(first)) first++;
         if (first == n) return true;
         int next = first + 1;
         if (next < n && a.charAt(first) == b.charAt(next) && a.charAt(next) == b.charAt(first)
            && a.regionMatches(next + 1, b, next + 1, n - next - 1)) return true;
         return a.regionMatches(next, b, next, n - next);
      }
      String shorter = n < m ? a : b, longer = n < m ? b : a;
      int index = 0;
      while (index < shorter.length() && shorter.charAt(index) == longer.charAt(index)) index++;
      return shorter.regionMatches(index, longer, index + 1, shorter.length() - index);
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
