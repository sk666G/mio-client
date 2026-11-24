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

   void requestPause(NamedFeature feature);

   void releasePause(NamedFeature feature);

   void pathForward();
}
