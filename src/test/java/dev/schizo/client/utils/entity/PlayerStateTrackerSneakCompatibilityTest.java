package dev.schizo.client.utils.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerStateTrackerSneakCompatibilityTest {
   @Test
   void immediatelyTracksLegacyPlayerCommandSneakChanges() {
      assertTrue(PlayerStateTracker.shouldUpdateTrackedSneakingImmediately(true, false));
   }

   @Test
   void leavesModernPlayerInputStateToPacketTracking() {
      assertFalse(PlayerStateTracker.shouldUpdateTrackedSneakingImmediately(false, false));
   }

   @Test
   void doesNotTrackCancelledSneakChanges() {
      assertFalse(PlayerStateTracker.shouldUpdateTrackedSneakingImmediately(true, true));
      assertFalse(PlayerStateTracker.shouldUpdateTrackedSneakingImmediately(false, true));
   }
}
