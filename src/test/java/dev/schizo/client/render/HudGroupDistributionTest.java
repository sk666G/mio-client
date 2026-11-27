package dev.schizo.client.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGroupDistributionTest {
   @Test void equalHorizontalGapsRespectDifferentWidthsAndInputOrder() {
      var bounds = List.of(new HudGroupMove.Bounds(80, 30, 20, 10),
         new HudGroupMove.Bounds(10, 12, 10, 20), new HudGroupMove.Bounds(25, 50, 30, 10));
      assertEquals(List.of(new HudGroupAlignment.Position(80,30), new HudGroupAlignment.Position(10,12),
         new HudGroupAlignment.Position(35,50)),
         HudGroupAlignment.plan(bounds,120,100,HudGroupAlignment.Mode.DISTRIBUTE_X));
   }

   @Test void equalVerticalGapsPreserveHorizontalCoordinates() {
      var bounds = List.of(new HudGroupMove.Bounds(10, 5, 20, 10),
         new HudGroupMove.Bounds(40, 30, 15, 20), new HudGroupMove.Bounds(70, 85, 20, 10));
      assertEquals(List.of(new HudGroupAlignment.Position(10,5), new HudGroupAlignment.Position(40,40),
         new HudGroupAlignment.Position(70,85)),
         HudGroupAlignment.plan(bounds,100,100,HudGroupAlignment.Mode.DISTRIBUTE_Y));
   }

   @Test void rejectsOverfullEnvelopeAndInsufficientSelection() {
      var bounds = List.of(new HudGroupMove.Bounds(0,0,40,10), new HudGroupMove.Bounds(10,20,40,10),
         new HudGroupMove.Bounds(20,40,40,10));
      assertTrue(HudGroupAlignment.plan(bounds,100,100,HudGroupAlignment.Mode.DISTRIBUTE_X).isEmpty());
      assertTrue(HudGroupAlignment.plan(bounds.subList(0,2),100,100,HudGroupAlignment.Mode.DISTRIBUTE_X).isEmpty());
   }

   @Test void stableTiesAndZeroGapAreSupported() {
      var bounds = List.of(new HudGroupMove.Bounds(0,0,10,10), new HudGroupMove.Bounds(0,20,10,10),
         new HudGroupMove.Bounds(20,40,10,10));
      assertEquals(List.of(new HudGroupAlignment.Position(0,0),new HudGroupAlignment.Position(10,20),
         new HudGroupAlignment.Position(20,40)),
         HudGroupAlignment.plan(bounds,100,100,HudGroupAlignment.Mode.DISTRIBUTE_X));
   }
}
