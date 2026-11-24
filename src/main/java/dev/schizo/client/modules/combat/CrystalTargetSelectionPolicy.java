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
 * Pure target and candidate filtering for AutoCrystal damage selection.
 *
 * <p>Callers capture all entity, social, setting, and geometry values before
 * invoking this policy. No Minecraft object is retained or read here.</p>
 */
public final class CrystalTargetSelectionPolicy {
   private CrystalTargetSelectionPolicy() {
   }

   public enum Decision {
      ACCEPT,
      REJECT,
      ABORT_SCAN
   }

   public record TargetState(
      boolean localPlayer,
      boolean player,
      boolean friendPlayer,
      boolean creative,
      boolean alive,
      boolean deadOrDying,
      boolean serverSideDead,
      boolean spectator,
      double distanceToLocalPlayer
   ) {
   }

   public record CandidateState(
      double baseY,
      double targetY,
      boolean hasCrystalBase,
      boolean ignoreNakeds,
      boolean targetHasArmor,
      boolean targetHoldingCrystal,
      double distanceToBase,
      double crystalRange,
      boolean lethal,
      boolean obsidianDropNearTarget,
      boolean quiverThreatActive,
      boolean friendEntity,
      boolean antiFriendKill,
      double damage,
      double targetHealthWithAbsorption,
      double minimumDamage
   ) {
   }

   /**
    * Mirrors AutoCrystalModule.isInvalidTarget, including the force-suicide
    * special case which deliberately bypasses the ordinary target checks.
    */
   public static boolean isInvalidTarget(
      TargetState target,
      boolean forceSuicide,
      double targetRange
   ) {
      if (target == null) {
         return true;
      }
      if (forceSuicide) {
         return !target.localPlayer();
      }
      if (!Double.isFinite(targetRange) || targetRange < 0.0D
         || !Double.isFinite(target.distanceToLocalPlayer())
         || target.distanceToLocalPlayer() < 0.0D) {
         return true;
      }

      if (target.player() && (target.friendPlayer() || target.creative())) {
         return true;
      }
      if (!target.alive() || target.deadOrDying()) {
         return true;
      }
      if (!(target.distanceToLocalPlayer() <= targetRange)
         || target.serverSideDead()
         || target.localPlayer()) {
         return true;
      }

      return target.spectator();
   }

   /**
    * Evaluates the remaining per-base filters after target eligibility and
    * damage calculation. ABORT_SCAN preserves AntiFriendKill's existing
    * whole-pass abort when a friend would be killed.
    */
   public static Decision evaluateCandidate(CandidateState candidate) {
      if (candidate == null
         || !Double.isFinite(candidate.baseY())
         || !Double.isFinite(candidate.targetY())
         || !Double.isFinite(candidate.distanceToBase())
         || candidate.distanceToBase() < 0.0D
         || !Double.isFinite(candidate.crystalRange())
         || candidate.crystalRange() < 0.0D
         || !Double.isFinite(candidate.damage())
         || candidate.damage() < 0.0D
         || !Double.isFinite(candidate.minimumDamage())
         || candidate.minimumDamage() < 0.0D
         || (candidate.friendEntity()
            && candidate.antiFriendKill()
            && (!Double.isFinite(candidate.targetHealthWithAbsorption())
               || candidate.targetHealthWithAbsorption() < 0.0D))) {
         return Decision.REJECT;
      }

      if (candidate.baseY() > candidate.targetY() + 1.5D && !candidate.hasCrystalBase()) {
         return Decision.REJECT;
      }

      if (candidate.ignoreNakeds()
         && !candidate.targetHasArmor()
         && !candidate.targetHoldingCrystal()) {
         return Decision.REJECT;
      }

      if (candidate.distanceToBase() > candidate.crystalRange()
         && !candidate.lethal()
         && !candidate.obsidianDropNearTarget()
         && !candidate.quiverThreatActive()) {
         return Decision.REJECT;
      }

      if (candidate.friendEntity()) {
         if (candidate.antiFriendKill() && candidate.damage() > candidate.targetHealthWithAbsorption()) {
            return Decision.ABORT_SCAN;
         }
         return Decision.REJECT;
      }

      if ((candidate.minimumDamage() > candidate.damage() || candidate.damage() < 0.5D)
         && !candidate.lethal()) {
         return Decision.REJECT;
      }

      return Decision.ACCEPT;
   }

   /**
    * AutoCrystal keeps the earlier target on equal damage; only a strict
    * improvement replaces the current best candidate.
    */
   public static boolean shouldReplaceBest(double candidateDamage, double bestDamage) {
      return Double.isFinite(candidateDamage)
         && candidateDamage >= 0.0D
         && (!Double.isFinite(bestDamage) || candidateDamage > bestDamage);
   }
}
