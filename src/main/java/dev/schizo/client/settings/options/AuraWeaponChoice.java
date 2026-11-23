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

public enum AuraWeaponChoice implements NamedEntry {
   NONE("None"),
   REQUIRE("Require"),
   SWAP("Swap");

   private final String displayName;

   @Override
   public String getName() {
      return this.displayName;
   }

   AuraWeaponChoice(String displayName) {
      this.displayName = displayName;
   }
}
