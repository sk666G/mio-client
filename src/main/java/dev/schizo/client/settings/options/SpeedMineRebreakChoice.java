/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.settings.NamedEntry;

public enum SpeedMineRebreakChoice implements NamedEntry {
   NONE("None"),
   FAST("Fast"),
   INSTANT("Instant");

   private final String name;

   public boolean isNone() {
      return this == NONE;
   }

   @Override
   public String getName() {
      return this.name;
   }

   SpeedMineRebreakChoice(String name) {
      this.name = name;
   }

   public boolean isInstant() {
      return this == INSTANT;
   }

   public boolean isFast() {
      return this == FAST;
   }
}
