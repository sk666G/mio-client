package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioScreenThemeTest {
   @Test void namedPalettesResolveToDistinctChrome() {
      var red = StudioScreenTheme.from(PaletteChoice.SCHIZO.getPalette());
      var blue = StudioScreenTheme.from(PaletteChoice.MIDNIGHT.getPalette());
      assertNotEquals(red.accent(), blue.accent());
      assertNotEquals(red.panel(), blue.panel());
      assertTrue(StudioScreenTheme.contrast(red.accent(), red.panel()) >= 4.5);
      assertTrue(StudioScreenTheme.contrast(blue.accent(), blue.panel()) >= 4.5);
   }

   @Test void darkAccentAndBrightCustomSurfaceRemainReadable() {
      var palette = new UiPalette(Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
      var theme = StudioScreenTheme.from(palette);
      assertTrue(StudioScreenTheme.contrast(theme.accent(), theme.panel()) >= 4.5);
      assertEquals(Color.BLACK, palette.accent());
      assertEquals(Color.WHITE, palette.background());
      assertEquals(255, theme.panel() >>> 24);
   }

   @Test void chromeCacheReusesUnchangedRgbButReflectsEachRelevantColorChange() {
      var cache=new StudioScreenTheme.Cache();
      int accent=0xffe7394d,background=0xff13141a,button=0xff21222a;
      var first=cache.resolve(accent,background,button);
      assertSame(first,cache.resolve(accent & 0x00ffffff,background & 0x00ffffff,button & 0x00ffffff));
      var next=cache.resolve(0xff0088ff,background,button);
      assertNotSame(first,next);
      assertNotEquals(first.accent(),next.accent());
      var changedBackground=cache.resolve(0xff0088ff,0xffffffff,button);
      assertNotEquals(next.panel(),changedBackground.panel());
      var changedButton=cache.resolve(0xff0088ff,0xffffffff,0xffffffff);
      assertNotEquals(changedBackground.row(),changedButton.row());
      assertSame(changedButton,cache.resolve(0xff0088ff,0xffffffff,0xffffffff));
   }

   @Test void packedCacheMatchesPaletteDerivedChrome() {
      var cache=new StudioScreenTheme.Cache();
      var random=new java.util.Random(3728);
      for(int i=0;i<1000;i++) {
         Color a=new Color(random.nextInt(),true),b=new Color(random.nextInt(),true),c=new Color(random.nextInt(),true);
         var palette=new UiPalette(a,b,c,Color.BLACK,Color.WHITE,Color.RED);
         assertEquals(StudioScreenTheme.from(palette),cache.resolve(a.getRGB(),b.getRGB(),c.getRGB()));
      }
   }
}
