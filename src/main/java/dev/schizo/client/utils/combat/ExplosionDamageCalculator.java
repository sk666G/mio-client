/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import java.util.Objects;

/**
 * Complete worker-side explosion damage calculation over immutable profiles
 * and a captured ray snapshot.
 */
public final class ExplosionDamageCalculator {
   private ExplosionDamageCalculator() {
   }

   public enum Status {
      COMPLETE,
      STALE_SNAPSHOT,
      INCOMPLETE_SNAPSHOT
   }

   public record Result(
      Status status,
      int targetEntityId,
      String targetUuid,
      float damage,
      float exposure
   ) {
      public Result {
         Objects.requireNonNull(status, "status");
         Objects.requireNonNull(targetUuid, "targetUuid");
      }

      public boolean complete() {
         return this.status == Status.COMPLETE;
      }
   }

   public static Result calculate(
      ExplosionDamageProfile profile,
      ExplosionRaycastSnapshot raySnapshot,
      ExplosionRaycastSnapshot.Query rayQuery,
      double explosionX,
      double explosionY,
      double explosionZ,
      double power
   ) {
      Objects.requireNonNull(profile, "profile");
      Objects.requireNonNull(raySnapshot, "raySnapshot");
      Objects.requireNonNull(rayQuery, "rayQuery");

      if (profile.worldGeneration() != raySnapshot.worldGeneration()
         || profile.captureTick() != raySnapshot.captureTick()
         || !profile.dimensionId().equals(raySnapshot.dimensionId())) {
         return result(Status.STALE_SNAPSHOT, profile, 0.0F, 0.0F);
      }

      if (profile.creativeSelfDamageImmune()) {
         return result(Status.COMPLETE, profile, 0.0F, 0.0F);
      }

      double bottomCenterX = profile.minX() + (profile.maxX() - profile.minX()) * 0.5D;
      double bottomCenterY = profile.minY();
      double bottomCenterZ = profile.minZ() + (profile.maxZ() - profile.minZ()) * 0.5D;
      double distanceX = bottomCenterX - explosionX;
      double distanceY = bottomCenterY - explosionY;
      double distanceZ = bottomCenterZ - explosionZ;
      double distance = Math.sqrt(
         distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ
      );
      double diameter = power * 2.0D;
      double normalizedDistance = distance / diameter;
      if (normalizedDistance > 1.0D) {
         return result(Status.COMPLETE, profile, 0.0F, 0.0F);
      }

      ExplosionExposureMath.ExposureResult exposureResult = ExplosionExposureMath.calculateFromSnapshot(
         profile.minX(),
         profile.minY(),
         profile.minZ(),
         profile.maxX(),
         profile.maxY(),
         profile.maxZ(),
         explosionX,
         explosionY,
         explosionZ,
         raySnapshot,
         rayQuery
      );
      if (!exposureResult.complete()) {
         return result(Status.INCOMPLETE_SNAPSHOT, profile, 0.0F, 0.0F);
      }

      float damage = ExplosionDamageMath.calculate(
         normalizedDistance,
         exposureResult.exposure(),
         power,
         profile.difficulty()
      );
      damage = ExplosionDamageReductionMath.apply(
         damage,
         profile.armor(),
         profile.toughness(),
         profile.hasResistance(),
         profile.resistanceAmplifier(),
         profile.assumeBestArmor(),
         profile.protectionScore()
      );

      if (profile.hasBlockingItem()) {
         double attackX = explosionX - profile.targetX();
         double attackZ = explosionZ - profile.targetZ();
         double horizontalLength = Math.sqrt(attackX * attackX + attackZ * attackZ);
         double directionX = 0.0D;
         double directionZ = 0.0D;
         if (horizontalLength >= 1.0E-4D) {
            directionX = attackX / horizontalLength;
            directionZ = attackZ / horizontalLength;
         }
         double dot = directionX * profile.viewX() + directionZ * profile.viewZ();
         double angle = Math.acos(dot);
         damage = ExplosionBlockingDamageMath.apply(
            damage,
            angle,
            profile.blockingReductions()
         );
      }

      return result(Status.COMPLETE, profile, damage, exposureResult.exposure());
   }

   private static Result result(
      Status status,
      ExplosionDamageProfile profile,
      float damage,
      float exposure
   ) {
      return new Result(status, profile.targetEntityId(), profile.targetUuid(), damage, exposure);
   }
}
