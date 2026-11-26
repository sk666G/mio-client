package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PistonCrystalPlannerTest {
   @Test
   void prefersCloserPlanCandidates() {
      assertTrue(PistonCrystalPlanner.isCloserCandidate(2.0D, 4.0D));
      assertFalse(PistonCrystalPlanner.isCloserCandidate(4.0D, 2.0D));
   }

   @Test
   void rejectsEqualDistanceAsAnImprovement() {
      assertFalse(PistonCrystalPlanner.isCloserCandidate(3.0D, 3.0D));
   }
}
