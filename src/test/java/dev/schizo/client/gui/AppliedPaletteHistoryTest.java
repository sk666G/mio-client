package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppliedPaletteHistoryTest {
   private static AppliedPaletteHistory.State state(int value) {
      var p = new UiPalette(new Color(value,0,0),Color.BLACK,Color.GRAY,Color.RED,Color.WHITE,Color.BLUE);
      return new AppliedPaletteHistory.State(PaletteChoice.CUSTOM,p);
   }

   @Test void restoresExactStatesAndRetainsHistoryOnFailedRestore() {
      var history = new AppliedPaletteHistory();
      var a = state(1); var b = state(2); var c = state(3);
      history.record(a,b); history.record(b,c);
      assertFalse(history.move(c,false,target -> false));
      assertTrue(history.canMove(c,false));
      var live = new AtomicReference<>(c);
      assertTrue(history.move(live.get(),false,target -> {live.set(target);return true;}));
      assertEquals(b,live.get());
      assertTrue(history.move(live.get(),false,target -> {live.set(target);return true;}));
      assertEquals(a,live.get());
      assertTrue(history.move(live.get(),true,target -> {live.set(target);return true;}));
      assertEquals(b,live.get());
      history.record(b,state(4));
      assertFalse(history.canMove(state(4),true));
   }

   @Test void refusesStaleStateAndBoundsHistory() {
      var history = new AppliedPaletteHistory();
      history.record(state(0),state(1));
      assertFalse(history.move(state(2),false,target -> {fail("stale restore"); return true;}));
      assertFalse(history.canMove(state(1),false));
      for (int i=0;i<24;i++) history.record(state(i),state(i+1));
      int count=0;
      var live = new AtomicReference<>(state(24));
      while(history.move(live.get(),false,target -> {live.set(target);return true;})) count++;
      assertEquals(16,count);
      assertEquals(state(8),live.get());
   }
}
