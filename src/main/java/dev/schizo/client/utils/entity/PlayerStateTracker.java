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
import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.util.function.Consumer;
import dev.schizo.client.events.bus.EventBus;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public final class PlayerStateTracker implements ClientAccess {
   private final SessionState sessionState = new SessionState();
   private final ElapsedTimer fallFlyingTimer;

   public boolean isPacketOnGround() {
      return this.sessionState.packetOnGround;
   }

   public double getPreviousTickY() {
      return this.sessionState.previousTickY;
   }

   public float[] getPacketRotations() {
      return this.sessionState.packetRotations;
   }

   public double getLastGroundedY() {
      return this.sessionState.lastGroundedY;
   }

   public boolean isSneaking() {
      return this.sessionState.sneaking;
   }

   public Input getLastSentInput() {
      return this.sessionState.lastSentInput;
   }

   public ElapsedTimer getFallFlyingTimer() {
      return this.fallFlyingTimer;
   }

   public double getCurrentTickY() {
      return this.sessionState.currentTickY;
   }

   public Vec3 getTickDisplacement() {
      if (this.sessionState.tickDisplacement != null) {
         return this.sessionState.tickDisplacement;
      }
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      return player != null ? player.getDeltaMovement() : Vec3.ZERO;
   }

   public Vec3 getPacketPosition() {
      return new Vec3(
         this.sessionState.packetX,
         this.sessionState.packetY,
         this.sessionState.packetZ
      );
   }

   public int getGroundedTicks() {
      return this.sessionState.groundedTicks;
   }

   public float getFallDistance() {
      return this.sessionState.fallDistance;
   }

   public int getAirborneTicks() {
      return this.sessionState.airborneTicks;
   }

   public PlayerStateTracker() {
      this.fallFlyingTimer = new ElapsedTimer();
      ClientAccess.eventBus.subscribe(this);
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.sessionState.reset();
      this.fallFlyingTimer.clear();
   }

   public void setFallDistance(float distance) {
      this.sessionState.fallDistance = distance;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {

      if (PlayerStateUtils.isFallFlying()) {
         this.fallFlyingTimer.reset();
      }

      this.sessionState.previousTickY = this.sessionState.currentTickY;
      LocalPlayer heightPlayer = ClientAccess.schizoMinecraft.player;
      this.sessionState.currentTickY = heightPlayer.getY();
      this.sessionState.tickDisplacement = MathUtils.getPositionDelta(heightPlayer);
      LocalPlayer flightPlayer = ClientAccess.schizoMinecraft.player;

      flightPlayer.isFallFlying();
      LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
      if (!groundPlayer.onGround() && !this.sessionState.packetOnGround) {
         this.sessionState.groundedTicks = 0;
         this.sessionState.airborneTicks++;
      } else {
         this.sessionState.airborneTicks = 0;
         this.sessionState.groundedTicks++;
         LocalPlayer groundedHeightPlayer = ClientAccess.schizoMinecraft.player;
         this.sessionState.lastGroundedY = groundedHeightPlayer.getY();
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
            this.sessionState.sneaking = requestedSneaking;
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
         this.sessionState.packetOnGround = movementPacket.isOnGround();
         if (movementPacket.hasPosition()) {
            this.sessionState.packetX = movementPacket.getX(0.0);
            this.sessionState.packetY = movementPacket.getY(0.0);
            this.sessionState.packetZ = movementPacket.getZ(0.0);
         }

         if (movementPacket.hasRotation()) {

            float[] rotations = new float[2];
            rotations[0] = movementPacket.getYRot(0.0F);
            rotations[1] = movementPacket.getXRot(0.0F);
            this.sessionState.packetRotations = rotations;
         }
      }

      if (event.getPacket() instanceof ServerboundPlayerInputPacket inputPacket) {
         Input decodedInput;
         try {
            decodedInput = inputPacket.input();
         } catch (Throwable failure) {
            throw new MatchException(failure.toString(), failure);
         }

         this.sessionState.sneaking = decodedInput.shift();
         this.sessionState.lastSentInput = decodedInput;
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
               this.sessionState.fallDistance += (float)downwardDistance;
            }

            return;
         }
      }

      this.sessionState.fallDistance = 0.0F;
   }

   static final class SessionState {
      double packetX;
      double packetY;
      double packetZ;
      boolean packetOnGround;
      boolean sneaking;
      Input lastSentInput;
      double lastGroundedY;
      double currentTickY;
      double previousTickY;
      int groundedTicks;
      int airborneTicks;
      float fallDistance;
      float[] packetRotations = new float[2];
      Vec3 tickDisplacement;

      void reset() {
         this.packetX = 0.0;
         this.packetY = 0.0;
         this.packetZ = 0.0;
         this.packetOnGround = false;
         this.sneaking = false;
         this.lastSentInput = null;
         this.lastGroundedY = 0.0;
         this.currentTickY = 0.0;
         this.previousTickY = 0.0;
         this.groundedTicks = 0;
         this.airborneTicks = 0;
         this.fallDistance = 0.0F;
         this.packetRotations = new float[2];
         this.tickDisplacement = null;
      }
   }

}
