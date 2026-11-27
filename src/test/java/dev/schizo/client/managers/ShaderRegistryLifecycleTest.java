package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ShaderRegistryLifecycleTest {
   @Test
   void featureFrameCleanupRunsAfterRenderFailure() {
      AtomicBoolean cleaned = new AtomicBoolean();
      IllegalStateException failure = new IllegalStateException("render failed");

      IllegalStateException thrown = assertThrows(
         IllegalStateException.class,
         () -> ShaderRegistry.finishFeatureFrame(
            () -> { throw failure; },
            () -> cleaned.set(true)
         )
      );

      assertSame(failure, thrown);
      assertTrue(cleaned.get());
   }

   @Test
   void cleanupFailureDoesNotMaskRenderFailure() {
      IllegalStateException renderFailure = new IllegalStateException("render failed");
      IllegalArgumentException cleanupFailure = new IllegalArgumentException("cleanup failed");

      IllegalStateException thrown = assertThrows(
         IllegalStateException.class,
         () -> ShaderRegistry.finishFeatureFrame(
            () -> { throw renderFailure; },
            () -> { throw cleanupFailure; }
         )
      );

      assertSame(renderFailure, thrown);
      assertArrayEquals(new Throwable[]{cleanupFailure}, thrown.getSuppressed());
   }

   @Test
   void featureFrameCleanupRunsAfterSuccessfulRender() {
      AtomicBoolean rendered = new AtomicBoolean();
      AtomicBoolean cleaned = new AtomicBoolean();

      ShaderRegistry.finishFeatureFrame(
         () -> rendered.set(true),
         () -> cleaned.set(true)
      );

      assertTrue(rendered.get());
      assertTrue(cleaned.get());
   }
}
