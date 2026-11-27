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
      });
      ShaderPass healthy = new TestReloadablePass(() -> laterReloaded.set(true));

      ShaderRegistry.reloadPasses(
         List.of(broken, healthy),
         (pass, failure) -> failures.incrementAndGet()
      );

      assertEquals(1, failures.get());
      assertTrue(laterReloaded.get());
   }

   @Test
   void fatalErrorStillAbortsReloadSequence() {
      AssertionError fatal = new AssertionError("fatal");
      AtomicBoolean laterReloaded = new AtomicBoolean();

      ShaderPass broken = new TestReloadablePass(() -> {
         throw fatal;
      });
      ShaderPass later = new TestReloadablePass(() -> laterReloaded.set(true));

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
      private final Runnable reloadAction;

      private TestReloadablePass(Runnable reloadAction) {
         super("unused-vertex", "unused-fragment");
         this.reloadAction = reloadAction;
      }

      @Override
      public CompiledShaderProgram compileProgram() {
         throw new UnsupportedOperationException("compileProgram is not used by this test");
      }

      @Override
      public void reload(BaseShaderPass ignored) {
         this.reloadAction.run();
      }
   }
}
