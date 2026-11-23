/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.panels.ModuleCategoryPanel;
import dev.schizo.client.gui.GuiElement;
import dev.schizo.client.modules.ModuleCategory;

public class HudModulePanel extends ModuleCategoryPanel {

   @Override
   protected int getHeightLimit() {
      int height = 0;

      for (Object childElement : this.children) {
         GuiElement child = (GuiElement)childElement;
         height += child.getBaseHeight() + 1;
      }

      return height;
   }

   public HudModulePanel() {
      super(ModuleCategory.HUD);
   }

}
