package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class StashFinderModuleTest {
   @Test
   void twoIntersectingMinecartsCountAsTwoStackParticipants() {
      AABB first = new AABB(0, 0, 0, 1, 1, 1);
      AABB second = new AABB(0.5, 0, 0.5, 1.5, 1, 1.5);

      assertEquals(2, StashFinderModule.countStackedMinecartBounds(List.of(first, second)));
   }

   @Test
   void isolatedMinecartsDoNotCountAsStackParticipants() {
      AABB first = new AABB(0, 0, 0, 1, 1, 1);
      AABB second = new AABB(2, 0, 2, 3, 1, 3);

      assertEquals(0, StashFinderModule.countStackedMinecartBounds(List.of(first, second)));
   }

   @Test
   void overlappingClusterCountsEachMinecartOnce() {
      AABB first = new AABB(0, 0, 0, 1, 1, 1);
      AABB second = new AABB(0.5, 0, 0.5, 1.5, 1, 1.5);
      AABB third = new AABB(1.0, 0, 1.0, 2.0, 1, 2.0);

      assertEquals(3, StashFinderModule.countStackedMinecartBounds(List.of(first, second, third)));
   }

   @Test
   void singleMinecartIsNotAStack() {
      assertEquals(
         0,
         StashFinderModule.countStackedMinecartBounds(List.of(new AABB(0, 0, 0, 1, 1, 1)))
      );
   }
}
