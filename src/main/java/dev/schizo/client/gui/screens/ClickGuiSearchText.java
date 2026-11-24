package dev.schizo.client.gui.screens;

/** Prefix completion for the ClickGUI search field, independent of caret position. */
final class ClickGuiSearchText {
   private ClickGuiSearchText() {
   }

   /**
    * Returns the remaining suffix for a matching normalized query, or null
    * when no nonempty prefix matches. An exact match has an empty suffix.
    */
   static String completionSuffix(String name, String normalizedQuery) {
      if (normalizedQuery.isEmpty() || normalizedQuery.length() > name.length()
         || !name.regionMatches(true, 0, normalizedQuery, 0, normalizedQuery.length())) {
         return null;
      }
      return name.substring(normalizedQuery.length());
   }

   /** Normalize native selection endpoints before taking substrings. */
   static Selection selection(int textLength, int caret, int anchor) {
      int length = Math.max(0, textLength);
      int boundedCaret = Math.max(0, Math.min(length, caret));
      int boundedAnchor = Math.max(0, Math.min(length, anchor));
      return new Selection(Math.min(boundedCaret, boundedAnchor), Math.max(boundedCaret, boundedAnchor), boundedCaret);
   }

   record Selection(int start, int end, int caret) { }
}
