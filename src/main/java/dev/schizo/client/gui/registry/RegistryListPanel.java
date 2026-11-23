/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.registry;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.gui.panels.Panel;

class RegistryListPanel extends Panel {

   public RegistryListPanel(String title) {
      super(title);
   }

   @Override
   protected int getHeightLimit() {
      return ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight() / 2 + 10;
   }

}
