/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import java.awt.Color;

/** Immutable colors used by a named Schizo UI palette. */
public record UiPalette(
   Color accent,
   Color background,
   Color button,
   Color enabled,
   Color tint,
   Color gradient
) {
   public UiPalette {
      if (accent == null || background == null || button == null
         || enabled == null || tint == null || gradient == null) {
         throw new IllegalArgumentException("Every UI palette color is required");
      }
   }
}
