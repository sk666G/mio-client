package dev.schizo.client.managers;

/** Pure validation and normalization for Schizo's single-character command prefix. */
final class CommandPrefix {
   private CommandPrefix() {
   }

   static String normalize(String requested) {
      if (requested == null || requested.isBlank()) {
         throw new IllegalArgumentException("Command prefix must not be blank");
      }
      int codePoint = requested.codePointAt(0);
      if (Character.isWhitespace(codePoint) || Character.isISOControl(codePoint)
         || Character.charCount(codePoint) != 1) {
         throw new IllegalArgumentException("Command prefix must be one printable BMP character");
      }
      return Character.toString((char)codePoint);
   }
}
