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
import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Tracks provenance for paths started through Schizo's owner-aware Baritone API. */
final class BaritonePathOwnership {
   private NamedFeature owner;
   private BlockPos goal;

   void acquire(NamedFeature owner, BlockPos goal) {
      this.owner = Objects.requireNonNull(owner, "owner");
      this.goal = Objects.requireNonNull(goal, "goal").immutable();
   }

   boolean owns(NamedFeature owner, BlockPos activeGoal, boolean active) {
      if (!Objects.equals(this.owner, owner)) {
         return false;
      }

      boolean matches = active && Objects.equals(this.goal, activeGoal);
      if (!matches) {
         this.clear();
      }
      return matches;
   }

   void clear() {
      this.owner = null;
      this.goal = null;
   }
}
