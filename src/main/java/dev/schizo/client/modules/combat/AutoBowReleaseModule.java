/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoBowReleaseModule extends Module {
   private final Setting<Integer> delaySetting;
   private final Setting<Boolean> crossbowSetting;
   private boolean releasedDuringUse;

   public AutoBowReleaseModule() {
      super("AutoBowRelease", "Shoots your bow automatically.", ModuleCategory.COMBAT, new String[]{"bowspam"});
      this.delaySetting = this.add(new NumberSetting<>("Delay", 5, 2, 20));
      this.crossbowSetting = this.add(new BooleanSetting("Crossbow", false));
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerPreTickEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }

      ArrowsModule arrows = (ArrowsModule)ClientContext.getModule(ArrowsModule.class);

      if (arrows != null && arrows.isAutoShooting()) {
         return;
      }
      if (!this.hasArrowAccess()
         || ClientAccess.schizoMinecraft.screen != null) {
         return;
      }

      if (this.crossbowSetting.getValue() && this.fireFirstChargedCrossbow()) {
         return;
      }

      this.releaseBowIfReady();
   }

   private void releaseBowIfReady() {
      if (!ClientAccess.schizoMinecraft.player.isUsingItem()) {
         KeyMapping useKey = ClientAccess.schizoMinecraft.options.keyUse;
         if (!useKey.isDown()) {
            this.releasedDuringUse = false;
         }
      }

      if (!this.hasArrowAccess()) {
         return;
      }

      if (!this.isBowReadyToRelease()) {
         return;
      }

      ClientAccess.schizoMinecraft.gameMode.releaseUsingItem(ClientAccess.schizoMinecraft.player);
      KeyMapping useKey = ClientAccess.schizoMinecraft.options.keyUse;
      if (useKey.isDown()) {
         InteractionHand usedHand = ClientAccess.schizoMinecraft.player.getUsedItemHand();
         PacketSender.sendUseItem(usedHand);
         InteractionHand swingHand = InteractionHand.MAIN_HAND;
         PacketSender.sendSwing(swingHand);
      }
      this.releasedDuringUse = true;
   }

   private boolean fireFirstChargedCrossbow() {
      for (InteractionHand hand : InteractionHand.values()) {
         ItemStack stack = ClientAccess.schizoMinecraft.player.getItemInHand(hand);
         Item crossbowItem = Items.CROSSBOW;

         if (stack.is(crossbowItem) && CrossbowItem.isCharged(stack)) {
            PacketSender.sendUseItem(hand);
            InteractionHand swingHand = InteractionHand.MAIN_HAND;
            PacketSender.sendSwing(swingHand);
            return true;
         }
      }

      return false;
   }

   private boolean hasArrowAccess() {
      if (ClientAccess.schizoMinecraft.player.isCreative()) {
         return true;
      }

      Predicate<ItemStack> arrowPredicate = candidate -> {
         return candidate.getItem() instanceof ArrowItem;
      };

      return InventoryUtils.countItems(arrowPredicate) > 0;
   }

   private boolean isBowReadyToRelease() {
      int releaseTicks = this.getReleaseTicks();

      if (!ClientAccess.schizoMinecraft.player.isUsingItem()) {
         return false;
      }

      int useTicks = ClientAccess.schizoMinecraft.player.getTicksUsingItem();
      if (useTicks < releaseTicks) {
         return false;
      }

      ItemStack useStack = ClientAccess.schizoMinecraft.player.getUseItem();
      Item bowItem = Items.BOW;

      return useStack.is(bowItem);
   }

   public int getReleaseTicks() {
      return this.delaySetting.getValue() + (this.releasedDuringUse ? 0 : 1);
   }

   public int getInitialReleaseTicks() {
      return this.delaySetting.getValue() + 1;
   }
}
