package dev.schizo.client.compatibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MultiplayerCompatibilityPolicyTest {
   @Test
   void conservativeMultiplayerBlocksRiskyAndUnknownClasses() {
      for (CompatibilityClass classification : new CompatibilityClass[] {
         CompatibilityClass.PACKET_AFFECTING,
         CompatibilityClass.SERVER_SENSITIVE,
         CompatibilityClass.UNVERIFIED
      }) {
         assertFalse(
            MultiplayerCompatibilityPolicy.isActivationAllowed(true, true, classification),
            classification.name()
         );
      }
   }

   @Test
   void conservativeMultiplayerAllowsReviewedNonPacketClassesAndSingleplayer() {
      for (CompatibilityClass classification : new CompatibilityClass[] {
         CompatibilityClass.CLIENT_ONLY,
         CompatibilityClass.VISUAL_ONLY,
         CompatibilityClass.INPUT_MODIFYING,
         CompatibilityClass.WORLD_INTERACTION
      }) {
         assertTrue(
            MultiplayerCompatibilityPolicy.isActivationAllowed(true, true, classification),
            classification.name()
         );
      }
      assertTrue(MultiplayerCompatibilityPolicy.isActivationAllowed(true, false, CompatibilityClass.UNVERIFIED));
      assertTrue(MultiplayerCompatibilityPolicy.isActivationAllowed(false, true, CompatibilityClass.UNVERIFIED));
   }
}
