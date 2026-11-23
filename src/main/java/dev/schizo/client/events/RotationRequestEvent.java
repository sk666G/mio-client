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

public final class RotationRequestEvent extends CancellableEvent implements ClientAccess {

   public float getPlayerPitch() {
      return ClientAccess.schizoMinecraft.player.getXRot();
   }

   public float getPlayerYaw() {
      return ClientAccess.schizoMinecraft.player.getYRot();
   }
}
