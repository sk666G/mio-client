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
 * Captured inputs for AutoCrystal's self-damage safety decision.
 *
 * <p>This value is safe to pass to a worker. It contains no entity, module,
 * inventory, setting or Minecraft references.</p>
 */
public record CrystalDamageSafetyInputs(
   boolean hit,
   boolean selfInvulnerable,
   boolean forceSuicide,
   boolean antiSuicide,
   boolean selfHoldingTotem,
   boolean targetDamageIsKill,
   boolean targetDamageIsIntersection,
   double selfHealthWithAbsorption,
   double targetHealthWithAbsorption,
   double minimumTargetDamage,
   double maximumSelfDamage,
   double minimumTargetToSelfDamageRatio
) {
   public boolean shouldReject(double selfDamage, double targetDamage) {
      if (this.selfInvulnerable) {
         selfDamage = 0.0D;
      }

      if (this.forceSuicide) {
         return false;
      }

      if (selfDamage >= this.selfHealthWithAbsorption
         && this.antiSuicide
         && (!this.targetDamageIsKill || !this.selfHoldingTotem)) {
         return true;
      }

      if (this.hit && this.targetDamageIsIntersection) {
         return false;
      }

      if (this.targetDamageIsKill) {
         return false;
      }

      if (!this.hit && selfDamage > 0.0D) {
         double damageRatio = targetDamage / selfDamage;
         if (damageRatio < this.minimumTargetToSelfDamageRatio) {
            return true;
         }
      }

      if (!(targetDamage >= this.targetHealthWithAbsorption)) {
         if (!(targetDamage >= this.minimumTargetDamage)) {
            return true;
         }

         if (!(selfDamage <= this.maximumSelfDamage)) {
            return true;
         }
      }

      return false;
   }
}
