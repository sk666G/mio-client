package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class FontFormattingColorTest {
   @Test
   void formattingPreservesEveryOpacityValue() {
      for (int alpha = 0; alpha <= 255; alpha++) {
         int base = (alpha << 24) | 0x00112233;
         assertEquals((alpha << 24) | 0x00FFAA55,
            ColorUtils.fontFormattingColor(base, 0x00FFAA55, false));
         assertEquals((alpha << 24) | 0x003F2A15,
            ColorUtils.fontFormattingColor(base, 0x00FFAA55, true));
      }
   }

   @Test
   void formattingCannotOverrideAlphaEvenWithAnArgbInput() {
      assertEquals(0x80445566, ColorUtils.fontFormattingColor(0x80112233, 0xFF445566, false));
      assertEquals(0x80000000, ColorUtils.fontFormattingColor(0x80112233, 0, true));
      assertEquals(0xFF3F3F3F, ColorUtils.fontFormattingColor(0xFFFFFFFF, 0xFFFFFF, true));
   }
}
