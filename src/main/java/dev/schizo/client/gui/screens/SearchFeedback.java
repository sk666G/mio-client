package dev.schizo.client.gui.screens;

import java.util.function.ToDoubleFunction;

/** Pure presentation helpers; match ranges use original UTF-16 offsets, never normalized offsets. */
final class SearchFeedback {
   private SearchFeedback() {}
   record Span(int start, int end) {}

   static Span literalSpan(String title, String query) {
      if (query.isEmpty() || query.length() > title.length()) return null;
      for (int i = 0; i <= title.length() - query.length(); i++) {
         if (title.regionMatches(true, i, query, 0, query.length()))
            return new Span(i, i + query.length());
      }
      return null;
   }

   static String hint(int score, String query) {
      if (query.isEmpty()) return "";
      return switch (score) {
         case 3 -> "Context match";
         case 4 -> "Flexible match";
         case 5 -> "Typo match";
         default -> "";
      };
   }

   static String fit(String text, float width, ToDoubleFunction<String> measure) {
      if (!Float.isFinite(width) || width <= 0) return "";
      if (measure.applyAsDouble(text) <= width) return text;
      if (measure.applyAsDouble("...") > width) return "";
      int end = text.length();
      while (end > 0) {
         end = text.offsetByCodePoints(end, -1);
         String candidate = text.substring(0, end) + "...";
         if (measure.applyAsDouble(candidate) <= width) return candidate;
      }
      return "...";
   }
}
