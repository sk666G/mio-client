package dev.schizo.client.gui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteRecentColorsTest {
   @Test void deduplicatesExactRgbaAndKeepsMostRecentFirst() {
      var history=new PaletteRecentColors();
      Color opaque=new Color(10,20,30,255), transparent=new Color(10,20,30,42);
      history.record(opaque);
      history.record(transparent);
      history.record(new Color(10,20,30,255));
      assertEquals(java.util.List.of(opaque,transparent),history.snapshot());
   }

   @Test void boundsHistoryAndKeepsSnapshotsImmutable() {
      var history=new PaletteRecentColors();
      history.record(Color.WHITE);
      var first=history.snapshot();
      for(int i=0;i<20;i++) history.record(new Color(i,i,i,255));
      assertEquals(PaletteRecentColors.LIMIT,history.snapshot().size());
      assertEquals(new Color(19,19,19),history.snapshot().getFirst());
      assertFalse(history.snapshot().contains(Color.WHITE));
      assertEquals(java.util.List.of(Color.WHITE),first);
      assertThrows(UnsupportedOperationException.class,()->first.add(Color.BLACK));
   }
}
