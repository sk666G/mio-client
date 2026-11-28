package dev.schizo.client.modules.combat;

final class AntiPhaseTargetPolicy {
   private AntiPhaseTargetPolicy() {
   }

   static boolean isEligible(
      boolean alive,
      boolean localPlayer,
      boolean swimming,
      boolean spectator,
      boolean creative,
      boolean friend,
      boolean serverSideDead
   ) {
      return alive && !localPlayer && !swimming && !spectator && !creative && !friend && !serverSideDead;
   }
}
