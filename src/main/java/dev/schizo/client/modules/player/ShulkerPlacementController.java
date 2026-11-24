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
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.MultiPlayerGameModeUseItemOnEvent;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.world.BlockInteractionUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ShulkerPlacementController extends ChestStealerController {
   public ShulkerPlacementController(ChestStealerModule module) {
      super(module);
   }

   public void onUseItemOn(MultiPlayerGameModeUseItemOnEvent event) {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      InteractionHand hand = event.getHand();
      ItemStack heldStack = player.getItemInHand(hand);
      TagKey shulkerTag = ItemTags.SHULKER_BOXES;
      if (heldStack.is(shulkerTag)) {
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         BlockPos clickedPosition = event.getHitResult().getBlockPos();
         Block clickedBlock = level.getBlockState(clickedPosition).getBlock();
         if (BlockInteractionUtils.isInteractiveBlock(clickedBlock)) {
            LocalPlayer sneakingPlayer = ClientAccess.schizoMinecraft.player;
            if (!sneakingPlayer.isShiftKeyDown()) {
               return;
            }
         }

         TaskScheduler scheduler = ClientContext.taskScheduler;
         Runnable openPlacedShulker = () -> {
            BlockHitResult placedHit = event.getHitResult();
            BlockPos clickedBlockPosition = placedHit.getBlockPos();
            Direction placementDirection = placedHit.getDirection();
            BlockPos shulkerPosition = clickedBlockPosition.relative(placementDirection);
            Direction fallbackDirection = Direction.UP;
            Direction openDirection = (Direction)BlockInteractionUtils.getStrictDirectionsOrFallback(shulkerPosition, fallbackDirection).getFirst();

            Vec3 hitPosition = shulkerPosition.getCenter().relative(openDirection, 0.5D);
            BlockHitResult openHit = new BlockHitResult(hitPosition, openDirection, shulkerPosition, false);
            InteractionHand openHand = event.getHand();
            PacketSender.sendUseItemOn(openHand, openHit);
            RotationManager rotationManager = ClientContext.rotationManager;

            float[] rotations = RotationManager.getRotationsToBlock(shulkerPosition);
            rotationManager.requestRotation(rotations, 1000);
         };
         scheduler.scheduleRotationTask(openPlacedShulker, 2);
      }
   }
}
