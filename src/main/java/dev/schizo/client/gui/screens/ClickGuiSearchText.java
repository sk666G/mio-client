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
}
