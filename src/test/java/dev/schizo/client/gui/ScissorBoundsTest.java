package dev.schizo.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScissorBoundsTest {
   @Test
   void clipsOverlappingRectangles() {
      assertEquals(new ScissorBounds(5, 6, 5, 4),
         new ScissorBounds(0, 0, 10, 10).intersect(new ScissorBounds(5, 6, 10, 10)));
   }

   @Test
   void preservesContainedRectangles() {
      ScissorBounds inner = new ScissorBounds(2, 3, 4, 5);
      assertEquals(inner, new ScissorBounds(0, 0, 10, 10).intersect(inner));
   }

   @Test
   void disjointAndTouchingRectanglesHaveZeroExtent() {
      assertEquals(0, new ScissorBounds(0, 0, 10, 10).intersect(new ScissorBounds(20, 0, 5, 5)).width());
      assertEquals(0, new ScissorBounds(0, 0, 10, 10).intersect(new ScissorBounds(0, 10, 5, 5)).height());
   }

   @Test
   void normalizesNegativeExtentsAndPreservesEmptyIntersections() {
      ScissorBounds empty = new ScissorBounds(3, 3, -1, -2);
      assertEquals(new ScissorBounds(3, 3, 0, 0), empty);
      assertEquals(empty, empty.intersect(new ScissorBounds(0, 0, 10, 10)));
   }

   @Test
   void calculatesEdgesWithoutIntegerOverflow() {
      ScissorBounds first = new ScissorBounds(Integer.MAX_VALUE - 5, 0, 10, 10);
      ScissorBounds second = new ScissorBounds(Integer.MAX_VALUE - 2, 0, 10, 10);
      assertEquals(new ScissorBounds(Integer.MAX_VALUE - 2, 0, 7, 10), first.intersect(second));
   }
}
