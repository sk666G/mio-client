/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.gui.CloseState;
import dev.schizo.client.gui.components.StringSettingComponent;
import dev.schizo.client.settings.StringSetting;

public abstract class TradeSelectorPanel<T> extends Panel implements ClientAccess {
   protected final TradeItemsComponent tradeItems;
   protected final StringSettingComponent searchInput;
   private final boolean fromSide;

   public TradeSelectorPanel(TradeItemsComponent tradeItems, boolean fromSide) {
      super(fromSide ? "From" : "To");
      StringSetting searchSetting = new StringSetting("Search", "");
      this.fromSide = fromSide;
      this.tradeItems = tradeItems;
      this.searchInput = new StringSettingComponent(
         this, CloseState.ALWAYS_OPEN, searchSetting
      );
      this.children
         .add(this.searchInput);
      this.disableHeightLimit();
   }

   protected abstract void applySelection(T selection);

   protected abstract T getSelection();

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      super.mouseClicked(mouseX, mouseY, button);
      T selection = this.getSelection();
      if (selection != null) {
         try {
            this.applySelection(selection);
         } finally {
            this.tradeItems.closeSelector();
         }
      }
   }

   public boolean isFromSide() {
      return this.fromSide;
   }

}
