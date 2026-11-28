package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Font;
import java.util.List;
import org.junit.jupiter.api.Test;

class FontAtlasFallbackTest {
   @Test
   void missingGlyphsUseOneFallbackAdvancePerCodePoint() {
      FontAtlas atlas = new FontAtlas(new Font("Dialog", Font.PLAIN, 32));
      atlas.buildAtlas(List.of('A', '?'));
      float fallback = atlas.getGlyphWidth('?');
      assertTrue(fallback > 0);
      assertEquals(fallback, atlas.getGlyphWidth(0x1F600));
      assertEquals(fallback, atlas.getGlyphWidth('\u2603'));
      assertEquals(fallback, atlas.getGlyphWidth('\uD83D'));
      assertEquals(fallback, atlas.getGlyphWidth(-1));
      assertEquals(fallback, atlas.getGlyphWidth(0x110000));
      assertTrue(atlas.getGlyphWidth('A') > 0);
   }

   @Test
   void rebuildingAtlasRemovesStaleGlyphs() {
      FontAtlas atlas = new FontAtlas(new Font("Dialog", Font.PLAIN, 32));
      atlas.buildAtlas(List.of('A', '?'));
      assertTrue(atlas.getGlyphWidth('A') > 0);
      atlas.buildAtlas(List.of('B'));
      assertEquals(0.0F, atlas.getGlyphWidth('A'));
      assertEquals(0.0F, atlas.getGlyphWidth('?'));
      assertTrue(atlas.getGlyphWidth('B') > 0);
   }

   @Test
   void absentFallbackDoesNotInventAnAdvance() {
      FontAtlas atlas = new FontAtlas(new Font("Dialog", Font.PLAIN, 32));
      atlas.buildAtlas(List.of('A'));
      assertEquals(0.0F, atlas.getGlyphWidth(0x1F600));
      assertEquals(0.0F, atlas.getGlyphWidth('?'));
   }
}
