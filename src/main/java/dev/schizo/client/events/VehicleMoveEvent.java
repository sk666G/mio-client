/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.events;

import dev.schizo.client.ClientAccess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VehicleMoveEvent extends LocalPlayerMoveEvent {

   public VehicleMoveEvent(Vec3 velocity) {
      super(velocity, null);
   }

   public static boolean isLocalPlayerVehicle(Object candidate) {
      return ClientAccess.schizoMinecraft.player == null
         ? false
         : candidate instanceof Entity
            && ClientAccess.schizoMinecraft.player.getVehicle()
               == candidate;
   }
}
