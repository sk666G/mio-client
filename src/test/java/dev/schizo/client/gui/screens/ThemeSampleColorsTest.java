package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeSampleColorsTest {
   @Test void compositesAlphaWithoutChangingPalette() {
      var clear = new Color(255,0,0,0);
      var half = new Color(255,0,0,128);
      var p = new UiPalette(half,clear,clear,clear,clear,half);
      var dark = ThemeSampleColors.from(p,false);
      var light = ThemeSampleColors.from(p,true);
      assertEquals(0xff14151b,dark.surface());
      assertEquals(0xffc5c6cc,light.surface());
      assertEquals(dark.surface(),dark.button());
      assertEquals(0xff8a0a0d,dark.accent());
      assertEquals(dark.surface(),dark.gradient(0));
      assertEquals(dark.accent(),dark.gradient(1));
      assertEquals(128,p.accent().getAlpha());
   }

   @Test void lowContrastDetectedAndOpaqueColorsIgnoreBackdrop() {
      var p = new UiPalette(Color.WHITE,Color.WHITE,Color.WHITE,Color.WHITE,Color.WHITE,Color.WHITE);
      var light = ThemeSampleColors.from(p,true);
      assertEquals(light,ThemeSampleColors.from(p,false));
      assertEquals(1.0,light.minimumTextContrast(),1e-9);
      assertEquals(21.0,StudioScreenTheme.contrast(0xff000000,0xffffffff),1e-9);
   }
}
