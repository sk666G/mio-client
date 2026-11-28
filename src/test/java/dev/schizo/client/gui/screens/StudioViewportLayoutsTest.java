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
}
