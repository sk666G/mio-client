/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;

public class NoNarratorModule extends Module {
   public NoNarratorModule() {
      super("NoNarrator", "Disables the narrator feature.", ModuleCategory.MISC, new String[0]);
      this.setDrawn(false);
   }
}
