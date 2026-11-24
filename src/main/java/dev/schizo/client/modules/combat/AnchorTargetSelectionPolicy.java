/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

/**
 * Pure target validity and damage-threshold rules for AnchorAura.
 */
public final class AnchorTargetSelectionPolicy {
   private AnchorTargetSelectionPolicy() {
   }

   public record TargetState(
      boolean localPlayer,
      boolean alive,
      boolean friend,
      boolean serverSideDead,
      boolean spectator,
      boolean creative,
      double distanceToLocalPlayer
   ) {
   }

   public static boolean isInvalidTarget(TargetState target, double range) {
      if (target == null
         || !Double.isFinite(range)
         || range < 0.0D
         || !Double.isFinite(target.distanceToLocalPlayer())
         || target.distanceToLocalPlayer() < 0.0D) {
         return true;
      }

      return target.localPlayer()
         || !target.alive()
         || target.friend()
         || target.serverSideDead()
         || target.spectator()
         || target.creative()
         || target.distanceToLocalPlayer() > range;
   }

   /**
    * AnchorAura accepts damage at either threshold exactly. Non-finite values
    * are always rejected rather than slipping through inverted comparisons.
    */
   public static boolean acceptsDamage(double damage, double minimumDamage) {
      return Double.isFinite(damage)
         && damage >= 0.0D
         && Double.isFinite(minimumDamage)
         && minimumDamage >= 0.0D
         && damage >= minimumDamage
         && damage >= 0.5D;
   }
}
