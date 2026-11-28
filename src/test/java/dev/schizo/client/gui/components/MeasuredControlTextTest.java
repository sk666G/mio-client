package dev.schizo.client.gui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeasuredControlTextTest {
   @Test void preservesWholeCodePointsAndFitsMeasuredWidth() {
      assertEquals("😀...",MeasuredControlText.fit("😀😀😀",5,String::length));
      assertEquals("",MeasuredControlText.fit("long",2,String::length));
      assertEquals("word",MeasuredControlText.fit("word",4,String::length));
      assertEquals("",MeasuredControlText.fit("word",Float.NaN,String::length));
   }

   @Test void longLabelsRequireOnlyBoundedMeasurements() {
      var count=new java.util.concurrent.atomic.AtomicInteger();
      String fitted=MeasuredControlText.fit("a".repeat(10000),20,text->{count.incrementAndGet();return text.length();});
      assertEquals(20,fitted.length());
      assertTrue(fitted.endsWith("..."));
      assertTrue(count.get()<=17);
   }

   @Test void measuresActualCandidateRatherThanAssumingUniformCharacters() {
      java.util.function.ToDoubleFunction<String> width=text->text.codePoints().map(cp->cp=='W'?5:1).sum();
      assertEquals("W...",MeasuredControlText.fit("WWWWi",8,width));
      assertTrue(width.applyAsDouble(MeasuredControlText.fit("WWWWi",8,width))<=8);
   }
}
