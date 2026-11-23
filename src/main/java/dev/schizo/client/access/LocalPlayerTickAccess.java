/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.access;

public interface LocalPlayerTickAccess {
   void sendMovementPacketsWrapper();

   void superTick();

   void resetEvent();

   void schizo$updateServer(float yaw, float pitch);

   void resetRotations();
}
