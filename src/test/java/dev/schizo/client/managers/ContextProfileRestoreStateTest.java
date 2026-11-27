package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ContextProfileRestoreStateTest {
   @Test
   void capturesAllSnapshotsBeforeReplacingAnExistingBaseline() {
      Map<String, Integer> baseline = new LinkedHashMap<>();
      baseline.put("old", 9);
      Set<String> modified = new LinkedHashSet<>();
      List<String> failures = new ArrayList<>();

      boolean captured = ContextProfileRestoreState.capture(
         baseline, modified, List.of("a", "b"),
         key -> {
            if (key.equals("b")) throw new IllegalStateException("snapshot failed");
            return 1;
         },
         (key, failure) -> failures.add(key)
      );

      assertFalse(captured);
      assertEquals(Map.of("old", 9), baseline);
      assertEquals(List.of("b"), failures);
   }

   @Test
   void doesNotReplaceBaselineWhileAnyCategoryIsStillModified() {
      Map<String, Integer> baseline = new LinkedHashMap<>(Map.of("a", 1));
      Set<String> modified = new LinkedHashSet<>(Set.of("a"));

      assertFalse(ContextProfileRestoreState.capture(
         baseline, modified, List.of("a"), key -> 2, (key, failure) -> fail(failure)
      ));
      assertEquals(Map.of("a", 1), baseline);
      assertEquals(Set.of("a"), modified);
   }

   @Test
   void partialRestoreRetainsOnlyFailedCategoriesForRetry() {
      Map<String, Integer> baseline = new LinkedHashMap<>(Map.of("a", 1, "b", 2));
      Set<String> modified = new LinkedHashSet<>(List.of("a", "b"));
      AtomicInteger aAttempts = new AtomicInteger();
      List<String> restored = new ArrayList<>();

      boolean first = ContextProfileRestoreState.restore(
         baseline, modified, List.of("a", "b"),
         (key, value) -> {
            if (key.equals("a") && aAttempts.getAndIncrement() == 0) {
               throw new IllegalStateException("temporary restore failure");
            }
            restored.add(key + value);
         },
         (key, failure) -> { }
      );

      assertFalse(first);
      assertEquals(Set.of("a"), modified);
      assertEquals(List.of("b2"), restored);

      boolean second = ContextProfileRestoreState.restore(
         baseline, modified, List.of("a", "b"),
         (key, value) -> restored.add(key + value),
         (key, failure) -> fail(failure)
      );

      assertTrue(second);
      assertTrue(modified.isEmpty());
      assertEquals(List.of("b2", "a1"), restored);
   }

   @Test
   void missingSnapshotDoesNotClearModifiedState() {
      Map<String, Integer> baseline = new LinkedHashMap<>();
      Set<String> modified = new LinkedHashSet<>(Set.of("missing"));
      List<String> failures = new ArrayList<>();

      assertFalse(ContextProfileRestoreState.restore(
         baseline, modified, List.of("missing"),
         (key, value) -> fail("restore must not run"),
         (key, failure) -> failures.add(key)
      ));
      assertEquals(Set.of("missing"), modified);
      assertEquals(List.of("missing"), failures);
   }

   @Test
   void successfulRestoreRemovesEachCategoryFromModifiedSet() {
      Map<String, Integer> baseline = new LinkedHashMap<>(Map.of("hud", 4));
      Set<String> modified = new LinkedHashSet<>(Set.of("hud"));

      assertTrue(ContextProfileRestoreState.restore(
         baseline, modified, List.of("hud"),
         (key, value) -> assertEquals(4, value),
         (key, failure) -> fail(failure)
      ));
      assertTrue(modified.isEmpty());
   }
}
