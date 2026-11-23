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

public enum AntiCheatPlacementsChoice implements NamedEntry {
   VANILLA("Vanilla"),
   PAPER("Paper");

   private final String displayName;

   @Override
   public String getName() {
      return this.displayName;
   }

   AntiCheatPlacementsChoice(String displayName) {
      this.displayName = displayName;
   }
}
