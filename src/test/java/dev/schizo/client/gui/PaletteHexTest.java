package dev.schizo.client.gui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteHexTest {
   @Test void exactRgbaRoundTripAndOpaqueShorthand() {
      var color = new Color(231,57,77,18);
      assertEquals("#E7394D12", PaletteHex.format(color));
      assertEquals(color, PaletteHex.parse("#e7394d12"));
      assertEquals(new Color(231,57,77), PaletteHex.parse("e7394d"));
      assertEquals(new Color(0,0,0,0), PaletteHex.parse("00000000"));
   }
   @Test void rejectsMalformedInputs() {
      for (String input : new String[]{"#123", "#1234567", "#GG000000", "-123456", "#123456789", ""})
         assertThrows(IllegalArgumentException.class, () -> PaletteHex.parse(input));
   }
}
