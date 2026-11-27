package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RenderTargetUtilsFailureTest {
   @Test
   void combiningSameCleanupFailureDoesNotSelfSuppress() {
      IllegalStateException failure = new IllegalStateException("cleanup");

      Throwable combined = RenderTargetUtils.combineFailures(failure, failure);

      assertSame(failure, combined);
      assertEquals(0, combined.getSuppressed().length);
   }

   @Test
   void combiningDistinctCleanupFailuresPreservesFirstAsPrimary() {
      IllegalStateException first = new IllegalStateException("first");
      IllegalArgumentException second = new IllegalArgumentException("second");

      Throwable combined = RenderTargetUtils.combineFailures(first, second);

      assertSame(first, combined);
      assertArrayEquals(new Throwable[]{second}, combined.getSuppressed());
   }

   @Test
   void combiningWithoutExistingFailureUsesNewFailure() {
      IllegalStateException failure = new IllegalStateException("cleanup");
      assertSame(failure, RenderTargetUtils.combineFailures(null, failure));
   }
}
