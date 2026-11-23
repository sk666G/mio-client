/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.components.TradeActionsPanel;
import dev.schizo.client.gui.components.TradeListPanel;
import dev.schizo.client.gui.components.CreateTradePanel;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.render.TradePanelMode;
import dev.schizo.client.render.TradePanelRequest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public final class TradeManagerScreen extends ClientScreen {
   private final TradeListPanel tradeListPanel = new TradeListPanel(this);
   private Panel actionPanel = new CreateTradePanel(this);
   private TradePanelRequest pendingPanelRequest;

   public TradeManagerScreen() {
      this.tradeListPanel
         .updateLayout();
      this.getPanels()
         .add(this.tradeListPanel);
      this.getPanels()
         .add(this.actionPanel);
   }

   @Override
   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
      if (this.pendingPanelRequest != null) {
         this.tradeListPanel
            .refreshTrades();
         this.getPanels()
            .remove(this.actionPanel);
         if (this.pendingPanelRequest
               .mode()
            == TradePanelMode.CREATE) {
            this.actionPanel = new CreateTradePanel(this);
         } else {
            this.actionPanel = new TradeActionsPanel(
               this,
               this.pendingPanelRequest
                  .trade()
            );
         }

         this.getPanels()
            .add(this.actionPanel);
         this.pendingPanelRequest = null;
      }
   }

   public void requestPanelChange(TradePanelRequest request) {
      this.pendingPanelRequest = request;
   }

}
