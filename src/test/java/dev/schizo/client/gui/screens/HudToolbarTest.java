package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudToolbarTest {
   @Test void everyButtonUsesSameDrawAndHitBoundsAcrossLayouts() {
      for (int width : new int[]{240, 320, 640, 1920}) {
         var layout = HudToolbar.layout(width, 400, true);
         assertTrue(layout.visible());
         assertTrue(layout.x() >= 0 && layout.y() >= 0);
         for (var action : HudToolbar.ACTIONS) {
            var cell = layout.cell(action);
            assertEquals(action, layout.hit(cell.x() + 1, cell.y() + 1));
            assertNull(layout.hit(cell.x() + cell.width(), cell.y() + 1));
            assertTrue(cell.x() + cell.width() <= width);
            assertTrue(cell.y() + cell.height() <= 400);
         }
         assertNull(layout.hit(layout.x() + 1, layout.y() + 1));
      }
   }

   @Test void hidesWhenRequestedOrViewportIsTooSmall() {
      assertFalse(HudToolbar.layout(800,600,false).visible());
      assertFalse(HudToolbar.layout(120,100,true).visible());
      assertNull(HudToolbar.layout(120,100,true).hit(0,0));
   }

   @Test void actionsRequireSelectionAndAvailableHistory() {
      assertTrue(HudToolbar.Action.GRID.enabled(0,false,false));
      assertFalse(HudToolbar.Action.UNDO.enabled(3,false,true));
      assertTrue(HudToolbar.Action.REDO.enabled(0,false,true));
      assertFalse(HudToolbar.Action.LEFT.enabled(1,true,true));
      assertTrue(HudToolbar.Action.CENTER_Y.enabled(2,false,false));
      assertFalse(HudToolbar.Action.DISTRIBUTE_X.enabled(2,true,true));
      assertTrue(HudToolbar.Action.DISTRIBUTE_Y.enabled(3,false,false));
      assertFalse(HudToolbar.Action.BACKGROUND.enabled(3,true,true));
   }
}
