/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import dev.schizo.client.ClientAccess;

public abstract class ChestStealerController implements ClientAccess {
   protected final ChestStealerModule module;

   public ChestStealerController(ChestStealerModule module) {
      this.module = module;
   }

}
