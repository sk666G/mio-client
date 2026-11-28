package dev.schizo.client.gui.screens;

/** Directional caret/anchor snapshot. Never transfer selection onto changed text. */
record StudioTextSelection(String text, int caret, int anchor) {
   StudioTextSelection {
      caret = bound(text, caret);
      anchor = bound(text, anchor);
   }

   boolean matches(String value) { return text.equals(value); }

   private static int bound(String text, int index) {
      int bounded = Math.max(0, Math.min(text.length(), index));
      // Avoid splitting a supplementary Unicode character.
      if (bounded > 0 && bounded < text.length()
         && Character.isHighSurrogate(text.charAt(bounded - 1))
         && Character.isLowSurrogate(text.charAt(bounded))) return bounded - 1;
      return bounded;
   }
}
