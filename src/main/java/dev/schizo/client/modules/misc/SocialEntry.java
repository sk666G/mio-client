/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.options.SocialRole;

public record SocialEntry(
   String name,
   SocialRole role
) implements NamedEntry {

   @Override
   public String getName() {
      return this.name;
   }
}
