package dev.schizo.client.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RuntimeDiagnosticsTest {
   @AfterEach
   void resetDiagnostics() {
      RuntimeDiagnostics.clear();
   }

   @Test
   void keepsOnlyTheMostRecentFailuresAndCountsAllOccurrences() {
      for (int index = 0; index < RuntimeDiagnostics.MAX_RECENT_FAILURES + 5; index++) {
         RuntimeDiagnostics.record(
            RuntimeDiagnostics.Area.EVENT,
            "listener",
            new IllegalStateException("failure-" + index)
         );
      }

      assertEquals(RuntimeDiagnostics.MAX_RECENT_FAILURES, RuntimeDiagnostics.recentFailures().size());
      assertEquals(RuntimeDiagnostics.MAX_RECENT_FAILURES + 5L, RuntimeDiagnostics.totalFailureCount());
      assertEquals("failure-5", RuntimeDiagnostics.recentFailures().getFirst().message());
   }

   @Test
   void recordsOwnerThreadAndOccurrenceWithoutRetainingThrowable() {
      RuntimeDiagnostics.record(
         RuntimeDiagnostics.Area.SETTING_CALLBACK,
         "Speed",
         new IllegalArgumentException("bad setting")
      );
      RuntimeDiagnostics.record(
         RuntimeDiagnostics.Area.SETTING_CALLBACK,
         "Speed",
         new IllegalArgumentException("bad setting again")
      );

      RuntimeDiagnostics.FailureRecord latest = RuntimeDiagnostics.recentFailures().getLast();
      assertEquals(RuntimeDiagnostics.Area.SETTING_CALLBACK, latest.area());
      assertEquals("Speed", latest.owner());
      assertEquals(Thread.currentThread().getName(), latest.threadName());
      assertEquals(2L, latest.occurrence());
      assertEquals(IllegalArgumentException.class.getName(), latest.failureType());
   }
}
