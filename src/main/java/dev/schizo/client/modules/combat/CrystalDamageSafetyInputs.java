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

      if (shouldRejectAntiSuicide(
         selfDamage,
         this.selfHealthWithAbsorption,
         this.antiSuicide,
         this.targetDamageIsKill,
         this.selfHoldingTotem
      )) {
         return true;
      }

      if (shouldAcceptHitIntersection(this.hit, this.targetDamageIsIntersection)
         || this.targetDamageIsKill) {
         return false;
      }

      if (shouldRejectPlaceRatio(
         this.hit,
         selfDamage,
         targetDamage,
         this.minimumTargetToSelfDamageRatio
      )) {
         return true;
      }

      return shouldRejectNonLethalThresholds(
         selfDamage,
         targetDamage,
         this.targetHealthWithAbsorption,
         this.minimumTargetDamage,
         this.maximumSelfDamage
      );
   }

   public static boolean shouldRejectAntiSuicide(
      double selfDamage,
      double selfHealthWithAbsorption,
      boolean antiSuicide,
      boolean targetDamageIsKill,
      boolean selfHoldingTotem
   ) {
      return selfDamage >= selfHealthWithAbsorption
         && antiSuicide
         && (!targetDamageIsKill || !selfHoldingTotem);
   }

   public static boolean shouldAcceptHitIntersection(
      boolean hit,
      boolean targetDamageIsIntersection
   ) {
      return hit && targetDamageIsIntersection;
   }

   public static boolean shouldRejectPlaceRatio(
      boolean hit,
      double selfDamage,
      double targetDamage,
      double minimumTargetToSelfDamageRatio
   ) {
      return !hit
         && selfDamage > 0.0D
         && targetDamage / selfDamage < minimumTargetToSelfDamageRatio;
   }

   public static boolean shouldRejectNonLethalThresholds(
      double selfDamage,
      double targetDamage,
      double targetHealthWithAbsorption,
      double minimumTargetDamage,
      double maximumSelfDamage
   ) {
      if (!(targetDamage >= targetHealthWithAbsorption)) {
         if (!(targetDamage >= minimumTargetDamage)) {
            return true;
         }
         return !(selfDamage <= maximumSelfDamage);
      }
      return false;
   }
}
