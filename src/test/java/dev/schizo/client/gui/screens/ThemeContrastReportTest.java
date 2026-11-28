package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeContrastReportTest {
   @Test void advisoryThresholdsDistinguishTextAndIndicators() {
      var text=new ThemeContrastReport.Pair("text",0xff777777,0xff000000,4.5);
      var indicator=new ThemeContrastReport.Pair("indicator",0xff666666,0xff000000,3);
      assertTrue(text.meetsTarget());
      assertTrue(indicator.meetsTarget());
      assertFalse(new ThemeContrastReport.Pair("text",0xff666666,0xff000000,4.5).meetsTarget());
      assertEquals(21,new ThemeContrastReport.Pair("white",0xffffffff,0xff000000,4.5).ratio(),1e-9);
   }

   @Test void translucentSamplesReflectReferenceBackdropButChromeDoesNot() {
      var transparent=new Color(255,0,0,0);
      var palette=new UiPalette(transparent,transparent,transparent,transparent,transparent,transparent);
      var dark=ThemeContrastReport.preview(palette,false);
      var light=ThemeContrastReport.preview(palette,true);
      assertEquals(7,dark.size());
      assertEquals(7,ThemeContrastReport.chrome(palette).size());
      assertNotEquals(dark.getFirst().background(),light.getFirst().background());
      assertTrue(dark.getFirst().meetsTarget());
      assertFalse(light.getFirst().meetsTarget());
      assertEquals(0,palette.background().getAlpha());
      assertEquals(3,ThemeContrastReport.chrome(palette).getLast().target(),0);
   }
}
