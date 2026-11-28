package dev.schizo.client.gui.panels;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryMatchCacheTest {
   @Test void cachesBothOutcomesUntilQueryOrScreenChanges() {
      QueryMatchCache<String> cache = new QueryMatchCache<>();
      AtomicInteger calls = new AtomicInteger();
      BiPredicate<String,String> matcher = (value, query) -> {
         calls.incrementAndGet();
         return value.contains(query);
      };
      String a = "Crosshair", b = "Zoom";
      for (int i = 0; i < 50; i++) {
         assertTrue(cache.matches("Cross", a, matcher));
         assertFalse(cache.matches("Cross", b, matcher));
      }
      assertEquals(2, calls.get());
      assertTrue(cache.matches("Zoom", b, matcher));
      assertEquals(3, calls.get());
      cache.clear();
      assertTrue(cache.matches("Zoom", b, matcher));
      assertEquals(4, calls.get());
   }
}
