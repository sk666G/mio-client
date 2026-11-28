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

   @Test void visitedSettingsReservesBothFooterLayoutsAndStatus() {
      for (int width : new int[]{180,235,236,320,375,376,480,825}) {
         for (int height=120; height<=500; height++) {
            var l=StudioViewportLayouts.visited(width,height);
            if (l.compact()) {
               assertEquals(0,l.rows());
               continue;
            }
            assertTrue(l.left()>=0 && l.left()+l.width()<=width);
            assertTrue(l.rows()>=1 && l.rows()<=10);
            assertTrue(l.rowsY()>=l.searchY()+20);
            assertEquals(l.rowsY()+l.rows()*13,l.footerY());
            assertTrue(l.statusY()>=l.footerY()+(l.stacked()?43:20));
            assertTrue(l.panelBottom()<=height-3);
            int count=l.stacked()?3:5;
            int buttonWidth=(l.width()-(count-1)*4)/count;
            assertTrue(buttonWidth>=68);
            assertTrue(buttonWidth*count+(count-1)*4<=l.width());
         }
      }
      assertTrue(StudioViewportLayouts.visited(236,150).stacked());
      assertFalse(StudioViewportLayouts.visited(376,150).stacked());
      assertTrue(StudioViewportLayouts.visited(235,150).compact());
      assertTrue(StudioViewportLayouts.visited(376,149).compact());
   }

   @Test void customPaletteKeepsFieldsPickersSwatchesAndFooterInsideViewport() {
      for (int width : new int[]{180,195,196,255,256,335,336,460,825}) {
         for (int height=120; height<=500; height++) {
            var l=StudioViewportLayouts.customPalette(width,height);
            if (l.compact()) continue;
            assertTrue(l.fieldWidth()>=65);
            assertTrue(l.panelBottom()<=height);
            for (int i=0;i<6;i++) {
               assertTrue(l.fieldX(i)>=l.left());
               assertTrue(l.fieldY(i)>=l.top()+37);
               assertEquals(l.fieldX(i)+l.fieldWidth()+4,l.pickerX(i));
               assertEquals(l.pickerX(i)+34,l.swatchX(i));
               assertTrue(l.swatchX(i)+14<=l.left()+l.width());
               assertTrue(l.fieldY(i)+18<=l.footerY());
               if (i>=l.columns()) assertTrue(l.fieldY(i)>=l.fieldY(i-l.columns())+33);
            }
            assertTrue(l.footerY()+20<=l.panelBottom());
            assertTrue(2*((l.width()-6)/2)+6<=l.width());
         }
      }
      assertTrue(StudioViewportLayouts.customPalette(196,269).compact());
      assertFalse(StudioViewportLayouts.customPalette(196,270).compact());
      assertEquals(1,StudioViewportLayouts.customPalette(320,270).columns());
      assertEquals(2,StudioViewportLayouts.customPalette(336,270).columns());
      assertEquals(2,StudioViewportLayouts.customPalette(256,170).columns());
   }

   @Test void savedThemesCondensesWithoutHidingActionsOrStatus() {
      for (int width : new int[]{180,255,256,320,480,825}) {
         for (int height=120; height<=500; height++) {
            var l=StudioViewportLayouts.savedThemes(width,height);
            if (l.compact()) continue;
            assertTrue(l.left()>=0 && l.left()+l.width()<=width);
            assertTrue(l.navigationY()>=l.top()+26);
            assertTrue(l.nameY()>=l.navigationY()+20);
            assertTrue(l.actionsY()>=l.nameY()+20);
            assertTrue(l.secondaryY()>=l.actionsY()+20);
            assertTrue(l.noticeY()>=l.secondaryY()+20);
            assertTrue(l.noteY()>=l.noticeY()+10);
            assertTrue(l.panelBottom()>=l.noteY()+10);
            assertTrue(l.panelBottom()<=height-4);
            int buttonWidth=(l.width()-8)/3;
            assertTrue(buttonWidth>=77);
            assertTrue(3*buttonWidth+8<=l.width());
         }
      }
      assertTrue(StudioViewportLayouts.savedThemes(256,151).compact());
      assertFalse(StudioViewportLayouts.savedThemes(256,152).compact());
      assertTrue(StudioViewportLayouts.savedThemes(256,185).condensed());
      assertFalse(StudioViewportLayouts.savedThemes(256,186).condensed());
      assertTrue(StudioViewportLayouts.savedThemes(255,500).compact());
   }

   @Test void pickerKeepsSampleRailsAndActionsInsideCompactViewport() {
      for (int width : new int[]{180,233,234,320,340,825}) {
         for (int height=120;height<=500;height++) {
            var l=StudioViewportLayouts.picker(width,height);
            if(l.compact()) continue;
            assertTrue(l.left()>=0 && l.left()+l.width()<=width);
            assertTrue(l.sampleY()+l.sampleHeight()<=l.rowY(0));
            assertTrue(l.railWidth()>=102);
            assertTrue(l.railLeft()+l.railWidth()<=l.left()+l.width()-50);
            for(int i=1;i<4;i++) assertTrue(l.rowY(i)>=l.rowY(i-1)+18);
            assertTrue(l.rowY(3)+18<=l.footerY());
            assertTrue(l.footerY()+18<=l.panelBottom());
            assertTrue(l.panelBottom()<=height-4);
         }
      }
      assertTrue(StudioViewportLayouts.picker(234,157).compact());
      assertFalse(StudioViewportLayouts.picker(234,158).compact());
      assertTrue(StudioViewportLayouts.picker(234,201).condensed());
      assertFalse(StudioViewportLayouts.picker(234,202).condensed());
   }
}
