package dev.schizo.client.profiling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCaptureTest {
   @Test void capturesUniqueFramesAndSkipsBoundaryInterval() {
      BenchmarkCapture capture = new BenchmarkCapture();
      capture.start(0);
      capture.onFrame(10_000_000L, 10_000_000L);
      capture.onFrame(20_000_000L, 10_000_000L);
      capture.onFrame(80_000_000L, 60_000_000L);
      var result = capture.finish();
      assertEquals(2, result.frames());
      assertEquals(35.0, result.meanMillis(), 0.0001);
      assertEquals(60.0, result.p99Millis(), 0.0001);
      assertEquals(1, result.hitches());
      assertEquals(1, result.timeline().size());
      assertSame(result, capture.finish());
   }

   @Test void stopsAtDeadlineAndExcludesLateFrames() {
      BenchmarkCapture capture = new BenchmarkCapture();
      capture.start(0);
      capture.onFrame(10_000_000L, 10_000_000L);
      capture.onFrame(20_000_000L, 10_000_000L);
      capture.onFrame(BenchmarkCapture.DURATION_NANOS, 10_000_000L);
      assertFalse(capture.isActive());
      assertEquals(1, capture.finish().frames());
   }

   @Test void emptyAndInvalidIntervalsDoNotProduceSummary() {
      BenchmarkCapture capture = new BenchmarkCapture();
      capture.start(0);
      capture.onFrame(1, 1);
      capture.onFrame(2, 0);
      assertNull(capture.finish());
   }

   @Test void frameCapBoundsCaptureMemory() {
      BenchmarkCapture capture = new BenchmarkCapture();
      capture.start(0);
      for (int i = 1; i <= BenchmarkCapture.MAX_FRAMES + 2; i++) capture.onFrame(i * 100L, 100L);
      assertFalse(capture.isActive());
      assertEquals(BenchmarkCapture.MAX_FRAMES, capture.finish().frames());
   }
}
