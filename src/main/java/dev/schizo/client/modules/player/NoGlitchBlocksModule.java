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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MultiPlayerGameModeDestroyBlockEvent;
import dev.schizo.client.events.MultiPlayerGameModeUseItemOnEvent;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;

import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class NoGlitchBlocksModule extends Module {
   private final Setting<Boolean> placeSetting;
   public final Setting<Boolean> breakSetting;

   @EventHandler
   private void onMultiPlayerGameModeUseItemOnEvent(MultiPlayerGameModeUseItemOnEvent event) {
      InteractionHand interactionHand = event.getHand();
      Item heldItem = ClientAccess.schizoMinecraft.player.getItemInHand(interactionHand).getItem();
      if ((this.placeSetting.getValue()) && heldItem instanceof BlockItem blockItem) {
         BlockPos clickedPos = event.getHitResult().getBlockPos();
         Direction clickedFace = event.getHitResult().getDirection();
         BlockPos placementPos = clickedPos.relative(clickedFace);
         Block placedBlock = blockItem.getBlock();
         if (BlockInteractionUtils.canPlaceBlock(placementPos, placedBlock, false)) {
            InteractionHand packetHand = event.getHand();
            BlockHitResult packetHit = event.getHitResult();
            PacketSender.sendUseItemOn(packetHand, packetHit);
            LocalPlayer playerForSwing = ClientAccess.schizoMinecraft.player;
            InteractionHand swingHand = event.getHand();
            playerForSwing.swing(swingHand);
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onMultiPlayerGameModeDestroyBlockEvent(MultiPlayerGameModeDestroyBlockEvent event) {
      if (this.breakSetting.getValue()) {
         event.cancel();
      }
   }

   public NoGlitchBlocksModule() {
      super("NoGlitchBlocks", "Makes sure there are no ghost blocks as you interact with them. \n"
         + String.valueOf(ChatFormatting.RED) + "Not recommended on 2b2t/Grim servers.", ModuleCategory.PLAYER, new String[0]);
      Boolean defaultPlace = true;
      BooleanSetting placeOption = new BooleanSetting("Place", defaultPlace);
      this.placeSetting = this.add(placeOption);
      Boolean defaultBreak = true;
      BooleanSetting breakOption = new BooleanSetting("Break", defaultBreak);
      this.breakSetting = this.add(breakOption);
   }

}
