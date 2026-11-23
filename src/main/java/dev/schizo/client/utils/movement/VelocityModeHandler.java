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
import dev.schizo.client.events.ClientPacketListenerHandleExplosionEvent;
import dev.schizo.client.events.ClientPacketListenerHandleSetEntityMotionEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.FishingHookPullEntityEvent;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.events.MultiPlayerGameModeUseItemOnEvent;
import dev.schizo.client.modules.movement.VelocityModule;

public abstract class VelocityModeHandler implements ClientAccess {
   protected final VelocityModule velocityModule;

   public VelocityModeHandler(VelocityModule velocityModule) {
      this.velocityModule = velocityModule;
   }

   public void onDisable() {
   }

   public void onExplosion(
      ClientPacketListenerHandleExplosionEvent event
   ) {
   }

   public void onEntityMotion(
      ClientPacketListenerHandleSetEntityMotionEvent event
   ) {
   }

   public void onSendPacket(
      PacketEvent.Send event
   ) {
   }

   public void onUseItemOn(MultiPlayerGameModeUseItemOnEvent event) {
   }

   public void onFishingPull(FishingHookPullEntityEvent event) {
   }

   public void onSendPosition(LocalPlayerSendPositionEvent event) {
   }

   public void onReceivePacket(
      PacketEvent.Receive event
   ) {
   }

   public void onTick() {
   }
}
