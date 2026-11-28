package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Font;
import java.util.List;
import org.junit.jupiter.api.Test;

class FontAtlasGeometryTest {
   @Test
   void underestimatedAtlasExpandsAndRenormalizesEveryGlyph() {
      FontAtlas atlas = new FontAtlas(new Font("Dialog", Font.PLAIN, 32));
      List<Character> characters = List.of('A', '?', 'W', 'i');
      atlas.buildAtlas(characters, 2);
      assertTrue(atlas.atlasSize() > 2, "must exercise the expansion branch");
      for (char character : characters) {
         assertContained(atlas, character);
      }
   }

   @Test
   void normalEstimatesContainMixedScriptGlyphsAcrossStylesAndSizes() {
      List<Character> characters = List.of('A', 'W', '?', '\u0416', '\u4e2d', '\u3042', 'A', ' ');
      for (int style : new int[]{Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}) {
         for (int size : new int[]{12, 24, 32, 40}) {
            FontAtlas atlas = new FontAtlas(new Font("Dialog", style, size));
            atlas.buildAtlas(characters);
            for (char character : characters) {
               assertContained(atlas, character);
            }
         }
      }
   }

   @Test
   void emptyRebuildClearsGeometryAndResetsDimensions() {
      FontAtlas atlas = new FontAtlas(new Font("Dialog", Font.PLAIN, 32));
      atlas.buildAtlas(List.of('A', '?'));
      atlas.buildAtlas(List.of());
      assertEquals(2, atlas.atlasSize());
      assertEquals(0, atlas.getHeight());
      assertNull(atlas.resolveGlyph('A'));
      assertNull(atlas.resolveGlyph('?'));
      assertNull(atlas.resolveGlyph(0x1F600));
   }

   private static void assertContained(FontAtlas atlas, char character) {
      int size = atlas.atlasSize();
      assertTrue(size > 0 && size % 2 == 0);
      FontAtlas.Glyph glyph = atlas.resolveGlyph(character);
      assertNotNull(glyph);
      assertTrue(glyph.x() >= 0 && glyph.y() >= 0);
      assertTrue(glyph.width() > 0 && glyph.height() > 0);
      assertTrue(glyph.x() + glyph.width() <= size);
      assertTrue(glyph.y() + glyph.height() <= size);
      assertEquals(0, glyph.x() % 2);
      assertEquals(0, glyph.y() % 2);
      assertEquals(0, glyph.width() % 2);
      assertEquals(0, glyph.height() % 2);
      assertEquals((float)glyph.x() / size, glyph.minimumUv().u());
      assertEquals((float)glyph.y() / size, glyph.minimumUv().v());
      assertEquals((float)(glyph.x() + glyph.width()) / size, glyph.maximumUv().u());
      assertEquals((float)(glyph.y() + glyph.height()) / size, glyph.maximumUv().v());
      assertTrue(glyph.minimumUv().u() >= 0 && glyph.minimumUv().v() >= 0);
      assertTrue(glyph.maximumUv().u() <= 1 && glyph.maximumUv().v() <= 1);
      assertTrue(glyph.maximumUv().u() > glyph.minimumUv().u());
      assertTrue(glyph.maximumUv().v() > glyph.minimumUv().v());
   }
}
