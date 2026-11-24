package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BedPlacementScannerPolicyTest {
   @Test
   void rejectsDamageAboveTheConfiguredMaximum() {
      assertTrue(BedPlacementScanner.shouldRejectSelfDamage(10.1F, 10.0F, 20.0F, false));
   }

   @Test
   void antiSuicideRejectsLethalDamageEvenBelowTheConfiguredMaximum() {
      assertTrue(BedPlacementScanner.shouldRejectSelfDamage(8.0F, 10.0F, 8.0F, true));
   }

   @Test
   void allowsNonLethalDamageWhenWithinTheConfiguredMaximum() {
      assertFalse(BedPlacementScanner.shouldRejectSelfDamage(7.9F, 10.0F, 8.0F, true));
   }

   @Test
   void disablingAntiSuicideDoesNotBypassTheMaximumDamageSetting() {
      assertFalse(BedPlacementScanner.shouldRejectSelfDamage(8.0F, 10.0F, 8.0F, false));
      assertTrue(BedPlacementScanner.shouldRejectSelfDamage(10.1F, 10.0F, 8.0F, false));
   }
}
