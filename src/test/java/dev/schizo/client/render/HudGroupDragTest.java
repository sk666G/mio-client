package dev.schizo.client.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGroupDragTest {
   private final List<HudGroupMove.Bounds> group = List.of(
      new HudGroupMove.Bounds(10, 20, 20, 10), new HudGroupMove.Bounds(60, 40, 20, 10));

   @Test void snapsSharedDisplacementAndClampsAtBoundary() {
      assertEquals(new HudGroupMove.Delta(16, -8), HudGroupDrag.plan(group,100,100,13,-9,8));
      assertEquals(new HudGroupMove.Delta(20, 0), HudGroupDrag.plan(group,100,100,200,0,8));
      assertEquals(new HudGroupMove.Delta(0, 0), HudGroupDrag.plan(group,100,100,3,-3,8));
      assertEquals(new HudGroupMove.Delta(3,-3), HudGroupDrag.plan(group,100,100,3,-3,0));
   }

   @Test void rejectsInvalidOrNoLongerFittingGroup() {
      assertNull(HudGroupDrag.plan(group,70,100,10,0,0));
      assertNull(HudGroupDrag.plan(group,100,100,Double.NaN,0,0));
      assertNull(HudGroupDrag.plan(group.subList(0,1),100,100,10,0,0));
   }
}
