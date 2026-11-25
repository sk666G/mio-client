/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

/** Pure validation/normalization for the single-character Schizo command prefix. */
public final class CommandPrefix {
   private CommandPrefix() {
   }

   public static String normalize(String requested, String fallback) {
      if (requested == null || requested.isBlank()) {
         return fallback;
      }
      int codePoint = requested.codePointAt(0);
      if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint)) {
         return fallback;
      }
      return new String(Character.toChars(codePoint));
   }
}
