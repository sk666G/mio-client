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
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.Vec3;

public final class AutoMountModule extends Module {
   private final Setting<Double> rangeSetting;
   private final Setting<Float> delaySetting;
   private final Setting<Boolean> rotateSetting;
   private final ElapsedTimer mountTimer;

   @EventHandler
   private void onRotationRequest(RotationRequestEvent event) {
      if (ClientAccess.schizoMinecraft.player.isPassenger()) {
         return;
      }
      ElapsedTimer timerSnapshot = this.mountTimer;
      float configuredDelaySnapshot = this.delaySetting.getValue();

      float minimumDelay = 0.1F;

      double minimumElapsedDelay = Math.max(configuredDelaySnapshot, minimumDelay);
      TimeUnit delayUnit = TimeUnit.SECONDS;

      if (!timerSnapshot.hasElapsed(minimumElapsedDelay, delayUnit)) {
         return;
      }
      if (ClientAccess.schizoMinecraft.player.isShiftKeyDown()) {
         return;
      }

      ClientLevel level = ClientAccess.schizoMinecraft.level;
      for (Entity entity : level.entitiesForRendering()) {
         double distance = ClientAccess.schizoMinecraft.player.distanceTo(entity);
         Double range = this.rangeSetting.getValue();
         if (distance > range) {
            continue;
         }
         if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
               continue;
            }
         }

         if (entity instanceof Mob mob) {
            if (!mob.isSaddled() && !(entity instanceof AbstractHorse)) {
               continue;
            }
         }

         if (!(entity instanceof PlayerRideable || entity instanceof VehicleEntity || entity instanceof Strider)) {
            continue;
         }
         InteractionHand interactionHand = InteractionHand.MAIN_HAND;

         BlockInteractionUtils.interactWithEntity(entity, interactionHand);
         if (this.rotateSetting.getValue()) {
            RotationManager rotationManagerSnapshot = ClientContext.rotationManager;

            Vec3 boundingBoxCenter = entity.getBoundingBox().getCenter();

            float[] rotations = RotationManager.getRotationsToPosition(
               boundingBoxCenter);

            rotationManagerSnapshot.requestRotation(rotations, 5);
         }

         timerSnapshot = this.mountTimer;

         timerSnapshot.reset();
         break;

      }

   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (!(event.getPacket() instanceof ClientboundSetPassengersPacket passengersPacket)) {
         return;
      }

      int packetVehicleId = passengersPacket.getVehicle();
      int[] passengerIds = passengersPacket.getPassengers().clone();
      ClientAccess.schizoMinecraft.executeIfPossible(() -> {
         if (!this.isToggled()
            || !this.delaySetting.isAtMinimum()
            || ClientAccess.schizoMinecraft.player == null) {
            return;
         }

         LocalPlayer player = ClientAccess.schizoMinecraft.player;
         Entity vehicle = player.getVehicle();
         if (vehicle == null || packetVehicleId != vehicle.getId()) {
            return;
         }

         for (int passengerId : passengerIds) {
            if (passengerId == player.getId()) {
               return;
            }
         }
         this.mountTimer.clear();
      });
   }

   public AutoMountModule() {
      super("AutoMount", "Mounts rideable entities automatically.", ModuleCategory.PLAYER, new String[0]);
      this.rangeSetting = this.add(new NumberSetting<>("Range", 3.0, 0.1, 6.0).withUnit("m"));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 1.0F, 0.0F, 5.0F).withUnit("s"));
      Boolean defaultRotate = true;
      this.rotateSetting = this.add(new BooleanSetting("Rotate", defaultRotate));
      this.mountTimer = new ElapsedTimer();
   }

}
