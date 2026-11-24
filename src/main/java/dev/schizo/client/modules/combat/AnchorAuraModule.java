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
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.utils.entity.PlayerStateTracker;
import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.combat.DamageUtils;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.render.FeatureBoxRenderer;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.DimensionType;
import dev.schizo.client.settings.options.BlockSampleMode;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AnchorAuraModule extends Module {
   private static AntiCheatModule antiCheat = (AntiCheatModule)ClientContext.getModule(AntiCheatModule.class);
   private static OffhandModule offhand = (OffhandModule)ClientContext.getModule(OffhandModule.class);
   private static AutoCrystalModule autoCrystal = (AutoCrystalModule)ClientContext.getModule(AutoCrystalModule.class);
   private static SpeedMineModule speedMine = (SpeedMineModule)ClientContext.getModule(SpeedMineModule.class);
   private final Setting<Integer> delaySetting;
   private final Setting<Float> rangeSetting;
   private final Setting<Float> wallRangeSetting;
   final Setting<Float> targetRangeSetting;
   final Setting<Float> minDamageSetting;
   private final Setting<Float> maxSelfDamageSetting;
   private final Setting<Boolean> antiSuicideSetting;
   private final Setting<Boolean> rotateSetting;
   private final Setting<Boolean> airPlaceSetting;
   private final Setting<Boolean> liquidPlaceSetting;
   private final Setting<Boolean> inhibitSetting;
   private final Setting<Boolean> sequentialSetting;
   private final Setting<Boolean> mineIgnoreSetting;
   final Setting<Boolean> renderSetting;
   private final Setting<Color> fillSetting;
   private final Setting<Color> outlineSetting;
   private final Setting<Boolean> fadeSetting;
   private final Setting<Float> fadeTimeSetting;
   private final Setting<Boolean> pauseSetting;
   private final Setting<Boolean> miningSetting;
   private final Setting<Boolean> eatingSetting;
   private final Setting<Float> healthSetting;
   private CompletableFuture<AnchorPlacementResult> calculationFuture;
   private FutureTask<Void> calculationTask;
   private ClientLevel calculationWorld;
   private final ElapsedTimer actionTimer;
   public final AtomicReference placementData;
   public AnchorTargetDamage targetDamage;

   private InteractionHand selectAnchorHand() {
      boolean offhandFlag = false;

      if (offhand.isToggled()) {
         offhandFlag = (offhand.itemSetting.getValue()).getItem(false) == Items.RESPAWN_ANCHOR;
      }

      Item anchorItemSnapshot = Items.RESPAWN_ANCHOR;

      InteractionHand heldAnchorHand = InventoryUtils.getHandForItem(anchorItemSnapshot);
      if (offhandFlag && heldAnchorHand == null) {
         return null;
      }
      return offhandFlag ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   @Override
   public String getInfo() {
      if (this.targetDamage != null) {
         Object[] arguments = new Object[2];
         Player target = this.targetDamage.target();
         Component targetName = target.getName();
         arguments[0] = targetName.getString();
         double damage = this.targetDamage.damage();
         arguments[1] = damage;
         return "%s, %.1f".formatted(arguments);
      } else {
         return null;
      }
   }

   @Override
   public void onEnable() {
      this.resetCalculationState();
   }

   @Override
   public void onToggle() {
      this.resetCalculationState();
   }

   private void completeCalculation() {
      FutureTask<Void> task = this.calculationTask;
      if (task != null && !task.isDone()) {
         task.cancel(true);
      }

      CompletableFuture<AnchorPlacementResult> pending = this.calculationFuture;
      if (pending != null && !pending.isDone() && !pending.isCancelled()) {
         pending.complete(null);
      }
   }

   private void resetCalculationState() {
      this.completeCalculation();
      this.calculationFuture = null;
      this.calculationTask = null;
      this.calculationWorld = null;
      this.placementData.set(null);
      this.targetDamage = null;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.resetCalculationState();
   }

   private boolean shouldPause() {
      if (this.isWorldUnavailable()) {
         return true;
      }

      if (ClientAccess.schizoMinecraft.player.isSleeping()) {
         return true;
      }

      if (ClientAccess.schizoMinecraft.player.isUsingItem()) {
         if (this.eatingSetting.getValue()) {
            return true;
         }
      }

      if (ClientAccess.schizoMinecraft.gameMode.isDestroying()) {
         if (this.miningSetting.getValue()) {
            return true;
         }
      }

      if (autoCrystal.isToggled()) {
         if (autoCrystal.placementData.get() != null && this.targetDamage != null) {
            double crystalDamage = ((CrystalPlacementResult)autoCrystal.placementData.get()).damage();
            AnchorTargetDamage anchorDamageSnapshot = this.targetDamage;

            if (crystalDamage > anchorDamageSnapshot.damage()) {
               return true;
            }
         }
      }

      float selfHealth = EntityUtils.getPlayerHealthWithAbsorption();

      Float minimumHealth = this.healthSetting.getValue();
      return selfHealth < minimumHealth;
   }

   boolean shouldIgnoreMiningBlock() {
      boolean miningThresholdReached = false;

      double miningDamage = speedMine.getMiningProgress();
      SpeedMineModule progressSnapshot = speedMine;

      double projectedDamage = miningDamage + progressSnapshot.getTickBreakProgress() * 2.0F;
      Setting<Float> thresholdSetting = speedMine.damageSetting;
      Float threshold = thresholdSetting.getValue();
      if (projectedDamage >= threshold.floatValue()) {
         if (speedMine.getMiningOrNearlyBrokenPos() != null) {
            BlockPos miningPositionSnapshot = speedMine.getMiningOrNearlyBrokenPos();

            if (WorldUtils.canMineBlock(miningPositionSnapshot)) {
               miningThresholdReached = true;
            }
         }
      }

      if (this.mineIgnoreSetting.getValue()) {
         if (miningThresholdReached) {
            return true;
         }

         if (!(speedMine.hasCrystalDelayElapsed(100L))) {
            return true;
         }
      }

      return false;
   }

   private void interactWithAnchor(
      BlockPos position, Direction direction, Vec3 hitPoint, InteractionHand hand
   ) {
      PlayerStateTracker interactionScheduler = ClientContext.playerStateTracker;
      Runnable interactionCallback = () -> {
         BlockHitResult hit = new BlockHitResult(hitPoint, direction, position, false);
         PacketSender.sendUseItemOn(hand, hit);
         SwingAction swingAction = SwingAction.INTERACT;

         ClientContext.swingManager.swingPacket(hand, swingAction);
      };

      interactionScheduler.runWithoutSneaking(interactionCallback);
   }

   private void chargeAnchor(BlockPos position, Direction direction, int originalSlot) {
      Vec3 centerSnapshot = position.getCenter();
      double faceOffset = MathConstants.ONE_HALF;
      Vec3 hitPoint = centerSnapshot.relative(direction, faceOffset);
      Item glowstoneSnapshot = Items.GLOWSTONE;

      InteractionHand glowstoneHand = InventoryUtils.getHandForItem(glowstoneSnapshot);

      if (this.rotateSetting.getValue()) {
         ClientContext.rotationManager.requestRotation(RotationManager.getRotationsToPosition(hitPoint), 100);
      }

      if (glowstoneHand != null) {
         InteractionHand interactionHand = InteractionHand.MAIN_HAND;
         this.interactWithAnchor(position, direction, hitPoint, interactionHand);
      } else {
         Item slotItemSnapshot = Items.GLOWSTONE;

         int glowstoneSlot = InventoryUtils.findHotbarSlot(slotItemSnapshot);
         if (glowstoneSlot != -1) {
            try {
               InventoryUtils.selectHotbarSlotForPlacement(glowstoneSlot);
               InteractionHand swappedHand = InteractionHand.MAIN_HAND;
               this.interactWithAnchor(position, direction, hitPoint, swappedHand);
            } finally {
               InventoryUtils.selectHotbarSlotForPlacement(originalSlot);
            }
         }
      }

      this.actionTimer.reset();
   }

   private boolean shouldRejectDamage(BlockPos position, AnchorTargetDamage targetDamage) {
      if (targetDamage == null) {
         return true;
      }

      BlockPos miningBlockSnapshot;
      if (this.shouldIgnoreMiningBlock()) {
         miningBlockSnapshot = speedMine.getMiningPos();
      } else {
         miningBlockSnapshot = null;
      }

      boolean existingAnchor = this.isAnchorBlock(position);
      Vec3 selfCenter = Vec3.atCenterOf(position);
      LocalPlayer selfPlayerSnapshot = ClientAccess.schizoMinecraft.player;

      AABB selfBoundsSnapshot = ClientAccess.schizoMinecraft.player.getBoundingBox();

      BlockPos selfIgnoredSnapshot = this.isAnchorBlock(position)
         ? position
         : miningBlockSnapshot;
      double selfDamage = DamageUtils.getExplosionDamage(
         selfCenter, selfPlayerSnapshot, selfBoundsSnapshot, 5.0D, true, selfIgnoredSnapshot, (BlockPos)null
      );

      double storedDamage = targetDamage.damage();

      Player lethalTargetPlayer = targetDamage.target();
      boolean lethalTarget = storedDamage >= EntityUtils.getHealthWithAbsorption(lethalTargetPlayer);
      Vec3 targetCenter = Vec3.atCenterOf(position);

      Player damageTargetSnapshot = targetDamage.target();

      Player boundsTarget = targetDamage.target();
      AABB targetBoundsSnapshot = boundsTarget.getBoundingBox();

      BlockPos targetIgnoredSnapshot = this.isAnchorBlock(position)
         ? position
         : miningBlockSnapshot;
      double recomputedTargetDamage = DamageUtils.getExplosionDamage(
         targetCenter, damageTargetSnapshot, targetBoundsSnapshot, 5.0D, true, targetIgnoredSnapshot, (BlockPos)null
      );
      boolean selfRejectionFlag = false;
      if (!lethalTarget) {
         Float maximumSelfDamage = this.maxSelfDamageSetting.getValue();
         selfRejectionFlag = selfDamage >= maximumSelfDamage.floatValue();
      }

      if (!selfRejectionFlag && selfDamage >= EntityUtils.getPlayerHealthWithAbsorption()) {
         selfRejectionFlag = this.antiSuicideSetting.getValue();
      }

      if (existingAnchor && !selfRejectionFlag) {
         return false;
      }

      return (this.minDamageSetting.getValue()).floatValue() > recomputedTargetDamage
         || recomputedTargetDamage < MathConstants.ONE_HALF || selfRejectionFlag;
   }

   public boolean isManagedAnchor(BlockPos position) {
      if (this.isToggled()) {
         if (!this.shouldPause()) {
            DimensionType dimension = ClientContext.worldStateManager.getDimension();
            if (!dimension.isNether()) {
               if (this.placementData.get() == null) {
                  return false;
               }

               return this.isAnchorBlock(position);
            }
         }
      }

      return false;
   }

   boolean isAnchorBlock(BlockPos position) {
      BlockState stateSnapshot = ClientAccess.schizoMinecraft.level.getBlockState(position);
      Block anchorBlock = Blocks.RESPAWN_ANCHOR;
      return stateSnapshot.is(anchorBlock);
   }

   private Direction selectSupportDirection(BlockPos position) {
      List<Direction> directions = BlockInteractionUtils.getStrictDirections(position);
      Direction direction = BlockInteractionUtils.isStrictDirectionEnabled() ? null : Direction.DOWN;
      if (!directions.isEmpty()) {
         direction = directions.get(0);
      }
      return direction;
   }

   private Vec3 selectHitPoint(BlockPos position) {
      BlockSampleMode samplingMode = BlockSampleMode.X8;
      List<Vec3> samples = samplingMode.sampleBlock(position);
      Optional<Vec3> selectedSample = samples.stream()
         .filter(sample -> {
            if (!WorldUtils.canSeeFromEyes(sample)) {
               double sampleDistance = ClientAccess.schizoMinecraft.player.getEyePosition().distanceTo(sample);
               Float wallRange = this.wallRangeSetting.getValue();
               return sampleDistance <= wallRange.floatValue();
            }
            return true;
         })
         .min(Comparator.comparing((Vec3 sample) -> {
            return ClientAccess.schizoMinecraft.player.getEyePosition().distanceTo(sample);
         }));
      Vec3 fallbackCenter = position.getCenter();
      return selectedSample.orElse(fallbackCenter);
   }

   private boolean isInvalidPlacement(BlockPos position) {
      BlockSampleMode samplingMode = BlockSampleMode.X8;
      List<Vec3> samples = samplingMode.sampleBlock(position);
      Float rangeSnapshot;
      if (!WorldUtils.canSeeAny(samples)) {
         rangeSnapshot = this.wallRangeSetting.getValue();
      } else {
         rangeSnapshot = this.rangeSetting.getValue();
      }

      double placementRange = rangeSnapshot.floatValue();

      AnchorTargetDamage targetDamage = AnchorTargetDamage.calculateForBase(this, position);
      if (this.shouldRejectDamage(position, targetDamage)) {
         return true;
      }

      Vec3 eyeSnapshot = ClientAccess.schizoMinecraft.player.getEyePosition();
      Vec3 center = position.getCenter();
      if (eyeSnapshot.distanceTo(center) > placementRange) {
         return true;
      }

      BlockState state = ClientAccess.schizoMinecraft.level.getBlockState(position);
      if (state.isAir()) {
         boolean hasSupport = false;
         if (this.selectSupportDirection(position) != null) {
            boolean directionModeSnapshot = BlockInteractionUtils.isStrictDirectionEnabled();

            boolean zeroWallRange = this.wallRangeSetting.getValue() <= 0.0F;
            hasSupport = BlockInteractionUtils.findSupportDirection(position, directionModeSnapshot, zeroWallRange) != null;
         }

         if (!hasSupport) {
            if (!this.airPlaceSetting.getValue()) {
               return true;
            }
         }
      }

      Block anchorBlock = Blocks.RESPAWN_ANCHOR;
      return !BlockInteractionUtils.canPlaceBlock(position, anchorBlock, true, false) && !this.isAnchorBlock(position);
   }

   private void placeAnchor(
      BlockPos position, Direction direction, InteractionHand hand
   ) {
      if (!(this.isAnchorBlock(position))) {
         int originalSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
         Item anchorItemSnapshot = Items.RESPAWN_ANCHOR;
         int anchorSlot = InventoryUtils.findHotbarSlot(anchorItemSnapshot);
         boolean offhandSwap = BlockInteractionUtils.findSupportDirection(position) == null
            && antiCheat.is2b2tEnabled() && this.airPlaceSetting.getValue();

         if (anchorSlot != -1 || hand != InteractionHand.MAIN_HAND) {
            if (this.rotateSetting.getValue()) {
               RotationManager rotationSnapshot = ClientContext.rotationManager;
               Vec3 center = position.getCenter();

               rotationSnapshot.requestRotation(RotationManager.getRotationsToFace(center, direction), 100);
            }

            ItemStack mainStackSnapshot = ClientAccess.schizoMinecraft.player.getMainHandItem();
            Item anchorBlockItem = Items.RESPAWN_ANCHOR;
            boolean selectedSwap = !mainStackSnapshot.is(anchorBlockItem);
            boolean offhandSwapApplied = false;
            try {
               if (selectedSwap) {
                  InventoryUtils.selectHotbarSlotForPlacement(anchorSlot);
               }

               if (offhandSwap) {
                  hand = PlayerStateUtils.oppositeHand(hand);
                  PacketSender.sendSwapOffhand();
                  offhandSwapApplied = true;
               }

               BlockPos relativeHitPosition = position.relative(direction);
               Vec3 hitPoint = this.selectHitPoint(relativeHitPosition);

               boolean airPlacement = this.airPlaceSetting.getValue();
               if (BlockInteractionUtils.sendPlacement(
                  position, hitPoint, direction, airPlacement, hand
               )) {
                  if (this.renderSetting.getValue()) {
                     ClientContext.featureBoxRenderer.addBox(this, position);
                  }
               }
            } finally {
               try {
                  if (offhandSwapApplied) {
                     PacketSender.sendSwapOffhand();
                  }
               } finally {
                  if (selectedSwap) {
                     InventoryUtils.selectHotbarSlotForPlacement(originalSlot);
                  }
               }
            }
         }
      }
   }

   private AnchorPlacementResult calculatePlacement() {
      LocalPlayer playerSnapshot = ClientAccess.schizoMinecraft.player;
      double originX = playerSnapshot.getX();
      double originY = playerSnapshot.getEyeY();
      double originZ = playerSnapshot.getZ();
      Vec3 eyeSnapshot = playerSnapshot.getEyePosition();
      MutableBlockPos scanPosition = new MutableBlockPos();
      AnchorPlacementResult bestPlacement = null;
      for (double offsetX = -Math.floor((this.rangeSetting.getValue()).floatValue());
         offsetX < Math.ceil((this.rangeSetting.getValue()).floatValue()); offsetX += 1.0D) {
         if (Thread.currentThread().isInterrupted()) {
            return null;
         }
         for (double offsetY = -Math.floor((this.rangeSetting.getValue()).floatValue());
            offsetY < Math.ceil((this.rangeSetting.getValue()).floatValue()); offsetY += 1.0D) {
            if (Thread.currentThread().isInterrupted()) {
               return null;
            }
            for (double offsetZ = -Math.floor((this.rangeSetting.getValue()).floatValue());
               offsetZ < Math.ceil((this.rangeSetting.getValue()).floatValue()); offsetZ += 1.0D) {
               if (Thread.currentThread().isInterrupted()) {
                  return null;
               }
               MutableBlockPos scanPositionSnapshot = scanPosition;

               double scanXSnapshot = originX + offsetX;

               double scanYSnapshot = originY + offsetY;

               double scanZ = originZ + offsetZ;
               scanPositionSnapshot.set(scanXSnapshot, scanYSnapshot, scanZ);
               scanPositionSnapshot = scanPosition;

               if (BlockInteractionUtils.isOutsidePlacementBounds(scanPositionSnapshot)) {
                  continue;
               }

               boolean miningBlockIgnored = false;
               if (this.shouldIgnoreMiningBlock()) {
                  scanPositionSnapshot = scanPosition;

                  if (scanPositionSnapshot.equals(speedMine.getMiningPos())) {
                     miningBlockIgnored = true;
                  }
               }

               if (WorldUtils.isPlacementOccupied(scanPosition) && !miningBlockIgnored) {
                  continue;
               }

               AABB boundsSnapshot = new AABB(scanPosition);
               if (BlockInteractionUtils.hasBlockingEntity(boundsSnapshot, true, true)) {
                  MutableBlockPos entityAnchorSnapshot = scanPosition;
                  if (!this.isAnchorBlock(entityAnchorSnapshot)) {
                     continue;
                  }
               }

               MutableBlockPos samplePosition = scanPosition;
               BlockSampleMode samplingMode = BlockSampleMode.X8;
               List samples = samplingMode.sampleBlock(samplePosition);
               boolean visible = WorldUtils.canSeeAny(samples);

               Vec3 center = scanPosition.getCenter();
               double distance = eyeSnapshot.distanceTo(center);
               if (!visible) {
                  Float visibilityWallRange = this.wallRangeSetting.getValue();
                  if (distance > visibilityWallRange.floatValue()) {
                     continue;
                  }
               }

               Float placementRange = this.rangeSetting.getValue();
               if (distance > placementRange.floatValue()) {
                  continue;
               }

               scanPositionSnapshot = scanPosition;

               boolean directionModeSnapshot = BlockInteractionUtils.isStrictDirectionEnabled();

               boolean zeroWallRange = this.wallRangeSetting.getValue() <= 0.0F;
               Direction direction = BlockInteractionUtils.findSupportDirection(
                  scanPositionSnapshot, directionModeSnapshot, zeroWallRange
               );
               MutableBlockPos damagePositionSnapshot = scanPosition;

               AnchorTargetDamage targetDamage = AnchorTargetDamage.calculateForBase(this, damagePositionSnapshot);
               if (direction == null && this.airPlaceSetting.getValue()) {
                  direction = Direction.DOWN;
               } else {
                  MutableBlockPos fallbackAnchorSnapshot = scanPosition;
                  if (this.isAnchorBlock(fallbackAnchorSnapshot)) {
                     direction = Direction.DOWN;
                  }
               }

               if (direction == null) {
                  MutableBlockPos fluidPosition = scanPosition;

                  FluidState fluid = ClientAccess.schizoMinecraft.level.getFluidState(fluidPosition);
                  if (!fluid.isEmpty()) {
                     if (this.liquidPlaceSetting.getValue()) {
                        direction = Direction.DOWN;
                     }
                  }
               }

               if (direction == null || targetDamage == null) {
                  continue;
               }
               MutableBlockPos damagePosition = scanPosition;
               if (this.shouldRejectDamage(damagePosition, targetDamage)) {
                  continue;
               }

               AnchorPlacementResult candidatePlacement = new AnchorPlacementResult(scanPosition.immutable(), direction, targetDamage);
               MutableBlockPos anchorSnapshot = scanPosition;

               if (this.isAnchorBlock(anchorSnapshot)) {
                  return candidatePlacement;
               }

               bestPlacement = candidatePlacement.selectHigherDamage(bestPlacement);
            }
         }
      }
      return bestPlacement;
   }

   private void updateCalculation() {
      ClientLevel activeLevel = ClientAccess.schizoMinecraft.level;
      if (this.calculationWorld != activeLevel) {
         // Cancel and discard any result that belongs to a replaced client world.
         this.completeCalculation();
         this.calculationFuture = null;
         this.calculationTask = null;
         this.calculationWorld = null;
         this.placementData.set(null);
         this.targetDamage = null;
      }

      if (this.placementData.get() != null) {
         AnchorPlacementResult existingPlacement = (AnchorPlacementResult)this.placementData.get();
         Player existingTarget = existingPlacement.targetDamage() == null
            ? null
            : existingPlacement.targetDamage().target();
         double validationRange = this.targetRangeSetting.getValue().doubleValue();
         if (this.isInvalidPlacement(existingPlacement.base())
            || existingTarget == null
            || existingTarget.level() != activeLevel
            || existingTarget.isRemoved()
            || AnchorTargetDamage.isInvalidTarget(existingTarget, validationRange)) {
            this.placementData.set(null);
            this.targetDamage = null;
         }
      }

      if (this.calculationFuture != null && this.calculationFuture.isDone() && this.placementData.get() == null) {
         try {
            AnchorPlacementResult completedPlacement = this.calculationFuture.get();
            AnchorTargetDamage completedDamage = completedPlacement == null
               ? null
               : completedPlacement.targetDamage();
            Player completedTarget = completedDamage == null ? null : completedDamage.target();
            double validationRange = this.targetRangeSetting.getValue().doubleValue();

            if (this.calculationWorld != activeLevel
               || completedPlacement == null
               || completedTarget == null
               || completedTarget.level() != activeLevel
               || completedTarget.isRemoved()
               || AnchorTargetDamage.isInvalidTarget(completedTarget, validationRange)
               || this.isInvalidPlacement(completedPlacement.base())) {
               this.placementData.set(null);
               this.targetDamage = null;
            } else {
               this.placementData.set(completedPlacement);
               this.targetDamage = completedDamage;
            }
         } catch (InterruptedException | ExecutionException failure) {
            failure.printStackTrace();
            this.placementData.set(null);
            this.targetDamage = null;
         }
      }

      if (this.calculationFuture != null && !this.calculationFuture.isDone() && !this.calculationFuture.isCancelled()) {
         return;
      }

      if (this.targetDamage != null) {
         Player currentTarget = this.targetDamage.target();

         double validationRange = (this.targetRangeSetting.getValue()).floatValue();
         if (currentTarget == null
            || currentTarget.level() != activeLevel
            || currentTarget.isRemoved()
            || AnchorTargetDamage.isInvalidTarget(currentTarget, validationRange)) {
            this.targetDamage = null;
            this.placementData.set(null);
         }
      }

      CompletableFuture<AnchorPlacementResult> scheduledCalculation = new CompletableFuture<>();
      FutureTask<Void> task = new FutureTask<>(() -> {
         try {
            scheduledCalculation.complete(this.calculatePlacement());
         } catch (Throwable failure) {
            scheduledCalculation.completeExceptionally(failure);
         }
         return null;
      });
      this.calculationWorld = activeLevel;
      this.calculationTask = task;
      Executor calculationExecutor = ClientAccess.executorService;
      calculationExecutor.execute(task);
      this.calculationFuture = scheduledCalculation
         .completeOnTimeout(null, 750L, TimeUnit.MILLISECONDS)
         .whenComplete((result, failure) -> {
            // Stop timed-out scans instead of letting them overlap later calculations.
            if (failure != null || result == null) {
               task.cancel(true);
            }
         })
         .exceptionally(failure -> null);
   }

   @EventHandler
   private void onPacketReceived(
      PacketEvent.Receive event
   ) {
      if (this.sequentialSetting.getValue()) {
         if (this.placementData.get() != null) {
            if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet
               && this.targetDamage != null) {
               BlockState state = packet.getBlockState();
               if (state.isAir()) {
                  BlockPos updatedPosition = packet.getPos();
                  AnchorPlacementResult placementSnapshot = (AnchorPlacementResult)this.placementData.get();

                  if (updatedPosition.equals(placementSnapshot.base())) {
                     placementSnapshot = (AnchorPlacementResult)this.placementData.get();

                     BlockPos baseSnapshot = placementSnapshot.base();

                     Direction directionSnapshot = ((AnchorPlacementResult)this.placementData.get()).direction();

                     InteractionHand placeHand = this.selectAnchorHand();
                     this.placeAnchor(baseSnapshot, directionSnapshot, placeHand);
                  }
               }
            }

            return;
         }
      }
   }

   @EventHandler
   private void onPlayerUpdate(RotationRequestEvent event) {
      DimensionType dimension = WorldUtils.getDimension();
      if (dimension.isNether()) {
         Component warningComponent = Component.nullToEmpty("Doesn't work in the nether!");

         MessageSignature warningSignature = ChatUtils.createSignature(-1);
         ChatUtils.MessagePriority warningPriority = ChatUtils.MessagePriority.HIGH;
         ChatUtils.printChat(warningComponent, warningSignature, warningPriority);
         this.disable();
      } else {
         this.updateCalculation();

         if (!this.shouldPause()) {
            AnchorPlacementResult placement = (AnchorPlacementResult)this.placementData.get();
            if (placement != null) {
               if (this.selectAnchorHand() != null) {
                  BlockPos position = placement.base();

                  BlockState state = ClientAccess.schizoMinecraft.level.getBlockState(position);

                  int originalSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
                  Block anchorBlock = Blocks.RESPAWN_ANCHOR;
                  if (state.is(anchorBlock)) {
                     Direction direction = this.selectSupportDirection(position);
                     if (direction == null) {
                        return;
                     }

                     ElapsedTimer timerSnapshot = this.actionTimer;

                     int configuredDelay = this.delaySetting.getValue();
                     ServerStateManager tickManagerSnapshot = ClientContext.serverStateManager;

                     long delayMillis = configuredDelay + tickManagerSnapshot.getLatencyMillis();
                     if (!timerSnapshot.hasElapsed(delayMillis)) {
                        return;
                     }

                     IntegerProperty chargeProperty = RespawnAnchorBlock.CHARGE;
                     Integer charges = state.getValue(chargeProperty);
                     if (charges == 0) {
                        this.chargeAnchor(position, direction, originalSlot);
                     }

                     if (this.rotateSetting.getValue()) {
                        RotationManager rotationSnapshot = ClientContext.rotationManager;
                        Vec3 center = position.getCenter();

                        rotationSnapshot.requestRotation(RotationManager.getRotationsToPosition(
                           center
                        ), 100);
                     }

                     Vec3 detonationCenter = Vec3.atCenterOf(position);
                     InteractionHand detonationHand = InteractionHand.MAIN_HAND;
                     this.interactWithAnchor(position, direction, detonationCenter, detonationHand);

                     if (this.inhibitSetting.getValue()) {
                        ClientLevel levelSnapshot = ClientAccess.schizoMinecraft.level;
                        Block airBlock = Blocks.AIR;
                        BlockState airState = airBlock.defaultBlockState();
                        levelSnapshot.setBlockAndUpdate(position, airState);
                     }
                  }

                  Direction directionSnapshot = placement.direction();

                  InteractionHand placementHand = this.selectAnchorHand();
                  this.placeAnchor(position, directionSnapshot, placementHand);
               }
            }
         }
      }
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      antiCheat = (AntiCheatModule)registry.getModule(
         AntiCheatModule.class
      );
      offhand = (OffhandModule)registry.getModule(
         OffhandModule.class
      );
      autoCrystal = (AutoCrystalModule)registry.getModule(
         AutoCrystalModule.class
      );
      speedMine = (SpeedMineModule)registry.getModule(
         SpeedMineModule.class
      );
   }

   private static String createDescription() {
      String warningColor = String.valueOf(ChatFormatting.RED);
      return "Explodes respawn anchors to kill players. \n" + warningColor + "Doesn't work in the nether.";
   }

   public AnchorAuraModule() {
      super("AnchorAura", createDescription(), ModuleCategory.COMBAT, new String[0]);
      this.delaySetting = this.add(new NumberSetting<>("Delay", 50, 0, 1000).withUnit("ms"));
      this.rangeSetting = this.add(new NumberSetting<>("Range", 5.0F, 0.0F, 6.0F).withUnit("m"));
      this.wallRangeSetting = this.add(new NumberSetting<>("WallRange", 5.0F, 0.0F, 6.0F).withUnit("m"));
      this.targetRangeSetting = this.add(new NumberSetting<>("TargetRange", 6.0F, 1.0F, 16.0F).withUnit("m"));
      this.minDamageSetting = this.add(new NumberSetting<>("MinDamage", 6.0F, 0.1F, 20.0F).withUnit("hp"));
      this.maxSelfDamageSetting = this.add(new NumberSetting<>("MaxSelfDamage", 12.0F, 0.0F, 20.0F).withUnit("hp"));
      this.antiSuicideSetting = this.add(new BooleanSetting("AntiSuicide", true));
      this.rotateSetting = this.add(new BooleanSetting("Rotate", false));
      this.airPlaceSetting = this.add(new BooleanSetting("AirPlace", false));
      this.liquidPlaceSetting = this.add(new BooleanSetting("LiquidPlace", false));
      this.inhibitSetting = this.add(new BooleanSetting("Inhibit", true));
      this.sequentialSetting = this.add(new BooleanSetting("Sequential", true));
      this.mineIgnoreSetting = this.add(new BooleanSetting("MineIgnore", false));
      this.renderSetting = this.add(new BooleanSetting("Render", true).markVisibilityDependency());
      this.fillSetting = this.add(new ColorSetting("Fill", new Color(1136499199, true), unused -> this.renderSetting.isExpanded()));
      this.outlineSetting = this.add(new ColorSetting("Outline", new Color(-910321153, true), unused -> this.renderSetting.isExpanded()));
      this.fadeSetting = this.add(new BooleanSetting("Fade", true).markVisibilityDependency().dependsOn(this.renderSetting));
      this.fadeTimeSetting = this.add(new NumberSetting<>("FadeTime", 1.0F, 0.1F, 1.0F).withUnit("s").dependsOn(this.renderSetting, this.fadeSetting));
      this.pauseSetting = this.add(new BooleanSetting("Pause", false).markAsGroup());
      this.miningSetting = this.add(new BooleanSetting("Mining", false).dependsOn(this.pauseSetting));
      this.eatingSetting = this.add(new BooleanSetting("Eating", false).dependsOn(this.pauseSetting));
      this.healthSetting = this.add(new NumberSetting<>("Health", 4.0F, 0.0F, 36.0F).withUnit("hp").dependsOn(this.pauseSetting));
      this.actionTimer = new ElapsedTimer();
      this.placementData = new AtomicReference();
      FeatureBoxRenderer renderer = ClientContext.featureBoxRenderer;
      FeatureBoxRenderer.RenderConfiguration renderConfiguration = new FeatureBoxRenderer.RenderConfiguration(
         this, this.fillSetting, this.outlineSetting,
         () -> {
            return 1.0F;
         },
         this.fadeTimeSetting, () -> false, this.fadeSetting,
         450
      );
      renderer.register(renderConfiguration);
   }
}
