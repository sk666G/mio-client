package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class AutoCrystalPlacementGuardTest {
   @Test
   void allowsFirstStrictInstantCrystalWhenNoPreviousPlacementExists() {
      assertFalse(AutoCrystalModule.isSamePreviousPlacement(null, new BlockPos(1, 2, 3)));
   }

   @Test
   void rejectsOnlyThePreviouslyAttackedCrystalPosition() {
      BlockPos previous = new BlockPos(1, 2, 3);
      assertTrue(AutoCrystalModule.isSamePreviousPlacement(previous, new BlockPos(1, 2, 3)));
      assertFalse(AutoCrystalModule.isSamePreviousPlacement(previous, new BlockPos(1, 2, 4)));
   }

   @Test
   void handlesNullCandidatePosition() {
      assertFalse(AutoCrystalModule.isSamePreviousPlacement(new BlockPos(1, 2, 3), null));
   }
}
