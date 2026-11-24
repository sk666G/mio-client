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
 * Immutable inputs and pure threshold arithmetic for AutoCrystal target damage.
 *
 * <p>The caller is responsible for capturing health, face-place conditions,
 * portal state, settings and mining state on the client thread.</p>
 */
public record CrystalDamageThresholds(
   boolean obsidianDropNearTarget,
   boolean quiverThreatActive,
   boolean facePlaceOverride,
   boolean targetInNetherPortal,
   double targetHealthWithAbsorption,
   double placeMinimumDamage,
   double hitMinimumDamage,
   boolean placeMinimumUsesTargetHealth,
   boolean ignoreMiningBlock,
   boolean alwaysIgnoreMining
) {
   public double minimumDamage(boolean hit) {
      if (this.obsidianDropNearTarget || this.quiverThreatActive) {
         return 0.1D;
      }

      if (this.facePlaceOverride || this.targetInNetherPortal) {
         return 0.6D;
      }

      double placeMinimum = this.placeMinimumUsesTargetHealth
         ? this.targetHealthWithAbsorption
         : this.placeMinimumDamage;
      double hitMinimum = this.hitMinimumDamage;

      if (this.ignoreMiningBlock) {
         double miningMinimum = this.alwaysIgnoreMining ? 0.6D : 5.0D;
         placeMinimum = Math.min(placeMinimum, miningMinimum);
         hitMinimum = 3.0D;
      }

      double selectedMinimum = hit ? hitMinimum : placeMinimum;
      return Math.min(selectedMinimum, this.targetHealthWithAbsorption);
   }
}
