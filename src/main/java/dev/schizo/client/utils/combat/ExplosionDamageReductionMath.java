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
 * Pure post-impact explosion damage reduction math.
 *
 * <p>All arguments are captured values. This class must not read Minecraft
 * entities, items, effects, damage sources, modules, or world state.</p>
 */
public final class ExplosionDamageReductionMath {
   private ExplosionDamageReductionMath() {
   }

   /**
    * Applies the client-side armor, resistance, and protection stages in the
    * same order as the existing DamageUtils path.
    *
    * <p>For the current client-side explosion calculation, CombatRules uses
    * its ordinary armor formula: the weapon-effectiveness override is only
    * evaluated for a ServerLevel with a non-null weapon stack.</p>
    */
   public static float apply(
      float damage,
      float armor,
      float toughness,
      boolean hasResistance,
      float resistanceAmplifier,
      boolean assumeBestArmor,
      int protectionScore
   ) {
      float toughnessScale = 2.0F + toughness / 4.0F;
      float effectiveArmor = clamp(
         armor - damage / toughnessScale,
         armor * 0.2F,
         20.0F
      );
      float armorFraction = effectiveArmor / 25.0F;
      damage = damage * (1.0F - armorFraction);

      if (hasResistance || assumeBestArmor) {
         float resistanceLevel = resistanceAmplifier + 1.0F;
         float resistanceNumerator = damage * (25.0F - resistanceLevel * 5.0F);
         damage = resistanceNumerator / 25.0F;
      }

      if (protectionScore > 20) {
         protectionScore = 20;
      }

      float protectionAsFloat = protectionScore;
      damage *= 1.0F - protectionAsFloat / 25.0F;
      return damage < 0.0F ? 0.0F : damage;
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
