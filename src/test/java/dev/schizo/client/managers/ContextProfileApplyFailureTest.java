package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContextProfileApplyFailureTest {
   @Test
   void remembersFailedContextWithoutSuppressingDifferentContexts() {
      ContextProfileApplyFailure failure = new ContextProfileApplyFailure();

      assertFalse(failure.shouldSkip("server-a|overworld|survival"));
      failure.markFailed("server-a|overworld|survival");

      assertTrue(failure.shouldSkip("server-a|overworld|survival"));
      assertFalse(failure.shouldSkip("server-a|the_nether|survival"));
      assertFalse(failure.shouldSkip("server-b|overworld|survival"));
      assertEquals("server-a|overworld|survival", failure.getFailedContext());
   }

   @Test
   void explicitRetryClearsFailedContext() {
      ContextProfileApplyFailure failure = new ContextProfileApplyFailure();
      failure.markFailed("server-a|overworld|survival");

      failure.clear();

      assertNull(failure.getFailedContext());
      assertFalse(failure.shouldSkip("server-a|overworld|survival"));
   }

   @Test
   void laterFailureReplacesEarlierContextFailure() {
      ContextProfileApplyFailure failure = new ContextProfileApplyFailure();
      failure.markFailed("server-a|overworld|survival");
      failure.markFailed("server-b|overworld|survival");

      assertFalse(failure.shouldSkip("server-a|overworld|survival"));
      assertTrue(failure.shouldSkip("server-b|overworld|survival"));
   }
}
