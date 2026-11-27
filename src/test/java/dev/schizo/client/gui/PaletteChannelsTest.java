package dev.schizo.client.gui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteChannelsTest {
   @Test void pointerRailIsBoundedAndReachesBothEndpoints() {
      assertEquals(0, PaletteChannels.fromPointer(10,10,101));
      assertEquals(128, PaletteChannels.fromPointer(60,10,101));
      assertEquals(255, PaletteChannels.fromPointer(110,10,101));
      assertEquals(255, PaletteChannels.fromPointer(200,10,101));
      assertEquals(0, PaletteChannels.fromPointer(-5,10,101));
   }

   @Test void updatesOneChannelWithoutChangingOthers() {
      Color c = new Color(12,34,56,78);
      assertEquals(new Color(255,34,56,78), PaletteChannels.replace(c,0,300));
      assertEquals(new Color(12,0,56,78), PaletteChannels.replace(c,1,-5));
      assertEquals(new Color(12,34,80,78), PaletteChannels.replace(c,2,80));
      assertEquals(new Color(12,34,56,0), PaletteChannels.replace(c,3,0));
      assertEquals(78, PaletteChannels.get(c,3));
   }
}
