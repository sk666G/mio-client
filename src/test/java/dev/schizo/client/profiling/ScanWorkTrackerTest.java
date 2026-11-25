package dev.schizo.client.profiling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanWorkTrackerTest {
   @Test
   void countsWorkSeparatelyFromSliceTimeAndReturnsAnIsolatedSnapshot() {
      var tracker = new ScanWorkTracker();
      tracker.record(ScanWorkTracker.Source.SEARCH, 100, 20);
      tracker.record(ScanWorkTracker.Source.SEARCH, 300, 10);
      var snapshot = tracker.snapshot();
      assertEquals(30, snapshot.getFirst().cells());
      assertEquals(200, snapshot.getFirst().averageSliceNanos());
      assertEquals(300, snapshot.getFirst().maximumSliceNanos());
      tracker.reset();
      assertTrue(tracker.snapshot().isEmpty());
      assertEquals(400, snapshot.getFirst().totalNanos());
      assertThrows(UnsupportedOperationException.class, snapshot::clear);
   }
}
