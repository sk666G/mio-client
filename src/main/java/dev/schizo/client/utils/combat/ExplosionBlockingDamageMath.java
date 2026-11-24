/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import java.util.List;

/**
 * Pure calculation for item-component blocking reductions against an
 * explosion. Damage-type eligibility is captured by the caller.
 */
public final class ExplosionBlockingDamageMath {
   private ExplosionBlockingDamageMath() {
   }

   public record Reduction(
      float horizontalBlockingAngle,
      boolean damageTypeMatches,
      float base,
      float factor
   ) {
   }

   public static float apply(float damage, double angle, List<Reduction> reductions) {
      float blockedDamage = 0.0F;

      for (Reduction reduction : reductions) {
         float maximumAngle = (float)(Math.PI / 180.0D) * reduction.horizontalBlockingAngle();
         if (angle > maximumAngle || !reduction.damageTypeMatches()) {
            continue;
         }

         float ruleDamage = reduction.base() + reduction.factor() * damage;
         blockedDamage += clamp(ruleDamage, 0.0F, damage);
      }

      return clamp(blockedDamage, 0.0F, damage);
   }

   private static float clamp(float value, float minimum, float maximum) {
      if (value < minimum) {
         return minimum;
      }
      if (value > maximum) {
         return maximum;
      }
      return value;
   }
}
