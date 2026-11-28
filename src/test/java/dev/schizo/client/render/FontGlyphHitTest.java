package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

class FontGlyphHitTest {
   @Test
   void fractionalAdvancesAndSupplementaryCharactersUseHalfOpenBounds() {
      Style first = Style.EMPTY.withBold(true);
      Style second = Style.EMPTY.withItalic(true);
      FormattedCharSequence text = sink -> sink.accept(0, first, 'A')
         && sink.accept(1, second, 0x1F600);
      List<Style> hits = new ArrayList<>();
      FontActiveTextCollector.visitGlyphStyleAt(text, 2.49F, cp -> cp == 'A' ? 2.5 : 3.25, hits::add);
      FontActiveTextCollector.visitGlyphStyleAt(text, 2.5F, cp -> cp == 'A' ? 2.5 : 3.25, hits::add);
      FontActiveTextCollector.visitGlyphStyleAt(text, 5.75F, cp -> cp == 'A' ? 2.5 : 3.25, hits::add);
      assertEquals(List.of(first, second), hits);
   }

   @Test
   void zeroAdvanceDoesNotClaimTheNextGlyph() {
      Style first = Style.EMPTY.withBold(true);
      Style second = Style.EMPTY.withItalic(true);
      FormattedCharSequence text = sink -> sink.accept(0, first, 'A')
         && sink.accept(1, second, 'B');
      List<Style> hits = new ArrayList<>();
      FontActiveTextCollector.visitGlyphStyleAt(text, 0.0F, cp -> cp == 'A' ? 0.0 : 1.5, hits::add);
      assertEquals(List.of(second), hits);
   }
}
