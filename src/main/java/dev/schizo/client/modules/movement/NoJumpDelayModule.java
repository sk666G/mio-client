/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;

public class NoJumpDelayModule extends Module {
   public NoJumpDelayModule() {
      super("NoJumpDelay", "Removes the jumping delay when sprint-jumping.", ModuleCategory.MOVEMENT, new String[0]);
      this.setDrawn(false);
   }
}
