package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeShareCodeTest {
   private final UiPalette palette = new UiPalette(new Color(231,57,77,18),Color.BLACK,Color.GRAY,
      Color.RED,new Color(0,0,0,0),Color.BLUE);

   @Test void exactRoundTripIncludesAlphaAndAllSixColors() {
      String code = ThemeShareCode.encode(palette);
      assertEquals("SCHIZO-THEME-1:E7394D12:000000FF:808080FF:FF0000FF:00000000:0000FFFF",code);
      assertEquals(palette,ThemeShareCode.decode(code));
      assertEquals(palette,ThemeShareCode.decode(" \n"+code+"\n"));
   }

   @Test void rejectsUnsupportedVersionsMissingAndExtraFieldsAndMalformedColors() {
      String code = ThemeShareCode.encode(palette);
      for (String invalid : new String[]{code.replace("THEME-1","THEME-2"),code.substring(0,code.length()-9),
         code+":00000000",code.replace("E7394D12","E7394D"),code.replace("E7394D12","GG394D12"),
         code+"\nexecute", "x".repeat(257), ""})
         assertThrows(IllegalArgumentException.class,() -> ThemeShareCode.decode(invalid));
      assertThrows(IllegalArgumentException.class,() -> ThemeShareCode.decode(null));
   }
}
