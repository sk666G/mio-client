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
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.managers.InventoryStateManager;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.render.FeatureBoxRenderer;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.network.ServerSideDeathState;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import java.awt.Color;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class PistonCrystalModule extends Module {
   private static AntiCheatModule antiCheat;
   private static MultiTaskModule multiTask;
   private final Setting<Integer> delaySetting;
   final Setting<Float> placeRangeSetting;
   final Setting<Float> attackRangeSetting;
   private final Setting<Float> targetRangeSetting;
   final Setting<Boolean> airPlaceSetting;
   final Setting<Boolean> rotateSetting;
   private final Setting<Boolean> alternativeSetting;
   private final Setting<Boolean> renderSetting;
   private final Setting<Color> fillSetting;
   private final Setting<Color> outlineSetting;
   private final Setting<Boolean> fadeSetting;
   private final Setting<Float> fadeTimeSetting;
   private final PistonCrystalPlanner planner;
   final PistonCrystalStageTracker stageTracker;
   private final ElapsedTimer pistonTimer = new ElapsedTimer();
   private final ElapsedTimer crystalTimer = new ElapsedTimer();
   private final ElapsedTimer forcedCrystalTimer = new ElapsedTimer();
   private final ElapsedTimer cycleTimer = new ElapsedTimer();
   private Item powerItem = Items.AIR;
   private Player target;
   private PistonCrystalPlan plan;
   private boolean pistonPlaced;
   private boolean crystalPlaced;
   private boolean powerPlaced;
   private boolean forceCrystal;
   private boolean wasBlocked;

   public PistonCrystalModule() {
      super("PistonCrystal", "Pushes crystals into your enemies to explode them.", ModuleCategory.COMBAT);
      this.delaySetting = this.add(new NumberSetting<>("Delay", 500, 0, 1000).withUnit("ms"));
      this.placeRangeSetting = this.add(new NumberSetting<>("PlaceRange", 4.5F, 0.1F, 6.0F));
      this.attackRangeSetting = this.add(new NumberSetting<>("AttackRange", 4.0F, 0.1F, 6.0F));
      this.targetRangeSetting = this.add(new NumberSetting<>("TargetRange", 10.0F, 1.0F, 20.0F));
      this.airPlaceSetting = this.add(new BooleanSetting("AirPlace", false));
      this.rotateSetting = this.add(new BooleanSetting("Rotate", true));
      this.alternativeSetting = this.add(new BooleanSetting("Alternative", false));
      this.renderSetting = this.add(new BooleanSetting("Render", true).markVisibilityDependency());
      this.fillSetting = this.add(new ColorSetting("Fill", new Color(1136499199, true)).dependsOn(this.renderSetting));
      this.outlineSetting = this.add(new ColorSetting("Outline", new Color(-910321153, true)).dependsOn(this.renderSetting));
      this.fadeSetting = this.add(new BooleanSetting("Fade", true).markVisibilityDependency().dependsOn(this.renderSetting));
      this.fadeTimeSetting = this.add(new NumberSetting<>("FadeTime", 0.5F, 0.1F, 1.0F).withUnit("s").dependsOn(this.renderSetting, this.fadeSetting));
      this.planner = new PistonCrystalPlanner(this);
      this.stageTracker = new PistonCrystalStageTracker();
      ClientContext.featureBoxRenderer.register(new FeatureBoxRenderer.RenderConfiguration(
         this, this.fillSetting, this.outlineSetting, () -> 1.0F, this.fadeTimeSetting, () -> true, this.fadeSetting, 1000
      ));
   }

   public static void bindModules(ModuleRegistry modules) {
      antiCheat = (AntiCheatModule)modules.getModule(AntiCheatModule.class);
      multiTask = (MultiTaskModule)modules.getModule(MultiTaskModule.class);
   }

   @Override
   public String getInfo() {
      return this.target == null ? null : this.target.getGameProfile().name();
   }

   @Override
   public void onToggle() {
      this.resetCycleState();
   }

   private void resetCycleState() {
      this.plan = null;
      this.target = null;
      this.powerItem = Items.AIR;
      this.resetStages();
      this.stageTracker.reset();
      this.forceCrystal = false;
      this.wasBlocked = false;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.resetCycleState();
   }

   @EventHandler
   private void onRotationRequest(RotationRequestEvent event) {
      if (this.isWorldUnavailable() || ClientAccess.schizoMinecraft.gameMode == null) {
         return;
      }
      this.stageTracker.update();
      this.selectPowerItem();
      Player nextTarget = this.selectTarget();
      if (nextTarget != this.target) {
         this.plan = null;
         this.resetStages();
         this.stageTracker.reset();
         this.target = nextTarget;
      }
      if (this.target == null || multiTask.shouldPauseModule(this)) {
         return;
      }
      if (!PistonCrystalPlanner.hasPowerSource(
         this.powerItem == Items.REDSTONE_BLOCK, this.powerItem == Items.REDSTONE_TORCH
      )) {
         this.plan = null;
         this.resetStages();
         this.stageTracker.reset();
         return;
      }

      PistonCrystalPlan nextPlan = this.planner.findPlan(this.target);
      if (nextPlan == null) {
         // A piston head can make the original crystal cell invalid for placement after a push.
         if (this.plan != null && this.powerPlaced && this.crystalPlaced
            && this.stageTracker.isPending(PistonCrystalStageTracker.Kind.CRYSTAL, this.plan.crystalPosition())) {
            this.attackCrystals(false);
         } else {
            this.plan = null;
            this.resetStages();
         }
         return;
      }
      this.plan = nextPlan;
      if (this.pistonPlaced && this.crystalPlaced && this.powerPlaced && this.cycleTimer.hasElapsed(this.delaySetting.getValue())) {
         this.resetStages();
      }
      if (this.isBlocked()) {
         this.attackCrystals(true);
         if (this.forceCrystal) {
            this.placeCrystal(true);
         }
         this.wasBlocked = true;
      } else {
         if (this.wasBlocked) {
            this.resetStages();
            this.wasBlocked = false;
         }
         this.attackCrystals(false);
         this.placePiston();
         this.placePower();
         this.placeCrystal(false);
      }
   }

   private Player selectTarget() {
      Player selected = null;
      double distance = this.targetRangeSetting.getValue();
      for (Player candidate : ClientAccess.schizoMinecraft.level.players()) {
         if (!CombatTargetPolicy.isEligible(
            candidate.isAlive(),
            candidate.isDeadOrDying(),
            candidate == ClientAccess.schizoMinecraft.player,
            candidate.isSpectator(),
            candidate.isCreative(),
            ClientContext.socialRegistry.isFriendPlayer(candidate),
            ((ServerSideDeathState)candidate).isServerSideDead()
         ) || !PlayerStateUtils.hasArmor(candidate)) {
            continue;
         }
         double candidateDistance = ClientAccess.schizoMinecraft.player.getEyePosition().distanceTo(candidate.position());
         if (candidateDistance <= distance) {
            selected = candidate;
            distance = candidateDistance;
         }
      }
      return selected;
   }

   private void placePiston() {
      if (this.pistonPlaced) {
         return;
      }
      if (ClientAccess.schizoMinecraft.level.getBlockState(this.plan.pistonPosition()).is(Blocks.PISTON)) {
         this.pistonPlaced = true;
         this.pistonTimer.reset();
         return;
      }
      float[] rotations = null;
      if (!this.rotateSetting.getValue() || this.airPlaceSetting.getValue() && RotationManager.isMovementSyncEnabled()) {
         rotations = new float[]{Mth.wrapDegrees(this.plan.pistonFacing().toYRot() - 180.0F), 0.0F};
      }
      if (this.placeItem(Items.PISTON, this.plan.pistonHit(), rotations)) {
         if (this.renderSetting.getValue()) {
            ClientContext.featureBoxRenderer.addBox(this, this.plan.pistonPosition());
         }
         this.pistonPlaced = true;
         this.pistonTimer.reset();
         this.stageTracker.markPiston(this.plan.pistonPosition(), this.plan.pistonFacing());
      }
   }

   private void placeCrystal(boolean force) {
      if (!force && (!this.powerPlaced || this.crystalPlaced) || force && !this.forcedCrystalTimer.hasElapsed(500L)) {
         return;
      }
      if (this.hasBlockingEntities(new AABB(this.plan.crystalPosition()), this::isOldEntity)) {
         return;
      }
      if (this.placeItem(Items.END_CRYSTAL, this.plan.crystalHit(), null)) {
         this.crystalPlaced = true;
         this.crystalTimer.reset();
         this.stageTracker.markCrystal(this.plan.crystalPosition());
         if (force) {
            this.forcedCrystalTimer.reset();
         }
      }
   }

   private void placePower() {
      if (this.pistonPlaced && !this.powerPlaced && this.placeItem(this.powerItem, this.plan.powerHit(), null)) {
         if (this.renderSetting.getValue()) {
            ClientContext.featureBoxRenderer.addBox(this, this.plan.powerPosition());
         }
         this.stageTracker.markPower(this.plan.powerPosition());
         this.powerPlaced = true;
         this.cycleTimer.reset();
      }
   }

   private void attackCrystals(boolean force) {
      if (!force && !this.powerPlaced) {
         return;
      }
      EndCrystal selected = null;
      double distance = this.attackRangeSetting.getValue();
      for (Entity entity : ClientAccess.schizoMinecraft.level.entitiesForRendering()) {
         if (!(entity instanceof EndCrystal crystal)) {
            continue;
         }
         if (!force && crystal.getX() == this.plan.crystalPosition().getX() + 0.5D
            && crystal.getZ() == this.plan.crystalPosition().getZ() + 0.5D) {
            continue;
         }
         if (!crystal.getBoundingBox().intersects(this.crystalBounds(this.plan.crystalPosition()))
            && (!force || !crystal.getBoundingBox().intersects(new AABB(this.plan.pistonPosition())))) {
            continue;
         }
         double candidateDistance = ClientAccess.schizoMinecraft.player.getEyePosition().distanceTo(crystal.position());
         if (candidateDistance <= distance) {
            selected = crystal;
            distance = candidateDistance;
         }
      }
      if (selected != null) {
         if (this.rotateSetting.getValue()) {
            ClientContext.rotationManager.requestRotation(RotationManager.getRotationsToPosition(selected.getBoundingBox().getCenter()), 250);
         }
         BlockInteractionUtils.attackEntity(selected);
      }
   }

   private boolean placeItem(Item item, BlockHitResult hit, float[] forcedRotations) {
      if (hit == null) {
         return false;
      }
      boolean alternative = this.alternativeSetting.getValue();
      if (antiCheat.is2b2tEnabled() && InventoryStateManager.wouldReachClickLimit(ClientContext.inventoryStateManager, 2)) {
         alternative = false;
      }
      int previousSlot = InventoryUtils.getSelectedSlot();
      InventoryUtils.SwapSlot slot = InventoryUtils.findSwapSlot(item, alternative);
      if (!slot.isFound() || !slot.hotbar() && slot.hand() == InteractionHand.MAIN_HAND
         && ClientAccess.schizoMinecraft.player.containerMenu != ClientAccess.schizoMinecraft.player.inventoryMenu) {
         return false;
      }
      if (this.rotateSetting.getValue()) {
         ClientContext.rotationManager.requestRotation(RotationManager.getRotationsToPosition(hit.getLocation()), 250);
      } else if (forcedRotations != null) {
         ClientContext.rotationManager.requestRotation(forcedRotations, 250);
      }
      Runnable interaction = () -> {
         PacketSender.sendUseItemOn(slot.hand(), hit);
         ClientContext.swingManager.swing(slot.hand(), SwingAction.INTERACT, true);
      };
      InventoryUtils.runWithSwap(previousSlot, slot, () -> {
         if (BlockInteractionUtils.requiresSneakForInteraction(hit.getBlockPos())) {
            ClientContext.playerStateTracker.runSneaking(interaction);
         } else {
            interaction.run();
         }
      });
      return true;
   }

   private boolean isBlocked() {
      var piston = ClientAccess.schizoMinecraft.level.getBlockState(this.plan.pistonPosition());
      boolean blocked = false;
      this.forceCrystal = false;
      if (!(piston.getBlock() instanceof PistonBaseBlock)) {
         blocked = this.hasBlockingEntities(new AABB(this.plan.pistonPosition()), entity -> !entity.isSpectator() && !(entity instanceof ItemEntity));
      } else if (piston.getValue(DirectionalBlock.FACING) != this.plan.pistonFacing()
         || piston.getValue(PistonBaseBlock.EXTENDED) && this.cycleTimer.hasElapsed(300L) && this.powerPlaced) {
         this.forceCrystal = true;
      }
      if (this.hasBlockingEntities(new AABB(this.plan.crystalPosition()), this::isOldEntity) || this.crystalPlaced && this.powerPlaced) {
         blocked = true;
      }
      return blocked || this.forceCrystal;
   }

   private boolean isOldEntity(Entity entity) {
      return !entity.isSpectator() && entity.tickCount >= 10;
   }

   private void resetStages() {
      this.pistonPlaced = false;
      this.crystalPlaced = false;
      this.powerPlaced = false;
   }

   private void selectPowerItem() {
      if (InventoryUtils.findSwapSlot(Items.REDSTONE_BLOCK, this.alternativeSetting.getValue()).isFound()) {
         this.powerItem = Items.REDSTONE_BLOCK;
      } else if (InventoryUtils.findSwapSlot(Items.REDSTONE_TORCH, this.alternativeSetting.getValue()).isFound()) {
         this.powerItem = Items.REDSTONE_TORCH;
      } else {
         this.powerItem = Items.AIR;
      }
   }

   Item getPowerItem() {
      return this.powerItem;
   }

   boolean hasBlockingEntities(AABB bounds, Predicate<Entity> predicate) {
      for (Entity entity : ClientAccess.schizoMinecraft.level.entitiesForRendering()) {
         if (predicate.test(entity) && entity.getBoundingBox().intersects(bounds)) {
            return true;
         }
      }
      return false;
   }

   AABB crystalBounds(BlockPos position) {
      return new AABB(position).expandTowards(0.0D, 1.0D, 0.0D);
   }
}
