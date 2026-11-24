package dev.schizo.client.modules.combat;

final class PusherTargetPolicy {
   private PusherTargetPolicy() {
   }

   static boolean isEligible(
      boolean alive,
      boolean localPlayer,
      boolean spectator,
      boolean creative,
      boolean friend,
      boolean serverSideDead
   ) {
      return alive && !localPlayer && !spectator && !creative && !friend && !serverSideDead;
   }
}
