/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.movement;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.LivingEntityRotationEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.FireworkRocketEntityTickEvent;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.modules.movement.ElytraFlyModule;

public abstract class ElytraFlyModeHandler implements ClientAccess {
   protected final ElytraFlyModule module;

   protected ElytraFlyModeHandler(ElytraFlyModule module) {
      this.module = module;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void handleFireworkTick(FireworkRocketEntityTickEvent event) {
   }

   public void handlePrePlayerTick(LocalPlayerPreTickEvent event) {
   }

   public void handleOutgoingPacket(
      PacketEvent.Send event
   ) {
   }

   public void handlePlayerTick(LocalPlayerTickEvent event) {
   }

   public void handleMovement(LocalPlayerMoveEvent event) {
   }

   public void handleTravelRotation(LivingEntityRotationEvent event) {
   }

   public void handleClientTick(MinecraftRunTickEvent event) {
   }

   public void handleRotationManagerEvent(RotationRequestEvent event) {
   }

   public void handleIncomingPacket(
      PacketEvent.Receive event
   ) {
   }

   public void handleSendPosition(LocalPlayerSendPositionEvent event) {
   }
}
