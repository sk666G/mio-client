package dev.schizo.client.gui.components;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class ButtonLabelLinesTest {
   @Test void unchangedLabelsReuseImmutableLinesAndChangesInvalidate() {
      var cache=new ButtonLabelLines();
      var first=cache.resolve("A\nB");
      assertEquals(List.of("A","B"),first);
      assertSame(first,cache.resolve(new String("A\nB")));
      assertEquals(List.of("C"),cache.resolve("C"));
      assertThrows(UnsupportedOperationException.class,()->first.add("D"));
   }
   @Test void preservesLegacyLineSplitting() {
      var cache=new ButtonLabelLines();
      for(String label:List.of("","A","A\n","A\n\nB","\n","\nA"))
         assertEquals(List.of(label.split("\n")),cache.resolve(label));
   }
}
