package dev.schizo.client.gui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteHsvTest {
   @Test void convertsWithoutLosingAlphaOrOriginalRgb() {
      for(int r=0;r<256;r+=17) for(int g=0;g<256;g+=17) for(int b=0;b<256;b+=17) {
         Color color=new Color(r,g,b,73);
         assertEquals(color,PaletteHsv.from(color).color());
      }
   }

   @Test void retainsHueAtZeroSaturationAndValue() {
      PaletteHsv state=PaletteHsv.from(Color.BLUE).with(1,0).with(2,0);
      assertEquals(240,state.get(0));
      assertEquals(Color.BLACK,state.color());
      state=state.with(1,100).with(2,100);
      assertEquals(Color.BLUE,state.color());
      assertEquals(new Color(0,0,255,42),state.with(3,42).color());
   }

   @Test void railsReachEndpointsAndRetainAlphaColor() {
      PaletteHsv state=PaletteHsv.from(new Color(12,34,56,78));
      assertEquals(Color.RED.getRGB(),state.gradientArgb(0,0));
      assertEquals(Color.RED.getRGB(),state.gradientArgb(0,360));
      assertEquals(0x000c2238,state.gradientArgb(3,0));
      assertEquals(0xff0c2238,state.gradientArgb(3,255));
      assertEquals(360,PaletteChannels.fromPointer(110,10,101,360));
      assertEquals(50,PaletteChannels.fromPointer(60,10,101,100));
      assertThrows(IllegalArgumentException.class,()->new PaletteHsv(Float.NaN,0,0,255));
   }
}
