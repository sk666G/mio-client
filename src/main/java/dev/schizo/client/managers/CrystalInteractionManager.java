/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.modules.misc.SwingModule.HandChoice;
import dev.schizo.client.modules.misc.SwingModule.TypeChoice;

import dev.schizo.client.utils.entity.PlayerState;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.network.PacketSender;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.AutoCrystalInstantChoice;
import dev.schizo.client.settings.options.AutoCrystalWeaknessChoice;

import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CrystalInteractionManager implements ClientAccess {
   private static final SwingModule swing;
   private static final AutoCrystalModule autoCrystal;
   private final ElapsedTimer attackCooldownTimer = new ElapsedTimer();
   private long additionalCooldownMillis;
   private int lastAttackedEntityId;

   public int getMinimumCrystalAge() {
      return autoCrystal.ticksExistedSetting.getValue();
   }

   public void setLastAttackedEntityId(int entityId) {
      this.lastAttackedEntityId = entityId;
   }

   public boolean isRotationEnabled() {
      return autoCrystal.rotateSetting.getValue();
   }

   public boolean isInstantAttackEnabled() {
      return autoCrystal.instantSetting.getValue() != AutoCrystalInstantChoice.NONE;
   }

   private boolean selectWeaknessWeapon() {
      ItemStack heldStack = ClientAccess.schizoMinecraft.player.getMainHandItem();
      DataComponentType<?> toolComponent = DataComponents.TOOL;

      boolean holdingTool = heldStack.has(toolComponent);

      if (autoCrystal.weaknessSetting.getValue() == AutoCrystalWeaknessChoice.NONE) {
         return false;
      }
      LocalPlayer weaknessPlayer = ClientAccess.schizoMinecraft.player;
      Holder<MobEffect> weaknessEffect = MobEffects.WEAKNESS;
      if (!weaknessPlayer.hasEffect(weaknessEffect) || holdingTool) {
         return false;
      }
      Predicate<ItemStack> weaponPredicate = candidateStack -> {
         DataComponentType<?> weaponComponent = DataComponents.WEAPON;
         return candidateStack.has(weaponComponent);
      };

      int weaponSlot = InventoryUtils.findHotbarSlot(weaponPredicate);
      if (weaponSlot == -1) {
         return false;
      }

      if (autoCrystal.weaknessSetting.getValue() == AutoCrystalWeaknessChoice.SILENT) {
         autoCrystal.ignoreNextSlotChange();
      }

      InventoryUtils.selectHotbarSlotForPlacement(weaponSlot);
      return true;
   }

   public int getAttackDelayMillis() {
      return autoCrystal.breakDelaySetting.getValue();
   }

   private void scheduleClientAttack(int entityId) {
      if (!RotationManager.isMovementSyncEnabled()) {
         return;
      }
      if (RotationManager.is2b2tMode()) {
         return;
      }
      var eventLoop = ClientAccess.schizoMinecraft;
      var expectedLevel = ClientAccess.schizoMinecraft.level;
      var expectedPlayer = ClientAccess.schizoMinecraft.player;
      Runnable attackTask = () -> {
         if (ClientAccess.schizoMinecraft.level != expectedLevel
            || ClientAccess.schizoMinecraft.player != expectedPlayer
            || expectedLevel == null
            || expectedPlayer == null) {
            return;
         }
         Entity targetEntity = expectedLevel.getEntity(entityId);
         if (targetEntity != null) {
            expectedPlayer.attack(targetEntity);
         }
      };

      eventLoop.execute(attackTask);
   }

   public CrystalInteractionManager() {
      swing = (SwingModule)java.util.Objects.requireNonNull(
         ClientContext.getModule(SwingModule.class),
         "Required module is not registered: SwingModule"
      );
      autoCrystal = (AutoCrystalModule)java.util.Objects.requireNonNull(
         ClientContext.getModule(AutoCrystalModule.class),
         "Required module is not registered: AutoCrystalModule"
      );
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   private void resetAttackCooldown(InteractionHand hand) {
      ((PlayerState)ClientAccess.schizoMinecraft.player).schizo$resetLastAttackedServer();
      ElapsedTimer cooldownTimerRef = this.attackCooldownTimer;
      long elapsedThreshold = 200L + this.additionalCooldownMillis;
      if (!cooldownTimerRef.hasElapsedAndReset(elapsedThreshold) || hand != InteractionHand.MAIN_HAND) {
         return;
      }
      Setting<HandChoice> handSetting = swing.handSetting;
      if (handSetting.getValue() == SwingModule.HandChoice.OFFHAND) {
         return;
      }
      Setting<TypeChoice> typeSetting = swing.typeSetting;
      if (typeSetting.getValue() == SwingModule.TypeChoice.VANILLA) {
         return;
      }

      ClientAccess.schizoMinecraft.player.resetOnlyAttackStrengthTicker();
      double randomFraction = Math.random();

      this.additionalCooldownMillis = (long)(
         randomFraction * 300.0D
      );
   }

   public boolean attackEntity(int entityId, boolean instantAttack) {
      if (!this.isInstantAttackEnabled() && instantAttack) {
         return false;
      }

      if (this.lastAttackedEntityId != -1 && RotationManager.isMovementSyncEnabled()) {
         return false;
      }

      int previousSlot = InventoryUtils.getSelectedSlot();

      Entity targetEntity = ClientAccess.schizoMinecraft.level.getEntity(entityId);
      boolean switchedForWeakness = false;
      if (targetEntity instanceof EndCrystal) {
         switchedForWeakness = this.selectWeaknessWeapon();
      }

      this.lastAttackedEntityId = entityId;

      InteractionHand hand = this.selectAttackHand(targetEntity, instantAttack);

      PacketSender.sendAttack(this.lastAttackedEntityId);
      if (!instantAttack) {
         ClientContext.swingManager.swingClientSide(hand);
      }

      SwingAction swingAction = SwingAction.HIT;
      InteractionHand packetSwingHand = InteractionHand.MAIN_HAND;

      ClientContext.swingManager.swingPacket(packetSwingHand, swingAction);

      this.resetAttackCooldown(hand);
      this.scheduleClientAttack(entityId);
      if (switchedForWeakness) {
         if (autoCrystal.weaknessSetting.getValue() == AutoCrystalWeaknessChoice.SILENT) {
            autoCrystal.ignoreNextSlotChange();

            InventoryUtils.selectHotbarSlotForPlacement(previousSlot);
         }
      }

      return true;
   }

   private InteractionHand selectAttackHand(
      Entity entity, boolean instantAttack
   ) {
      if (entity instanceof EndCrystal && !instantAttack) {
         return ClientAccess.schizoMinecraft.player.getOffhandItem().getItem() == Items.END_CRYSTAL ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      } else {
         return InteractionHand.MAIN_HAND;
      }
   }

   public boolean attackEntity(int entityId) {
      return this.attackEntity(entityId, false);
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.lastAttackedEntityId = -1;
      this.additionalCooldownMillis = 0L;
      this.attackCooldownTimer.clear();
   }

   @EventHandler(priority = -300)
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.lastAttackedEntityId = -1;
   }
}
