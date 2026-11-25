package dev.schizo.client.modules.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrailHistoryTest {
   @Test
   void evictsOldPointsAndLeastRecentlySampledEntity() {
      TrailHistory<Integer> history = new TrailHistory<>(2, 2);
      history.add(10, 1);
      history.add(20, 2);
      history.add(10, 3);
      history.add(10, 4);
      history.add(30, 5);
      assertEquals(List.of(List.of(3, 4), List.of(5)), history.values().stream().map(List::copyOf).toList());
      history.prune(point -> point < 5);
      assertEquals(List.of(List.of(5)), history.values().stream().map(List::copyOf).toList());
      history.clear();
      assertEquals(0, history.values().size());
   }
}
