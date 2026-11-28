package dev.schizo.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorGridTest {
   @Test void cyclesOptionalGridModes() {
      assertEquals(8, HudEditorGrid.next(0));
      assertEquals(16, HudEditorGrid.next(8));
      assertEquals(32, HudEditorGrid.next(16));
      assertEquals(0, HudEditorGrid.next(32));
   }

   @Test void snapsAndClampsWidgetExtent() {
      assertEquals(16F, HudEditorGrid.snap(13, 100, 20, 8));
      assertEquals(0F, HudEditorGrid.snap(-15, 100, 20, 8));
      assertEquals(77F, HudEditorGrid.snap(99, 100, 23, 8));
      assertEquals(0F, HudEditorGrid.snap(30, 100, 120, 8));
      assertEquals(13F, HudEditorGrid.snap(13, 100, 20, 0));
      assertEquals(0F, HudEditorGrid.snap(Float.NaN, 100, 20, 8));
   }

   @Test void limitsGuideDensityWithoutChangingSnapStep() {
      assertEquals(8, HudEditorGrid.visualStep(8, 800));
      assertEquals(32, HudEditorGrid.visualStep(8, 4000));
      assertEquals(0, HudEditorGrid.visualStep(0, 800));
   }
}
