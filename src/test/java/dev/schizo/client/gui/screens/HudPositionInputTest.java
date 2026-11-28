package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudPositionInputTest {
   @Test void exactCoordinatesAcceptViewportEdgesAndFractionalValues() {
      var position=HudPositionInput.parse(" 12.5 ","80");
      assertNotNull(position);
      assertEquals(12.5F,position.x());
      assertTrue(position.fits(100,100,25,20));
      assertTrue(new HudPositionInput(75,80).fits(100,100,25,20));
      assertFalse(new HudPositionInput(75.1F,80).fits(100,100,25,20));
   }

   @Test void rejectsNonFiniteAndInvalidGeometry() {
      assertNull(HudPositionInput.parse("NaN","0"));
      assertNull(HudPositionInput.parse("Infinity","0"));
      assertNull(HudPositionInput.parse("text","0"));
      assertFalse(new HudPositionInput(-1,0).fits(100,100,20,20));
      assertFalse(new HudPositionInput(0,0).fits(100,100,101,20));
      assertFalse(new HudPositionInput(0,0).fits(100,100,0,20));
      assertFalse(new HudPositionInput(0,0).fits(Float.NaN,100,20,20));
   }
}
