package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.render.BaseShaderPass;
import dev.schizo.client.render.CompiledShaderProgram;
import dev.schizo.client.render.ReloadableShaderPass;
import dev.schizo.client.render.ShaderPass;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShaderRegistryReloadTest {
   @Test
   void runtimeFailureInOnePassDoesNotAbortLaterReloads() {
      AtomicBoolean laterReloaded = new AtomicBoolean();
      AtomicInteger failures = new AtomicInteger();

      ShaderPass broken = new TestReloadablePass(() -> {
         throw new IllegalStateException("broken");
      }, () -> {
         throw new IllegalArgumentException("fallback broken");
      });
      ShaderPass healthy = new TestReloadablePass(() -> {
         laterReloaded.set(true);
         return new CompiledShaderProgram(41);
      }, () -> new CompiledShaderProgram(42));

      ShaderRegistry.reloadPasses(
         List.of(broken, healthy),
         (pass, failure) -> failures.incrementAndGet()
      );

      assertEquals(1, failures.get());
      assertTrue(laterReloaded.get());
      assertSame(healthy.getProgram(), ((BaseShaderPass)healthy).getProgram());
   }

   @Test
   void fatalErrorStillAbortsReloadSequence() {
      AssertionError fatal = new AssertionError("fatal");
      AtomicBoolean laterReloaded = new AtomicBoolean();

      ShaderPass broken = new TestReloadablePass(() -> {
         throw fatal;
      }, () -> new CompiledShaderProgram(51));
      ShaderPass later = new TestReloadablePass(() -> {
         laterReloaded.set(true);
         return new CompiledShaderProgram(52);
      }, () -> new CompiledShaderProgram(53));

      assertSame(
         fatal,
         assertThrows(
            AssertionError.class,
            () -> ShaderRegistry.reloadPasses(List.of(broken, later), (pass, failure) -> {})
         )
      );
      assertFalse(laterReloaded.get());
   }

   private static final class TestReloadablePass extends BaseShaderPass implements ReloadableShaderPass {
      private final java.util.function.Supplier<CompiledShaderProgram> primary;
      private final java.util.function.Supplier<CompiledShaderProgram> fallback;

      private TestReloadablePass(
         java.util.function.Supplier<CompiledShaderProgram> primary,
         java.util.function.Supplier<CompiledShaderProgram> fallback
      ) {
         super("vertex", "fragment");
         this.primary = primary;
         this.fallback = fallback;
      }

      @Override
      public CompiledShaderProgram compileProgram() {
         return this.primary.get();
      }

      @Override
      public CompiledShaderProgram compileFallback(BaseShaderPass ignored) {
         return this.fallback.get();
      }

      @Override
      public void reportReloadFailure(Throwable ignored) {
      }
   }
}
