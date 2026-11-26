/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations.xaero;

import dev.schizo.client.integrations.IntegrationHealth;

import net.minecraft.core.BlockPos;

public interface WaypointAdapter {
   void removeWaypoints(String name);

   boolean isAvailable();

   default IntegrationHealth health() {
      return this.isAvailable() ? IntegrationHealth.AVAILABLE : IntegrationHealth.UNAVAILABLE;
   }

   void addWaypoint(String name, BlockPos position);
}
