package dev.schizo.client.modules.combat;

final class HoleFillTargetPolicy {
   private HoleFillTargetPolicy() {
   }

   static boolean isRelevant(
      boolean alive,
      boolean deadOrDying,
      boolean localPlayer,
      boolean spectator,
      boolean creative,
      boolean friend,
      boolean serverSideDead
   ) {
      return alive && !deadOrDying && !localPlayer && !spectator && !creative && !friend && !serverSideDead;
   }
}
