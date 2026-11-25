/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import java.util.Set;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MultiPlayerGameModeStartDestroyBlockEvent;
import dev.schizo.client.render.WorldBoxRenderer;
import dev.schizo.client.managers.SwingInteractionManager;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.combat.ExtraBreakTarget;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.utils.world.NukerBlockQueue;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.RegistrySetSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.NukerShapeChoice;
import dev.schizo.client.settings.options.NukerSortChoice;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NukerModule extends Module {



   final Setting<Set<Block>> blocksSetting;
   final Setting<NukerModeChoice> modeSetting;
   final Setting<NukerShapeChoice> shapeSetting;
   private final Setting<NukerSortChoice> sortSetting;
   private final Setting<Integer> bPTSetting;
   private final Setting<Integer> delaySetting;
   public final Setting<Float> rangeSetting;
   private final Setting<Float> wallRangeSetting;
   public final Setting<Integer> heightSetting;
   private final Setting<Boolean> strictDirectionSetting;
   private final Setting<Boolean> rotateSetting;
   private final Setting<Boolean> creativeSetting;
   private final Setting<Boolean> flattenSetting;
   private final Setting<Boolean> baritoneAreaSetting;
   final Setting<Boolean> playerProximitySetting;
   private final Setting<Color> colorSetting;
   private final NukerBlockQueue blockQueue;
   private final List<RecentBreak> recentBreaks;
   private final ElapsedTimer delayTimer;
   private BlockPos lastMinedPos;
   private int blocksMinedThisTick;
   private Block clickedBlock;
   private boolean internalMining;
   private boolean instantBreak;

   public NukerBlockQueue getBlockQueue() {
      return this.blockQueue;
   }

   private boolean shouldUseInstantBlockFilter() {
      SpeedMineModule speedMineForToggle = schizo$speedMineModule();
      if (!speedMineForToggle.isToggled()) {
         return false;
      }

      SpeedMineModule speedMineSnapshot = schizo$speedMineModule();
      BlockPos primaryTarget = speedMineSnapshot.getMiningPos();
      if (primaryTarget == null) {
         speedMineSnapshot = schizo$speedMineModule();
         primaryTarget = speedMineSnapshot.getRebreakPos();
      }

      BlockPos extraTarget = null;
      speedMineSnapshot = schizo$speedMineModule();
      if (speedMineSnapshot.getExtraBreakTarget() != null) {
         speedMineSnapshot = schizo$speedMineModule();
         extraTarget = speedMineSnapshot.getExtraBreakTarget().getPosition();
      }

      if (!WorldUtils.canMineBlock(primaryTarget) && !WorldUtils.canMineBlock(extraTarget)) {
         return false;
      }

      return primaryTarget == null || !primaryTarget.equals(extraTarget);
   }

   private int getMiningBatchLimit() {
      if (schizo$speedMineModule().isToggled()) {
         Boolean extraBreakEnabled = schizo$speedMineModule().extraBreakSetting.getValue();
         int batchLimit;
         if (extraBreakEnabled) {
            batchLimit = 2;
         } else {
            batchLimit = 1;
         }

         return batchLimit;
      } else {
         return this.bPTSetting.getValue();
      }
   }

   private boolean isZeroHardnessBlock(BlockPos blockPos) {
      BlockState blockState = ClientAccess.schizoMinecraft.level.getBlockState(blockPos);
      ClientLevel blockLevel = ClientAccess.schizoMinecraft.level;
      return blockState.getDestroySpeed(blockLevel, blockPos) == 0.0F;
   }

   public Block getClickedBlock() {
      return this.clickedBlock;
   }

   @Override
   public String getInfo() {
      Setting<NukerModeChoice> modeSettingRef = this.modeSetting;
      return FontRenderer.capitalize(modeSettingRef.getValue());
   }

   @Override
   public void onDisable() {
      ClientAccess.schizoMinecraft.options.keyAttack.setDown(false);
      this.clickedBlock = null;
   }

   @EventHandler
   private void onTick(RotationRequestEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      this.recentBreaks.removeIf(recentBreak -> System.currentTimeMillis() > recentBreak.expiresAtMillis());
      this.instantBreak = false;
      this.lastMinedPos = null;
      this.blocksMinedThisTick = 0;
      ElapsedTimer delayTimerWork = this.delayTimer;
      long delayMillis = (this.delaySetting.getValue()).intValue();
      if (!delayTimerWork.hasElapsed(delayMillis)) {
         return;
      }
      if (schizo$speedMineModule().isToggled()) {
         SpeedMineModule speedMineForDelaySnapshot = schizo$speedMineModule();
         long speedMineDelayCapture = (this.delaySetting.getValue()).intValue();
         if (!speedMineForDelaySnapshot.hasCrystalDelayElapsed(speedMineDelayCapture)) {
            return;
         }
      }

      List<BlockPos> candidates = (this.shapeSetting.getValue()).collectPositions(this);

      this.blockQueue.pruneInvalidPositions();
      if (this.shapeSetting.getValue() != NukerShapeChoice.QUEUE) {
         candidates.sort(Comparator.comparingDouble(sortPos -> {
            return (this.sortSetting.getValue()).getSortKey(sortPos);
         }));
      }

      boolean restrictToInstantBlocks = this.shouldUseInstantBlockFilter();
      for (BlockPos candidatePos : candidates) {
         if (this.flattenSetting.getValue()) {
            double candidateY = candidatePos.getY();
            LocalPlayer playerForHeight = ClientAccess.schizoMinecraft.player;
            if (candidateY < playerForHeight.getY()) {
               continue;
            }
         }

         if (restrictToInstantBlocks) {
            if (!this.isZeroHardnessBlock(candidatePos)) {
               continue;
            }
         }

         if (!this.canMineBlock(candidatePos)) {
            continue;
         }
         if (schizo$antiCheatModule().isPacketLimitReached()) {
            break;
         }
         this.internalMining = true;

         this.mineBlock(candidatePos);
         this.internalMining = false;
         delayTimerWork = this.delayTimer;

         delayTimerWork.reset();

         if (this.blocksMinedThisTick >= this.getMiningBatchLimit()) {
            return;
         }
         if (this.blocksMinedThisTick > 0 && !this.instantBreak) {
            Setting<Boolean> creativeSetting = this.creativeSetting;
            Boolean creativeEnabled = creativeSetting.getValue();
            if (!creativeEnabled) {
               SpeedMineModule speedMine = schizo$speedMineModule();
               if (!speedMine.isToggled()) {
                  return;
               }
            }
         }

      }
   }

   @EventHandler
   private void onRenderQueue(WorldRenderEvent.Main event) {
      if (this.shapeSetting.getValue() != NukerShapeChoice.QUEUE) {
         return;
      }

      ExtraBreakTarget extraTask = schizo$speedMineModule().getExtraBreakTarget();
      NukerBlockQueue queueSnapshot = this.blockQueue;

      for (BlockPos queuedPos : queueSnapshot.getPositions()) {
         if (schizo$speedMineModule().isToggled()) {
            SpeedMineModule speedMineForPrimarySnapshot = schizo$speedMineModule();

            if (queuedPos.equals(speedMineForPrimarySnapshot.getMiningPos())) {
               continue;
            }

            if (extraTask != null && queuedPos.equals(extraTask.getPosition())) {
               continue;
            }
         }

         Color queueColor = this.colorSetting.getValue();
         if (queuedPos.equals(this.lastMinedPos)) {
            Color selectedColor = ColorUtils.withAlpha(
               queueColor, 1.0F
            );

            queueColor = ColorUtils.pulse(
               selectedColor, queueColor, 1000.0D, 0.0
            );
         }

         PoseStack poseStack = event.getPoseStack();

         WorldBoxRenderer.outlineBox(
            poseStack, queuedPos, queueColor, 1.0F
         );
      }
   }

   @EventHandler(priority = 200)
   private void onStartDestroyBlock(
      MultiPlayerGameModeStartDestroyBlockEvent event
   ) {
      if (event.isCancelled() || this.internalMining) {
         return;
      }
      if (this.shapeSetting.getValue() == NukerShapeChoice.QUEUE) {
         NukerBlockQueue queueSnapshot = this.blockQueue;
         BlockPos clickedPos = event.getPos();
         queueSnapshot.enqueue(clickedPos);
         event.cancel();
      } else {
         if (this.modeSetting.getValue() == NukerModeChoice.CLICK) {
            BlockPos clickedPosSnapshot = event.getPos();

            if (WorldUtils.canMineBlock(clickedPosSnapshot)) {
               BlockPos clickedPosForBlock = event.getPos();

               this.clickedBlock = WorldUtils.getBlock(clickedPosForBlock);
            }
         }
      }
   }

   private void mineBlock(BlockPos blockPos) {
      if (!WorldUtils.canMineBlock(blockPos)) {
         return;
      }
      Direction miningDirection = Direction.UP;
      if (this.strictDirectionSetting.getValue()) {
         List<Direction> availableFaces = BlockInteractionUtils.getStrictDirections(blockPos);
         if (availableFaces.isEmpty()) {
            return;
         }

         miningDirection = availableFaces.getFirst();
      }

      if (this.rotateSetting.getValue()) {
         RotationManager rotationManagerSnapshot = ClientContext.rotationManager;
         Vec3 centerSnapshot = blockPos.getCenter();
         float[] rotations = RotationManager.getRotationsToFace(centerSnapshot, miningDirection);

         rotationManagerSnapshot.requestRotation(rotations, 5);
      }

      this.blocksMinedThisTick++;
      if (schizo$speedMineModule().isToggled()) {
         SpeedMineModule.automatedMiningStart = true;
         ClientAccess.schizoMinecraft.gameMode.startDestroyBlock(blockPos, miningDirection);
         SpeedMineModule.automatedMiningStart = false;
         this.lastMinedPos = blockPos;

         return;
      }
      int selectedSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
      int bestSlot = -1;

      double bestBreakProgress = -1.0D;
      BlockState blockState = ClientAccess.schizoMinecraft.level.getBlockState(blockPos);
      for (int hotbarSlot = 0; ; hotbarSlot++) {
         if (hotbarSlot >= (9)) {
            break;
         }

         ItemStack stack = ClientAccess.schizoMinecraft.player.getInventory().getItem(hotbarSlot);
         boolean onGround = ClientAccess.schizoMinecraft.player.onGround();
         double breakProgress = WorldUtils.getBreakProgress(
            stack, blockState, onGround
         );
         if (schizo$itemSaverModule().isToggled()) {
            if (!ItemSaverModule.isSafeToUse(stack)) {
               continue;
            }
         }

         if (breakProgress > bestBreakProgress || breakProgress == bestBreakProgress && selectedSlot == hotbarSlot) {
            bestBreakProgress = breakProgress;
            bestSlot = hotbarSlot;
         }

      }

      this.lastMinedPos = blockPos;

      LocalPlayer playerForCreative = ClientAccess.schizoMinecraft.player;
      boolean survivalPlayer = !playerForCreative.isCreative();
      if (this.creativeSetting.getValue()) {
         if (schizo$antiCheatModule().is2b2tEnabled() && survivalPlayer) {
            InventoryUtils.selectHotbarSlotForPlacement(bestSlot);
         }

         BlockPos startPos = this.lastMinedPos;
         Action startAction = Action.START_DESTROY_BLOCK;
         PacketSender.sendPlayerAction(startAction, startPos, miningDirection);

         if (bestBreakProgress < 1.0D) {
            BlockPos stopPos = this.lastMinedPos;
            Action stopAction = Action.STOP_DESTROY_BLOCK;
            PacketSender.sendPlayerAction(stopAction, stopPos, miningDirection);
         }

         SwingInteractionManager swingManager = ClientContext.swingManager;
         InteractionHand swingHand = InteractionHand.MAIN_HAND;
         SwingAction swingType = SwingAction.INTERACT;
         swingManager.swingPacket(swingHand, swingType);
         List<RecentBreak> recentBreaksSnapshot = this.recentBreaks;
         recentBreaksSnapshot.add(new RecentBreak(this.lastMinedPos, System.currentTimeMillis() + 1000L));
      } else {
         if (survivalPlayer) {
            InventoryUtils.selectHotbarSlotForPlacement(bestSlot);
         }

         ItemStack selectedTool = ClientAccess.schizoMinecraft.player.getInventory().getItem(bestSlot);
         BlockState selectedState = ClientAccess.schizoMinecraft.level.getBlockState(blockPos);
         boolean selectedOnGround = ClientAccess.schizoMinecraft.player.onGround();
         this.instantBreak = WorldUtils.getBreakProgress(selectedTool, selectedState, selectedOnGround) >= 1.0D;
         if (this.instantBreak && RotationManager.isMovementSyncEnabled() && RotationManager.is2b2tMode()) {
            Action startAction = Action.START_DESTROY_BLOCK;
            PacketSender.sendPlayerAction(startAction, blockPos, miningDirection);
            List<RecentBreak> recentBreaksSnapshot = this.recentBreaks;
            recentBreaksSnapshot.add(new RecentBreak(this.lastMinedPos, System.currentTimeMillis() + 300L));
         } else {
            ClientAccess.schizoMinecraft.gameMode.continueDestroyBlock(blockPos, miningDirection);
         }
         LocalPlayer playerForSwing = ClientAccess.schizoMinecraft.player;
         InteractionHand mainHand = InteractionHand.MAIN_HAND;
         playerForSwing.swing(mainHand);
      }
   }

   public boolean canMineBlock(BlockPos blockPos) {
      if (schizo$speedMineModule().isToggled()) {
         if (schizo$speedMineModule().getExtraBreakTracker().isTracked(blockPos)) {
            return false;
         }
      }

      if (this.recentBreaks.stream().anyMatch(
         recentBreak -> {
            return recentBreak.blockPos().equals(blockPos);
         }
      )) {
         return false;
      }

      Vec3 eyePosition = ClientAccess.schizoMinecraft.player.getEyePosition();
      double squaredDistance = blockPos.distToCenterSqr(eyePosition);
      double distanceToEye = Math.sqrt(squaredDistance);
      Float selectedRangeValue;
      if (WorldUtils.canSeeBlock(blockPos)) {
         selectedRangeValue = this.rangeSetting.getValue();
      } else {
         selectedRangeValue = this.wallRangeSetting.getValue();
      }

      double allowedRange = selectedRangeValue.floatValue();
      if (schizo$speedMineModule().isToggled()) {
         double speedMineRangeLimit = (schizo$speedMineModule().rangeSetting.getValue()).floatValue();
         allowedRange = Math.min(allowedRange, speedMineRangeLimit);
      }

      if (distanceToEye > allowedRange) {
         return false;
      }

      if (WorldUtils.getBlock(blockPos) instanceof ShulkerBoxBlock
         )
       {
         if (ClientContext.worldStateManager.hasKnownPlacement(blockPos)) {
            return false;
         }
      }

      if ((this.baritoneAreaSetting.getValue())
         && ClientContext.baritoneBridge.isAvailable()
         && !ClientContext.baritoneBridge.isInSelection(blockPos)) {
         return false;
      }

      NukerModeChoice modeSnapshot = this.modeSetting.getValue();

      Block blockSnapshot = BlockInteractionUtils.getBlock(blockPos);
      return modeSnapshot.matchesBlock(blockPos, blockSnapshot, this);
   }

   public static void bindReferencedModules(ModuleRegistry moduleRegistry) {
      schizo$antiCheatModule() = (AntiCheatModule)moduleRegistry.getModule(
         AntiCheatModule.class
      );
      schizo$itemSaverModule() = (ItemSaverModule)moduleRegistry.getModule(
         ItemSaverModule.class
      );
      schizo$speedMineModule() = (SpeedMineModule)moduleRegistry.getModule(
         SpeedMineModule.class
      );
      NukerShapeChoice.bindModules(moduleRegistry);
      NukerBlockQueue.bindModules(moduleRegistry);
   }

   public NukerModule() {
      super("Nuker", "Breaks blocks nearby.", ModuleCategory.PLAYER, new String[0]);
      DefaultedRegistry blockRegistry = BuiltInRegistries.BLOCK;
      Block[] defaultBlocks = new Block[0];
      this.blocksSetting = this.add(new RegistrySetSetting<>("Blocks", blockRegistry, (Object[]) defaultBlocks));
      NukerModeChoice defaultMode = NukerModeChoice.SHULKERS;
      this.modeSetting = this.add(new EnumSetting<>("Mode", defaultMode));
      NukerShapeChoice defaultShape = NukerShapeChoice.SPHERE;
      this.shapeSetting = this.add(new EnumSetting<>("Shape", defaultShape));
      NukerSortChoice defaultSort = NukerSortChoice.CLOSEST;
      this.sortSetting = this.add(new EnumSetting<>("Sort", defaultSort));
      this.bPTSetting = this.add(new NumberSetting<>("BPT", 1, 1, 10));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 50, 0, 500).withUnit("ms"));
      this.rangeSetting = this.add(new NumberSetting<>("Range", 4.5F, 0.0F, 6.0F).withUnit("m"));
      this.wallRangeSetting = this.add(new NumberSetting<>("WallRange", 4.5F, 0.0F, 6.0F).withUnit("m"));
      Predicate<Object> heightVisibility = ignoredValue -> this.shapeSetting.getValue() == NukerShapeChoice.TUNNEL;
      NumberSetting<Integer> heightEditor = new NumberSetting<>("Height", 0, 0, 5, heightVisibility);
      SettingLabelCondition heightAutoMode = SettingLabelCondition.MIN;
      this.heightSetting = this.add(heightEditor.setBoundaryLabel("Auto", heightAutoMode));
      this.strictDirectionSetting = this.add(new BooleanSetting("StrictDirection", false));
      this.rotateSetting = this.add(new BooleanSetting("Rotate", false));
      this.creativeSetting = this.add(new BooleanSetting("Creative", false));
      this.flattenSetting = this.add(new BooleanSetting("Flatten", false));
      this.baritoneAreaSetting = this.add(new BooleanSetting("BaritoneArea", false));
      Predicate<Object> proximityVisibility = ignoredValue -> this.modeSetting.getValue() == NukerModeChoice.SHULKERS;
      this.playerProximitySetting = this.add(new BooleanSetting("PlayerProximity", false, proximityVisibility));
      Predicate<Object> colorVisibility = ignoredValue -> this.shapeSetting.getValue() == NukerShapeChoice.QUEUE;
      Color defaultColor = new Color(1019058687, true);
      this.colorSetting = this.add(new ColorSetting("Color", defaultColor, colorVisibility));
      this.blockQueue = new NukerBlockQueue(this);
      this.recentBreaks = new ArrayList<>();
      this.delayTimer = new ElapsedTimer();
      this.lastMinedPos = null;
      Setting<?> visibilitySetting = this.sortSetting;
      Predicate<Object> visibilityPredicate = ignored -> {
         Setting<NukerShapeChoice> visibilitySnapshot = this.shapeSetting;
         return visibilitySnapshot.getValue() != NukerShapeChoice.QUEUE;
      };

      visibilitySetting.addVisibilityPredicate(visibilityPredicate);
      visibilitySetting = this.bPTSetting;
      Predicate<Integer> batchLimitVisibility = ignored -> {
         SpeedMineModule visibilitySnapshot = schizo$speedMineModule();
         return !visibilitySnapshot.isToggled();
      };

      this.bPTSetting.addVisibilityPredicate(batchLimitVisibility);
      this.baritoneAreaSetting.hideWithoutBaritone();
   }

   private record RecentBreak(
      BlockPos blockPos,
      long expiresAtMillis
   ) {}

   private static AntiCheatModule schizo$antiCheatModule() {
      return ClientContext.requireModule(AntiCheatModule.class);
   }

   private static ItemSaverModule schizo$itemSaverModule() {
      return ClientContext.requireModule(ItemSaverModule.class);
   }

   private static SpeedMineModule schizo$speedMineModule() {
      return ClientContext.requireModule(SpeedMineModule.class);
   }
}
