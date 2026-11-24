/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.entity;

import dev.schizo.client.integrations.ProtocolCompatibility;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.math.MathUtils;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.AbstractClientPlayerTickEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.SneakStateChangeEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import java.util.function.Consumer;
import dev.schizo.client.events.bus.EventBus;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public final class PlayerStateTracker implements ClientAccess {
   private double packetX;
   private double packetY;
   private double packetZ;
   private boolean packetOnGround;
   private boolean sneaking;
   private Input lastSentInput;
   private double lastGroundedY;
   private double currentTickY;
   private double previousTickY;
   private int groundedTicks;
   private int airborneTicks;
   private float fallDistance;
   private float[] packetRotations;
   private Vec3 tickDisplacement;
   private final ElapsedTimer fallFlyingTimer;

   public boolean isPacketOnGround() {
      return this.packetOnGround;
   }

   public double getPreviousTickY() {
      return this.previousTickY;
   }

   public float[] getPacketRotations() {
      return this.packetRotations;
   }

   public double getLastGroundedY() {
      return this.lastGroundedY;
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   public Input getLastSentInput() {
      return this.lastSentInput;
   }

   public ElapsedTimer getFallFlyingTimer() {
      return this.fallFlyingTimer;
   }

   public double getCurrentTickY() {
      return this.currentTickY;
   }

   public Vec3 getTickDisplacement() {
      return this.tickDisplacement != null ? this.tickDisplacement : ClientAccess.schizoMinecraft.player.getDeltaMovement();
   }

   public Vec3 getPacketPosition() {
      return new Vec3(
         this.packetX,
         this.packetY,
         this.packetZ
      );
   }

   public int getGroundedTicks() {
      return this.groundedTicks;
   }

   public float getFallDistance() {
      return this.fallDistance;
   }

   public int getAirborneTicks() {
      return this.airborneTicks;
   }

   public PlayerStateTracker() {
      this.packetRotations = new float[2];
      this.fallFlyingTimer = new ElapsedTimer();
      ClientAccess.eventBus.subscribe(this);
   }

   public void setFallDistance(float distance) {
      this.fallDistance = distance;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {

      if (PlayerStateUtils.isFallFlying()) {
         this.fallFlyingTimer.reset();
      }

      this.previousTickY = this.currentTickY;
      LocalPlayer heightPlayer = ClientAccess.schizoMinecraft.player;
      this.currentTickY = heightPlayer.getY();
      this.tickDisplacement = MathUtils.getPositionDelta(heightPlayer);
      LocalPlayer flightPlayer = ClientAccess.schizoMinecraft.player;

      flightPlayer.isFallFlying();
      LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
      if (!groundPlayer.onGround() && !this.packetOnGround) {
         this.groundedTicks = 0;
         this.airborneTicks++;
      } else {
         this.airborneTicks = 0;
         this.groundedTicks++;
         LocalPlayer groundedHeightPlayer = ClientAccess.schizoMinecraft.player;
         this.lastGroundedY = groundedHeightPlayer.getY();
      }
   }

   public void runSneaking(Runnable action) {
      runWithSneakingState(true, ClientContext.playerStateTracker.isSneaking(), this::setSneaking, action);
   }

   static void runWithSneakingState(
      boolean requestedSneaking,
      boolean previousSneaking,
      Consumer<Boolean> setSneaking,
      Runnable action
   ) {
      if (previousSneaking == requestedSneaking) {
         action.run();
         return;
      }

      try {
         setSneaking.accept(requestedSneaking);
         action.run();
      } finally {
         setSneaking.accept(previousSneaking);
      }
   }

   public void setSneaking(boolean requestedSneaking) {
      EventBus eventBus = ClientAccess.eventBus;
      SneakStateChangeEvent event = new SneakStateChangeEvent(requestedSneaking);
      SneakStateChangeEvent postedEvent = (SneakStateChangeEvent)eventBus.post(event);
      boolean eventCancelled = postedEvent.isCancelled();
      if (!eventCancelled) {
         boolean legacyTarget = ProtocolCompatibility.getTargetVersion().olderEq_1_21_5();
         ProtocolCompatibility.setSneaking(requestedSneaking);
         if (shouldUpdateTrackedSneakingImmediately(legacyTarget, eventCancelled)) {
            this.sneaking = requestedSneaking;
         }
      }
   }

   static boolean shouldUpdateTrackedSneakingImmediately(boolean legacyTarget, boolean eventCancelled) {
      return legacyTarget && !eventCancelled;
   }

   public void runWithoutSneaking(Runnable action) {
      runWithSneakingState(false, ClientContext.playerStateTracker.isSneaking(), this::setSneaking, action);
   }

   @EventHandler(priority = -9999)
   private void onConnectionDoSendPacketEvent(
      PacketEvent.PostSend event
   ) {
      if (event.isCancelled()) {
         return;
      }
      if (event.getPacket() instanceof ServerboundMovePlayerPacket movementPacket) {
         this.packetOnGround = movementPacket.isOnGround();
         if (movementPacket.hasPosition()) {
            this.packetX = movementPacket.getX(0.0);
            this.packetY = movementPacket.getY(0.0);
            this.packetZ = movementPacket.getZ(0.0);
         }

         if (movementPacket.hasRotation()) {

            float[] rotations = new float[2];
            rotations[0] = movementPacket.getYRot(0.0F);
            rotations[1] = movementPacket.getXRot(0.0F);
            this.packetRotations = rotations;
         }
      }

      if (event.getPacket() instanceof ServerboundPlayerInputPacket inputPacket) {
         Input decodedInput;
         try {
            decodedInput = inputPacket.input();
         } catch (Throwable failure) {
            throw new MatchException(failure.toString(), failure);
         }

         this.sneaking = decodedInput.shift();
         this.lastSentInput = decodedInput;
      }

   }

   @EventHandler
   private void onAbstractClientPlayerTickEvent(AbstractClientPlayerTickEvent event) {
      double previousY = ClientAccess.schizoMinecraft.player.yo;
      LocalPlayer heightPlayer = ClientAccess.schizoMinecraft.player;
      double downwardDistance = previousY - heightPlayer.getY();
      LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
      if (!groundPlayer.onGround()) {
         LocalPlayer liquidPlayer = ClientAccess.schizoMinecraft.player;
         if (!PlayerStateUtils.isInLiquid(liquidPlayer)) {
            if (downwardDistance > 0.0) {
               this.fallDistance += (float)downwardDistance;
            }

            return;
         }
      }

      this.fallDistance = 0.0F;
   }

}
