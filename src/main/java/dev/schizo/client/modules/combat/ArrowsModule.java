/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.CancellableEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MultiPlayerGameModeReleaseUsingItemEvent;
import dev.schizo.client.events.MultiPlayerGameModeUseItemEvent;
import dev.schizo.client.events.MultiPlayerGameModeUseItemOnEvent;
import dev.schizo.client.access.MobEffectDurationAccess;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.inventory.PotionUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import dev.schizo.mixin.MinecraftAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.alchemy.Potion;

public class ArrowsModule extends Module {
   private static final AtomicBoolean autoShotInProgress;
   public final Setting<Boolean> autoShootSetting;
   private final Setting<Integer> speedSetting;
   public final Setting<Boolean> beforePullSetting;
   private final Setting<Float> fovSetting;
   private final Setting<Integer> assumeDurationSetting;
   private final Map<MobEffect, Long> recentEffectTimes;
   private final List<MobEffect> pendingEffects;
   private final ElapsedTimer arrowSwapTimer;
   private int selectedArrowSlot;
   private Potion selectedArrowPotion;
   private boolean allowNextProjectileUse;

   static {
      autoShotInProgress = new AtomicBoolean();
   }

   @Override
   public void onEnable() {
      if (autoShotInProgress.get()) {
         this.disable();
      }
   }

   private void interceptProjectileUse(CancellableEvent event, InteractionHand hand) {
      if (ClientAccess.schizoMinecraft.player.getItemInHand(hand).getItem() instanceof ProjectileWeaponItem) {
         if (this.allowNextProjectileUse) {
            this.allowNextProjectileUse = false;
         } else {
            event.cancel();

            this.prepareArrow();
            TaskScheduler scheduler = ClientContext.taskScheduler;
            Runnable clearCooldown = this::clearItemUseCooldown;
            scheduler.scheduleRunTickTask(clearCooldown);
            this.allowNextProjectileUse = true;
         }
      }
   }

   @EventHandler
   private void onMultiPlayerGameModeUseItemEvent(MultiPlayerGameModeUseItemEvent event) {
      if (this.beforePullSetting.getValue()) {
         InteractionHand hand = event.getHand();
         this.interceptProjectileUse(event, hand);
      }
   }

   @EventHandler
   private void onMultiPlayerGameModeReleaseUsingItemEvent(
      MultiPlayerGameModeReleaseUsingItemEvent event
   ) {
      InteractionHand hand = ClientAccess.schizoMinecraft.player.getUsedItemHand();

      Player target = this.findEffectTarget();

      if (ClientAccess.schizoMinecraft.player.getItemInHand(hand).getItem() instanceof ProjectileWeaponItem) {
         if (target == null || this.selectedArrowSlot != -1
            && this.scoreArrow(ClientAccess.schizoMinecraft.player.getInventory().getItem(this.selectedArrowSlot), target) > 0) {
            this.rememberReleasedEffects();

            if (!this.beforePullSetting.getValue()) {
               this.prepareArrow();
            }

            return;
         }

         int previousHotbarSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
         int temporarySlotIndex = previousHotbarSlot + 1;

         temporarySlotIndex %= 9;

         InventoryUtils.selectHotbarSlotForPlacement(temporarySlotIndex);
         Direction releaseDirection = Direction.DOWN;
         BlockPos releasePosition = BlockPos.ZERO;
         Action releaseAction = Action.RELEASE_USE_ITEM;
         PacketSender.sendPlayerAction(releaseAction, releasePosition, releaseDirection);

         InventoryUtils.selectHotbarSlotForPlacement(previousHotbarSlot);
         event.cancel();
      }
   }

   private int scoreArrow(ItemStack stack, Player player) {
      if (stack.getItem() instanceof ArrowItem && player != null) {
         Potion potion = PotionUtils.getPotion(stack);
         boolean beneficialTarget = player == ClientAccess.schizoMinecraft.player
            || ClientContext.socialRegistry.isFriendPlayer(player);
         int score = 0;
         if (potion.getEffects().isEmpty()) {
            score = beneficialTarget ? -1 : 1;
         }

         Iterator<MobEffectInstance> effects = potion.getEffects().iterator();

         while (effects.hasNext()) {
            MobEffectInstance effect = effects.next();
            Holder<MobEffect> effectType = effect.getEffect();
            boolean alreadyCovered = player.hasEffect(effectType);
            if (alreadyCovered) {
               MobEffectInstance existingEffect = player.getEffect(effectType);
               float remainingDuration = ((MobEffectDurationAccess)existingEffect).schizo$getDurationRatio();

               float scaledDuration = remainingDuration * 100.0F;

               Integer assumedDuration = this.assumeDurationSetting.getValue();
               if (scaledDuration < assumedDuration.intValue()) {
                  alreadyCovered = false;
               }
            }

            if (!alreadyCovered) {
               Map<MobEffect, Long> effectTimes = this.recentEffectTimes;
               Object recentEffect = effectType.value();
               if (!effectTimes.containsKey(recentEffect)) {
                  MobEffect effectValueSnapshot = (MobEffect)effectType.value();
                  int amplifier;
                  if (!effectValueSnapshot.isInstantenous()) {
                     amplifier = effect.getAmplifier();
                  } else {
                     amplifier = 0;
                  }

                  int effectScore = amplifier + 1;
                  if (this.isBeneficialArrowEffect(effectType) != beneficialTarget) {
                     effectScore *= -1;
                  }

                  score += effectScore;
               }
            }
         }

         return score;
      } else {
         return -1;
      }
   }

   @EventHandler(priority = 200)
   private void onLocalPlayerPreTick(LocalPlayerPreTickEvent event) {
      Player target = this.findEffectTarget();
      if (target == null) {
         target = ClientAccess.schizoMinecraft.player;
      }

      this.recentEffectTimes.entrySet().removeIf(entry -> {
         Long effectTime = entry.getValue();
         long expiryTime = effectTime;

         expiryTime += 750L;
         return expiryTime < System.currentTimeMillis();
      });
      int bestScore = -1;
      int bestSlot = -1;
      for (int slot = 0; slot < ClientAccess.schizoMinecraft.player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = ClientAccess.schizoMinecraft.player.getInventory().getItem(slot);
         Player effectTarget = target;
         int score = this.scoreArrow(stack, effectTarget);
         if (score >= 0 && score > bestScore) {
            bestSlot = slot;
            bestScore = score;
         }
      }

      this.selectedArrowSlot = bestSlot;
      if (this.selectedArrowSlot != -1) {
         ItemStack selectedStackSnapshot = ClientAccess.schizoMinecraft.player.getInventory().getItem(this.selectedArrowSlot);

         this.selectedArrowPotion = PotionUtils.getPotion(
            selectedStackSnapshot
         );
      }
   }

   @EventHandler
   private void onMultiPlayerGameModeUseItemOnEvent(MultiPlayerGameModeUseItemOnEvent event) {
      if (this.beforePullSetting.getValue()) {
         InteractionHand hand = event.getHand();
         this.interceptProjectileUse(event, hand);
      }
   }

   private boolean isBeneficialArrowEffect(Holder<MobEffect> effectType) {
      if (effectType == MobEffects.SLOW_FALLING) {
         return false;
      }

      return ((MobEffect)effectType.value()).isBeneficial();
   }

   @EventHandler
   private void onPlayerUpdate(RotationRequestEvent event) {
      if (!this.autoShootSetting.getValue()) {
         return;
      }
      if (this.selectedArrowSlot == -1) {
         this.disable();
         return;
      }

      ItemStack arrowStackSnapshot = ClientAccess.schizoMinecraft.player.getInventory().getItem(this.selectedArrowSlot);
      LocalPlayer effectTarget = ClientAccess.schizoMinecraft.player;
      if (this.scoreArrow(arrowStackSnapshot, effectTarget) <= 0) {
         this.disable();
         return;
      }

      this.startAutoShot();
   }

   public boolean isAutoShooting() {
      return this.isToggled() && this.autoShootSetting.getValue();
   }

   public boolean shouldBypassUsingItemCheck() {
      if (this.isToggled()) {
         if (this.autoShootSetting.getValue()) {
            Item bowItem = Items.BOW;

            if (InventoryUtils.findHotbarSlot(bowItem) != -1) {
               if (!ClientAccess.schizoMinecraft.player.isUsingItem()) {
                  return true;
               }

               if (autoShotInProgress.get()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void finishAutoShot() {
      autoShotInProgress.set(false);
      this.disable();
   }

   private void rememberReleasedEffects() {
      Iterator<MobEffect> effects = this.pendingEffects.iterator();

      while (effects.hasNext()) {
         MobEffect effect = effects.next();
         Map<MobEffect, Long> effectTimes = this.recentEffectTimes;
         long releaseTime = System.currentTimeMillis();
         effectTimes.put(effect, releaseTime);
      }

      this.pendingEffects.clear();
   }

   private void clearItemUseCooldown() {
      ((MinecraftAccessor)ClientAccess.schizoMinecraft).setItemUseCooldown(0);
   }

   private void startAutoShot() {
      int previousHotbarSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
      Item bowItem = Items.BOW;

      int bowSlot = InventoryUtils.findHotbarSlot(bowItem);
      RotationManager rotationManagerSnapshot = ClientContext.rotationManager;

      float[] rotationArray = new float[2];
      LocalPlayer yawPlayer = ClientAccess.schizoMinecraft.player;
      rotationArray[0] = yawPlayer.getYRot();

      rotationArray[1] = -MathConstants.NINETY_DEGREES;

      rotationManagerSnapshot.requestRotation(rotationArray, 999);
      if (bowSlot != -1) {
         if (!ClientAccess.schizoMinecraft.player.isUsingItem()) {
            float previousPitch = ClientAccess.schizoMinecraft.player.getXRot();

            InventoryUtils.selectHotbarSlot(bowSlot);

            ClientAccess.schizoMinecraft.player.setXRot(-MathConstants.NINETY_DEGREES);
            MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
            LocalPlayer usePlayer = ClientAccess.schizoMinecraft.player;
            InteractionHand useHand = InteractionHand.MAIN_HAND;
            boolean useStarted = false;
            try {
               gameMode.useItem(usePlayer, useHand);
               useStarted = true;
            } finally {
               usePlayer.setXRot(previousPitch);
               if (!useStarted) {
                  InventoryUtils.selectHotbarSlot(previousHotbarSlot);
               }
            }

            if (!autoShotInProgress.get()) {
               autoShotInProgress.set(true);
               TaskScheduler scheduler = ClientContext.taskScheduler;
               Runnable releaseShot = () -> {
                  ClientAccess.schizoMinecraft.gameMode.releaseUsingItem(ClientAccess.schizoMinecraft.player);

                  InventoryUtils.selectHotbarSlot(previousHotbarSlot);
                  TaskScheduler finishScheduler = ClientContext.taskScheduler;
                  Runnable finishShot = this::finishAutoShot;
                  finishScheduler.scheduleRotationTask(finishShot, 1);
               };

               int releaseDelay = this.speedSetting.getValue();
               scheduler.scheduleRotationTask(releaseShot, releaseDelay);
            }

            return;
         }
      }
   }

   private Player findEffectTarget() {
      if (this.autoShootSetting.getValue()) {
         return ClientAccess.schizoMinecraft.player;
      }

      float angleValue = ClientAccess.schizoMinecraft.player.getXRot();

      if (angleValue < -85.0F) {
         return ClientAccess.schizoMinecraft.player;
      }

      AbstractClientPlayer bestPlayer = null;

      double bestAngleDistance = 3.4028234663852886e+38D;

      Iterator<AbstractClientPlayer> players = ClientAccess.schizoMinecraft.level.players().iterator();

      while (players.hasNext()) {
         AbstractClientPlayer candidate = (AbstractClientPlayer)players.next();
         if (CombatTargetPolicy.isEligible(
            candidate.isAlive(),
            candidate.isDeadOrDying(),
            candidate == ClientAccess.schizoMinecraft.player,
            candidate.isSpectator(),
            candidate.isCreative(),
            ClientContext.socialRegistry.isFriendPlayer(candidate),
            ((ServerSideDeathState)candidate).isServerSideDead()
         )) {
               float[] rotations = RotationManager.getRotationsToEntity(candidate);

               angleValue = ClientAccess.schizoMinecraft.player.getYRot();
               float targetYaw = rotations[0];

               angleValue = Mth.degreesDifferenceAbs(angleValue, targetYaw);
               Pose yawPose = candidate.getPose();
               EntityDimensions yawDimensions = candidate.getDimensions(yawPose);
               float targetWidth = yawDimensions.width();

               float yawDistance = angleValue - targetWidth * 0.5F;

               angleValue = ClientAccess.schizoMinecraft.player.getXRot();
               float targetPitch = rotations[1];

               angleValue = Mth.degreesDifferenceAbs(angleValue, targetPitch);
               Pose pitchPose = candidate.getPose();
               EntityDimensions pitchDimensions = candidate.getDimensions(pitchPose);
               float targetHeight = pitchDimensions.height();

               float pitchDistance = angleValue - targetHeight * 0.5F;

               Float yawFov = this.fovSetting.getValue();
               if (!(yawDistance > yawFov)) {
                  Float pitchFov = this.fovSetting.getValue();
                  if (!(pitchDistance > pitchFov)) {
                     double yawAsDouble = yawDistance;
                     double hypotPitch = pitchDistance;
                     double angleDistance = Math.hypot(yawAsDouble, hypotPitch);
                     if (angleDistance < bestAngleDistance) {
                        bestPlayer = candidate;
                        bestAngleDistance = angleDistance;
                     }
                  }
               }
            }
         }

      return bestPlayer;
   }

   private void prepareArrow() {
      ElapsedTimer timerSnapshot = this.arrowSwapTimer;

      if (!timerSnapshot.hasElapsed(50L)) {
         return;
      }
      for (int slot = 0; slot < ClientAccess.schizoMinecraft.player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = ClientAccess.schizoMinecraft.player.getInventory().getItem(slot);
         if (!(stack.getItem() instanceof ArrowItem)) {
            continue;
         }
         if (slot != this.selectedArrowSlot) {
            int firstArrowSlot = slot;
            timerSnapshot = this.arrowSwapTimer;

            timerSnapshot.reset();

            ClientContext.inventoryStateManager.setClickBatching(true);
            RotationManager rotationManager = ClientContext.rotationManager;
            Runnable swapArrows = () -> {
               int sourceMenuSlot = InventoryUtils.toMenuSlot(firstArrowSlot);
               int destinationMenuSlot = InventoryUtils.toMenuSlot(this.selectedArrowSlot);
               InventoryUtils.swapSlotsWithPickup(sourceMenuSlot, destinationMenuSlot);
            };

            rotationManager.executeAction(swapArrows);

            ClientContext.inventoryStateManager.setClickBatching(false);
            this.selectedArrowSlot = -1;
         }

         if (this.selectedArrowPotion != null) {
            Iterator<MobEffectInstance> effects = this.selectedArrowPotion.getEffects().iterator();

            while (effects.hasNext()) {
               MobEffectInstance effect = effects.next();
               List<MobEffect> effectList = this.pendingEffects;
               Holder<MobEffect> pendingEffectType = effect.getEffect();
               effectList.add((MobEffect)pendingEffectType.value());
            }
         }

         return;
      }
   }

   public ArrowsModule() {
      super("Arrows", "Will swap between effect arrows in your inventory.", ModuleCategory.COMBAT, new String[]{"projectiles"});

      this.autoShootSetting = this.add(new BooleanSetting("AutoShoot", false).markVisibilityDependency());

      this.speedSetting = this.add(new NumberSetting<>("Speed", 3, 3, 8).dependsOn(this.autoShootSetting));
      this.beforePullSetting = this.add(new BooleanSetting("BeforePull", false));
      String fovKey = "Fov";
      Predicate<Float> fovVisibility = unused -> !this.autoShootSetting.getValue();
      this.fovSetting = this.add(new NumberSetting<>(fovKey, 20.0F, 10.0F, 45.0F, fovVisibility));

      this.assumeDurationSetting = this.add(new NumberSetting<>("AssumeDuration", 50, 0, 100).withUnit("%"));
      this.recentEffectTimes = new HashMap<>();
      this.pendingEffects = new ArrayList<>();
      this.arrowSwapTimer = new ElapsedTimer();
   }
}
