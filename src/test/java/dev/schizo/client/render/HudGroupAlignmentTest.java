package dev.schizo.client.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGroupAlignmentTest {
   private static final List<HudGroupMove.Bounds> GROUP = List.of(
      new HudGroupMove.Bounds(10, 20, 20, 10), new HudGroupMove.Bounds(60, 50, 30, 20));

   @Test void alignsEdgesWithinSelectionEnvelope() {
      assertEquals(List.of(new HudGroupAlignment.Position(10,20), new HudGroupAlignment.Position(10,50)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.LEFT));
      assertEquals(List.of(new HudGroupAlignment.Position(70,20), new HudGroupAlignment.Position(60,50)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.RIGHT));
      assertEquals(List.of(new HudGroupAlignment.Position(10,20), new HudGroupAlignment.Position(60,20)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.TOP));
      assertEquals(List.of(new HudGroupAlignment.Position(10,60), new HudGroupAlignment.Position(60,50)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.BOTTOM));
   }

   @Test void centersDifferentSizesWithoutMovingOtherAxis() {
      assertEquals(List.of(new HudGroupAlignment.Position(40,20), new HudGroupAlignment.Position(35,50)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.CENTER_X));
      assertEquals(List.of(new HudGroupAlignment.Position(10,40), new HudGroupAlignment.Position(60,35)),
         HudGroupAlignment.plan(GROUP,100,100,HudGroupAlignment.Mode.CENTER_Y));
   }

   @Test void rejectsInvalidOrOversizedGeometryAsAWhole() {
      assertTrue(HudGroupAlignment.plan(GROUP,50,100,HudGroupAlignment.Mode.LEFT).isEmpty());
      assertTrue(HudGroupAlignment.plan(List.of(GROUP.getFirst()),100,100,HudGroupAlignment.Mode.LEFT).isEmpty());
      assertTrue(HudGroupAlignment.plan(List.of(GROUP.getFirst(),new HudGroupMove.Bounds(Float.NaN,0,10,10)),
         100,100,HudGroupAlignment.Mode.TOP).isEmpty());
   }
}
