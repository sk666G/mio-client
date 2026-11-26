package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VoidFloorScanCursorTest {
   @Test
   void matchesThePreviousSphereFilterAtTheFloorSlice() {
      assertMatches(new Vec3(0.2D, 5.8D, -0.4D), 0, 8.0F);
      assertMatches(new Vec3(12.9D, -60.1D, 3.1D), -64, 16.0F);
      assertMatches(new Vec3(-4.2D, 80.0D, 9.7D), 0, 10.0F);
   }

   @Test
   void returnsNoCandidatesWhenTheFloorIsOutsideTheSphere() {
      assertMatches(new Vec3(0.0D, 100.0D, 0.0D), -64, 16.0F);
   }

   private static void assertMatches(Vec3 center, int floorY, float radius) {
      List<BlockPos> expected = collectPreviousSphereFloor(center, floorY, radius);

      List<BlockPos> actual = new ArrayList<>();
      Iterator<BlockPos> cursor = VoidFloorScanCursor.iterator(center, floorY, radius);
      cursor.forEachRemaining(actual::add);

      assertEquals(expected, actual);
   }

   private static List<BlockPos> collectPreviousSphereFloor(Vec3 center, int floorY, float radius) {
      BlockPos blockCenter = BlockPos.containing(center);
      int extent = (int)Math.ceil(radius);
      List<BlockPos> positions = new ArrayList<>();

      for (int offsetX = -extent; offsetX <= extent; offsetX++) {
         for (int offsetY = -extent; offsetY <= extent; offsetY++) {
            for (int offsetZ = -extent; offsetZ <= extent; offsetZ++) {
               BlockPos position = blockCenter.offset(offsetX, offsetY, offsetZ);
               if (blockCenter.closerThan(position, radius) && position.getY() == floorY) {
                  positions.add(position);
               }
            }
         }
      }

      return positions;
   }
}
