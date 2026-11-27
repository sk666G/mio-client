package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextRadarModuleTest {
   @Test
   void filterSortLimitReusesDestinationWithStreamEquivalentOrdering() {
      List<Integer> destination = new ArrayList<>(List.of(99, 100));

      TextRadarModule.replaceFilteredSorted(
         destination,
         List.of(5, 2, 7, 4, 1, 6),
         value -> value % 2 == 0,
         Integer::compareTo,
         2
      );

      assertEquals(List.of(2, 4), destination);
   }

   @Test
   void unlimitedStyleMaximumKeepsAllIncludedValues() {
      List<String> destination = new ArrayList<>();

      TextRadarModule.replaceFilteredSorted(
         destination,
         List.of("charlie", "alpha", "bravo"),
         value -> true,
         String::compareTo,
         Integer.MAX_VALUE
      );

      assertEquals(List.of("alpha", "bravo", "charlie"), destination);
   }
}
