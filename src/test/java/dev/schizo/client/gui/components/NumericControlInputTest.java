package dev.schizo.client.gui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumericControlInputTest {
   @Test void integerDragAlwaysYieldsAWholeNumberIncludingNegativeRanges() {
      for(int i=-100;i<=1100;i++) {
         double result=NumericControlInput.slider(i/10.0,0,100,-20,31,true);
         assertTrue(result>=-20&&result<=31);
         assertEquals(Math.rint(result),result);
      }
      assertEquals(-20,NumericControlInput.slider(-1,0,100,-20,31,true),0);
      assertEquals(31,NumericControlInput.slider(101,0,100,-20,31,true),0);
      assertEquals(2,NumericControlInput.slider(50,0,100,0,3,true),0);
   }

   @Test void fractionalSliderRetainsExactEndpointsAndBoundedSteps() {
      assertEquals(0.03,NumericControlInput.slider(0,0,100,0.03,1.07,false),0);
      assertEquals(1.07,NumericControlInput.slider(100,0,100,0.03,1.07,false),0);
      assertEquals(0.6,NumericControlInput.slider(52,0,100,0,1.2,false),1e-9);
      assertEquals(-0.5,NumericControlInput.slider(25,0,100,-1,1,false),1e-9);
   }

   @Test void rejectsInvalidTextAndNonFiniteOrDegenerateGeometry() {
      assertNull(NumericControlInput.parse("1.5",0,10,true));
      assertNull(NumericControlInput.parse("NaN",0,10,false));
      assertNull(NumericControlInput.parse("Infinity",0,10,false));
      assertNull(NumericControlInput.parse("11",0,10,true));
      assertNull(NumericControlInput.parse("--1",-10,10,false));
      assertEquals(1.0,NumericControlInput.parse(" 1.0 ",0,10,true),0);
      assertTrue(Double.isNaN(NumericControlInput.slider(0,0,0,0,10,false)));
      assertTrue(Double.isNaN(NumericControlInput.slider(Double.NaN,0,20,0,10,false)));
      assertTrue(Double.isNaN(NumericControlInput.slider(0,0,20,1,1,false)));
   }
}
