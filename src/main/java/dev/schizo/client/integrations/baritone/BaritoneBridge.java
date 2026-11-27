/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations.baritone;

import dev.schizo.client.NamedFeature;
import dev.schizo.client.integrations.IntegrationHealth;

import net.minecraft.core.BlockPos;

public interface BaritoneBridge {
   void pathElytraTo(BlockPos position);

   void setGoal(BlockPos position);

   boolean isPathingOrCalculating();

   boolean isInSelection(BlockPos position);

   boolean isPauseRequested();

   BlockPos getGoalPosition();

   void pathTo(BlockPos position);

   void pathTo(NamedFeature owner, BlockPos position);

   boolean ownsPath(NamedFeature owner);

   void cancelPathing();

   boolean cancelPathing(NamedFeature owner);

   boolean isAvailable();

   default IntegrationHealth health() {
      return this.isAvailable() ? IntegrationHealth.AVAILABLE : IntegrationHealth.UNAVAILABLE;
   }

   void requestPause(NamedFeature feature);

   void releasePause(NamedFeature feature);

   void pathForward();
}
