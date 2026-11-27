package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StudioLayoutTest {
   @Test void centeredWorkspaceDoesNotOverflowNormalOrSmallViewport() {
      for (int[] size : new int[][]{{825, 464}, {320, 250}, {1920, 1080}}) {
         StudioLayout l = StudioLayout.fit(size[0], size[1], 8);
         assertTrue(l.x() >= 0 && l.y() >= 0);
         assertTrue(l.right() <= size[0] && l.bottom() <= size[1]);
         assertTrue(l.contentX() + l.contentWidth() < l.right());
         assertTrue(l.contentY() + l.contentHeight() < l.bottom());
         assertTrue(l.navY(7) + l.navRowHeight() < l.bottom() - 14);
         assertEquals(7, l.navIndex(l.navY(7) + 0.5));
         assertEquals(-1, l.navIndex(l.navY(0) - 0.1));
         assertEquals(-1, l.navIndex(l.navY(8)));
         assertTrue(l.contentY() + l.contentHeight() < l.toolsY());
         assertTrue(l.toolX(3) + l.toolWidth() <= l.contentX() + l.contentWidth());
         for (int i = 0; i < 4; i++) {
            assertEquals(i, l.toolIndex(l.toolX(i) + 0.5, l.toolsY() + 0.5));
         }
         assertEquals(-1, l.toolIndex(l.toolX(0) + l.toolWidth() + 0.5, l.toolsY() + 1));
         assertEquals(-1, l.toolIndex(l.toolX(0), l.toolsY() - 1));
      }
   }
}
