package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioViewportLayoutsTest {
   @Test void inspectorReservesAllControlsBeforeAllocatingRows() {
      for(int width:new int[]{180,236,320,550,825}) for(int height=120;height<=500;height++) {
         var l=StudioViewportLayouts.inspector(width,height);
         if(l.compact()) continue;
         assertTrue(l.left()>=0 && l.left()+l.width()<=width);
         assertTrue(l.rows()>=1 && l.rows()<=9);
         assertTrue(l.rowsY()+l.rows()*13<=l.controlsY());
         assertTrue(l.controlsY()+68<=height-4);
      }
      assertTrue(StudioViewportLayouts.inspector(320,180).condensed());
      assertTrue(StudioViewportLayouts.inspector(320,160).compact());
   }

   @Test void themeReservesFooterAndDiagnosticsWithoutOverlap() {
      for(int width:new int[]{180,256,320,520,825}) for(int height=120;height<=500;height++) {
         var l=StudioViewportLayouts.theme(width,height);
         if(l.compact()) continue;
         assertTrue(l.left()>=0 && l.left()+l.width()<=width);
         assertTrue(l.previewHeight()>=1);
         assertTrue(l.previewTop()+l.previewHeight()+27<=l.bottom());
         assertTrue(l.bottom()+20<=height-5);
         assertTrue(l.previewTop()>=l.top()+(l.appliedControls()?118:95));
      }
      assertFalse(StudioViewportLayouts.theme(320,180).appliedControls());
      assertTrue(StudioViewportLayouts.theme(320,250).appliedControls());
   }

   @Test void quickAccessReservesSearchRowsAndFooterAtBothTabLayouts() {
      for (int width : new int[]{180,255,256,320,405,406,490,825}) {
         for (int height=120; height<=500; height++) {
            var l=StudioViewportLayouts.quickAccess(width,height);
            if (l.compact()) {
               assertEquals(0,l.rows());
               continue;
            }
            assertTrue(l.left()>=0 && l.left()+l.width()<=width);
            assertTrue(l.rows()>=1 && l.rows()<=10);
            assertTrue(l.searchY()>=l.top()+(l.stacked()?67:44));
            assertTrue(l.rowsY()>=l.searchY()+20);
            assertEquals(l.rowsY()+l.rows()*13,l.footerY());
            assertTrue(l.footerY()+56<=height-4);
            int tabWidth=l.stacked()?(l.width()-8)/3:(l.width()-16)/5;
            assertTrue(tabWidth>=74);
         }
      }
      assertTrue(StudioViewportLayouts.quickAccess(256,180).stacked());
      assertFalse(StudioViewportLayouts.quickAccess(406,180).stacked());
      assertTrue(StudioViewportLayouts.quickAccess(255,180).compact());
      assertTrue(StudioViewportLayouts.quickAccess(406,179).compact());
   }
}
