package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.schizo.client.utils.world.WorldUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VoidFloorScanCursorTest {
   @Test
   void matchesTheExistingSphereFilterAtTheFloorSlice() {
      assertMatches(new Vec3(0.2D, 5.8D, -0.4D), 0, 8.0F);
      assertMatches(new Vec3(12.9D, -60.1D, 3.1D), -64, 16.0F);
      assertMatches(new Vec3(-4.2D, 80.0D, 9.7D), 0, 10.0F);
   }

   @Test
   void returnsNoCandidatesWhenTheFloorIsOutsideTheSphere() {
      assertMatches(new Vec3(0.0D, 100.0D, 0.0D), -64, 16.0F);
   }

   private static void assertMatches(Vec3 center, int floorY, float radius) {
      List<BlockPos> expected = WorldUtils.streamSphereBlocks(center, radius)
         .filter(position -> position.getY() == floorY)
         .toList();

      List<BlockPos> actual = new ArrayList<>();
      Iterator<BlockPos> cursor = VoidFloorScanCursor.iterator(center, floorY, radius);
      cursor.forEachRemaining(actual::add);

      assertEquals(expected, actual);
   }
}
