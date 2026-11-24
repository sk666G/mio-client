/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

/**
 * Pure explosion-impact damage math.
 *
 * <p>This class deliberately accepts only primitive snapshot values. It must
 * not acquire world, entity, player, or module state; callers are responsible
 * for capturing exposure and difficulty on the client thread.</p>
 */
public final class ExplosionDamageMath {
   private ExplosionDamageMath() {
   }

   public enum Difficulty {
      PEACEFUL,
      EASY,
      NORMAL,
      HARD
   }

   /**
    * Calculates the vanilla-style explosion impact and difficulty scaling
    * before armor, effects, enchantments, or blocking are applied.
    */
   public static float calculate(double normalizedDistance, float exposure, double power, Difficulty difficulty) {
      if (normalizedDistance > 1.0D) {
         return 0.0F;
      }

      double diameter = power * 2.0D;
      double impact = (1.0D - normalizedDistance) * exposure;
      double quadraticImpact = impact * impact + impact;
      double scaledImpact = quadraticImpact / 2.0D;
      double rawDamage = scaledImpact * 7.0D * diameter;
      float damage = (float)(rawDamage + 1.0D);

      return switch (difficulty) {
         case PEACEFUL -> 0.0F;
         case EASY -> Math.min(damage / 2.0F + 1.0F, damage);
         case HARD -> damage * 1.5F;
         case NORMAL -> damage;
      };
   }
}
