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

   @Test void exactScaleInputRejectsInvalidAndOutOfRangePercentages() {
      assertEquals(0.5F,HudPositionInput.parseScalePercent("50"));
      assertEquals(1.255F,HudPositionInput.parseScalePercent("125.5"),0.00001F);
      assertEquals(2F,HudPositionInput.parseScalePercent("200"));
      for(String invalid:new String[]{"49.99","201","NaN","Infinity","hello",""})
         assertNull(HudPositionInput.parseScalePercent(invalid));
   }
   @Test void scaleFitUsesCurrentSizeAndPreservesFreePosition() {
      assertTrue(HudPositionInput.scaleFits(1,2,20,20,40,40,100,100,false));
      assertFalse(HudPositionInput.scaleFits(1,2,21,20,40,40,100,100,false));
      assertTrue(HudPositionInput.scaleFits(1,2,90,90,40,40,100,100,true));
      assertFalse(HudPositionInput.scaleFits(1,2,0,0,60,60,100,100,true));
      assertFalse(HudPositionInput.scaleFits(0,1,0,0,40,40,100,100,false));
      assertFalse(HudPositionInput.scaleFits(1,Float.NaN,0,0,40,40,100,100,false));
   }
}
