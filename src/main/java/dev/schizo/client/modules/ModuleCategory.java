/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules;

import dev.schizo.client.settings.NamedEntry;

public enum ModuleCategory implements NamedEntry {
   COMBAT("Combat"),
   MISC("Misc"),
   RENDER("Render"),
   MOVEMENT("Movement"),
   PLAYER("Player"),
   EXPLOIT("Exploit"),
   CLIENT("Client"),
   HUD("HUD");

   private final String displayName;

   @Override
   public String getName() {
      return this.displayName;
   }

   ModuleCategory(String displayName) {
      this.displayName = displayName;
   }

}
