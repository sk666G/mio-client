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
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.screens.TradeManagerScreen;
import dev.schizo.client.gui.screens.TradeEditorScreen;
import dev.schizo.client.gui.components.ActionButtonComponent;
import dev.schizo.client.utils.inventory.TradeDefinition;
import dev.schizo.client.gui.components.ButtonComponent;
import dev.schizo.client.render.TradePanelRequest;
import dev.schizo.client.utils.inventory.EnchantmentUtils;
import dev.schizo.client.utils.inventory.TradeEnchantmentEntry;

public class TradeActionsPanel extends Panel implements ClientAccess {
   private long deleteArmedAt;
   private boolean deleteArmed;
   private String notice="";
   private final TradeManagerScreen managerScreen;
   private final TradeDefinition trade;

   public TradeActionsPanel(TradeManagerScreen screen, TradeDefinition selectedTrade) {
      super("Trade");
      this.setX(
         this.getX()
            + this.getWidth()
            + 4
      );
      this.managerScreen = screen;
      this.trade = selectedTrade;
      this.registerChild(
         new ButtonComponent(
            this,
            "Trade: " + selectedTrade.getName()
         )
      );
      this.registerChild(
         new ActionButtonComponent(this, "Edit", this::openEditor)
      );
      this.registerChild(
         new ActionButtonComponent(
            this,
            this::getDeleteButtonLabel,
            this::deleteIfConfirmed
         )
      );
      this.registerChild(new ActionButtonComponent(this,"Cancel",this::showCreatePanel));
      this.registerChild(new ButtonComponent(this,()->StudioControlStyle.fitText(notice,Math.max(0,getWidth()-8))));
   }

   private TradeDefinition currentTrade() {
      return (TradeDefinition)ClientContext.tradeRegistry.findFirst(
         (java.util.function.Predicate<TradeDefinition>)candidate->candidate.getName().equals(trade.getName())
      ).orElse(null);
   }

   private void openEditor() {
      deleteArmed=false;
      TradeDefinition current=currentTrade();
      if(current!=trade) {notice="Trade changed; reopen selection.";return;}
      if(ClientAccess.schizoMinecraft.level==null) {notice="Open a world first.";return;}
      for(Object entry:current.enchants()) {
         var enchantment=(TradeEnchantmentEntry)entry;
         if(EnchantmentUtils.getHolder(enchantment.getName())==null) {
            notice="Missing enchantment; edit unavailable.";
            return;
         }
      }
      TradeEditorScreen editorScreen=new TradeEditorScreen(current);
      editorScreen.setParentScreen(managerScreen);
      ClientAccess.schizoMinecraft.setScreen(editorScreen);
   }

   private void deleteIfConfirmed() {
      if(currentTrade()!=trade) {
         deleteArmed=false;
         notice="Trade changed; reopen selection.";
         return;
      }
      long now=System.nanoTime();
      if(deleteArmed && now-deleteArmedAt<1_000_000_000L) {
         deleteArmed=false;
         ClientContext.tradeRegistry.unregisterByName(trade.name());
         showCreatePanel();
      } else {
         deleteArmed=true;
         deleteArmedAt=now;
      }
   }

   @Override public void cancelInput() {
      deleteArmed=false;
      super.cancelInput();
   }

   private void showCreatePanel() {
      this.managerScreen
         .requestPanelChange(
            TradePanelRequest.forCreate()
         );
   }

   private String getDeleteButtonLabel() {
      return deleteArmed && System.nanoTime()-deleteArmedAt<1_000_000_000L?"Confirm delete":"Delete";
   }
}
