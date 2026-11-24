package dev.schizo.client.integrations.baritone;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.NamedFeature;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BaritonePathOwnershipTest {
   @Test
   void recognizesTheOwnerWhileTheExpectedGoalIsActive() {
      BaritonePathOwnership ownership = new BaritonePathOwnership();
      NamedFeature owner = new NamedFeature("ObstaclePasser");
      BlockPos goal = new BlockPos(10, 64, 20);

      ownership.acquire(owner, goal);

      assertTrue(ownership.owns(owner, goal, true));
   }

   @Test
   void differentFeatureCannotClaimOrInvalidateThePath() {
      BaritonePathOwnership ownership = new BaritonePathOwnership();
      NamedFeature owner = new NamedFeature("ObstaclePasser");
      NamedFeature other = new NamedFeature("Other");
      BlockPos goal = new BlockPos(10, 64, 20);

      ownership.acquire(owner, goal);

      assertFalse(ownership.owns(other, goal, true));
      assertTrue(ownership.owns(owner, goal, true));
   }

   @Test
   void replacingTheGoalInvalidatesStaleOwnership() {
      BaritonePathOwnership ownership = new BaritonePathOwnership();
      NamedFeature owner = new NamedFeature("ObstaclePasser");
      BlockPos original = new BlockPos(10, 64, 20);

      ownership.acquire(owner, original);

      assertFalse(ownership.owns(owner, new BlockPos(11, 64, 20), true));
      assertFalse(ownership.owns(owner, original, true));
   }

   @Test
   void completedPathInvalidatesStaleOwnership() {
      BaritonePathOwnership ownership = new BaritonePathOwnership();
      NamedFeature owner = new NamedFeature("ObstaclePasser");
      BlockPos goal = new BlockPos(10, 64, 20);

      ownership.acquire(owner, goal);

      assertFalse(ownership.owns(owner, goal, false));
      assertFalse(ownership.owns(owner, goal, true));
   }
}
