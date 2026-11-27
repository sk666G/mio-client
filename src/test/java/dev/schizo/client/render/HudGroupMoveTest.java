package dev.schizo.client.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGroupMoveTest {
   @Test void commonTranslationPreservesSpacingAndStopsAtEdge() {
      var group = List.of(new HudGroupMove.Bounds(10, 20, 20, 10), new HudGroupMove.Bounds(75, 50, 20, 20));
      assertEquals(new HudGroupMove.Delta(5, 0), HudGroupMove.clamp(group, 100, 100, 10, 0));
      assertEquals(new HudGroupMove.Delta(-10, -20), HudGroupMove.clamp(group, 100, 100, -40, -40));
   }

   @Test void oversizedOrInvalidGeometryCannotForceUnexpectedMovement() {
      var group = List.of(new HudGroupMove.Bounds(0, 5, 120, 10));
      assertEquals(new HudGroupMove.Delta(0, 2), HudGroupMove.clamp(group, 100, 100, 5, 2));
      assertEquals(new HudGroupMove.Delta(0, 0), HudGroupMove.clamp(List.of(), 100, 100, 5, 2));
      assertEquals(new HudGroupMove.Delta(0, 0), HudGroupMove.clamp(group, 100, 100, Float.NaN, 2));
   }
}
