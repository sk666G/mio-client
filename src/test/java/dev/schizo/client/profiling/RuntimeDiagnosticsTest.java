package dev.schizo.client.profiling;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RuntimeDiagnosticsTest {
   @Test
   void recordsBoundedFailureSummaryAndCounts() {
      RuntimeDiagnostics diagnostics = new RuntimeDiagnostics();

      diagnostics.record(
         RuntimeDiagnostics.Area.EVENT_DISPATCH,
         "packet receive",
         new IllegalStateException("broken\nmessage")
      );
      diagnostics.record(
         RuntimeDiagnostics.Area.SCHEDULED_TASK,
         "rotation",
         new IllegalArgumentException("bad")
      );

      RuntimeDiagnostics.Snapshot snapshot = diagnostics.snapshot();
      assertEquals(2L, snapshot.totalFailures());
      assertEquals(1L, snapshot.counts().get(RuntimeDiagnostics.Area.EVENT_DISPATCH));
      assertEquals(1L, snapshot.counts().get(RuntimeDiagnostics.Area.SCHEDULED_TASK));
      assertEquals(RuntimeDiagnostics.Area.SCHEDULED_TASK, snapshot.lastFailure().area());
      assertEquals("rotation", snapshot.lastFailure().detail());
      assertEquals("IllegalArgumentException", snapshot.lastFailure().failureType());
   }

   @Test
   void resetClearsCountersAndLatestFailure() {
      RuntimeDiagnostics diagnostics = new RuntimeDiagnostics();
      diagnostics.record(
         RuntimeDiagnostics.Area.SETTING_CALLBACK,
         "setting",
         new RuntimeException("failure")
      );

      diagnostics.reset();

      RuntimeDiagnostics.Snapshot snapshot = diagnostics.snapshot();
      assertEquals(0L, snapshot.totalFailures());
      assertNull(snapshot.lastFailure());
      assertTrue(snapshot.counts().values().stream().allMatch(value -> value == 0L));
   }
}
