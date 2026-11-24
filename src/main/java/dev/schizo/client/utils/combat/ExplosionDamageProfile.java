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
import java.util.Objects;

/**
 * Immutable, data-only damage inputs captured for one target on the client
 * thread. No live entity, item stack, effect instance, registry holder, or
 * world object is retained.
 */
public record ExplosionDamageProfile(
   int targetEntityId,
   String targetUuid,
   long worldGeneration,
   long captureTick,
   String dimensionId,
   boolean creativeSelfDamageImmune,
   float healthWithAbsorption,
   double minX,
   double minY,
   double minZ,
   double maxX,
   double maxY,
   double maxZ,
   double targetX,
   double targetZ,
   double viewX,
   double viewZ,
   float armor,
   float toughness,
   boolean hasResistance,
   float resistanceAmplifier,
   boolean assumeBestArmor,
   int protectionScore,
   boolean hasBlockingItem,
   List<ExplosionBlockingDamageMath.Reduction> blockingReductions,
   ExplosionDamageMath.Difficulty difficulty
) {
   public ExplosionDamageProfile {
      Objects.requireNonNull(targetUuid, "targetUuid");
      Objects.requireNonNull(dimensionId, "dimensionId");
      blockingReductions = List.copyOf(Objects.requireNonNull(blockingReductions, "blockingReductions"));
      Objects.requireNonNull(difficulty, "difficulty");

      if (minX > maxX || minY > maxY || minZ > maxZ) {
         throw new IllegalArgumentException("Target bounds minimum exceeds maximum");
      }
   }
}
