package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PistonCrystalModulePolicyTest {
   @Test
   void acceptsEitherSupportedPowerSource() {
      assertTrue(PistonCrystalPlanner.hasPowerSource(true, false));
      assertTrue(PistonCrystalPlanner.hasPowerSource(false, true));
      assertTrue(PistonCrystalPlanner.hasPowerSource(true, true));
   }

   @Test
   void rejectsMissingPowerSources() {
      assertFalse(PistonCrystalPlanner.hasPowerSource(false, false));
   }
}
