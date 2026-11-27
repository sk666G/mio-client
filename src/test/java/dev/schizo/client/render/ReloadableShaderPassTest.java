package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReloadableShaderPassTest {
   @Test
   void doubleCompileFailurePreservesExistingProgram() {
      BaseShaderPass pass = new BaseShaderPass("vertex", "fragment");
      CompiledShaderProgram existing = new CompiledShaderProgram(17);
      pass.program = existing;

      ReloadableShaderPass reloadable = new ReloadableShaderPass() {
         @Override
         public CompiledShaderProgram compileProgram() {
            throw new IllegalStateException("primary");
         }

         @Override
         public CompiledShaderProgram compileFallback(BaseShaderPass ignored) {
            throw new IllegalArgumentException("fallback");
         }

         @Override
         public void reportReloadFailure(Throwable ignored) {
         }
      };

      assertDoesNotThrow(() -> reloadable.reload(pass));
      assertSame(existing, pass.program);
   }

   @Test
   void fallbackCanInitializePassWhenPrimaryCompileFails() {
      BaseShaderPass pass = new BaseShaderPass("vertex", "fragment");
      CompiledShaderProgram fallback = new CompiledShaderProgram(23);

      ReloadableShaderPass reloadable = new ReloadableShaderPass() {
         @Override
         public CompiledShaderProgram compileProgram() {
            throw new IllegalStateException("primary");
         }

         @Override
         public CompiledShaderProgram compileFallback(BaseShaderPass ignored) {
            return fallback;
         }

         @Override
         public void reportReloadFailure(Throwable ignored) {
         }
      };

      reloadable.reload(pass);

      assertSame(fallback, pass.program);
      assertNotNull(pass.uniforms);
   }

   @Test
   void doubleCompileFailureWithoutExistingProgramStillPropagatesFallbackFailure() {
      BaseShaderPass pass = new BaseShaderPass("vertex", "fragment");
      IllegalArgumentException fallbackFailure = new IllegalArgumentException("fallback");

      ReloadableShaderPass reloadable = new ReloadableShaderPass() {
         @Override
         public CompiledShaderProgram compileProgram() {
            throw new IllegalStateException("primary");
         }

         @Override
         public CompiledShaderProgram compileFallback(BaseShaderPass ignored) {
            throw fallbackFailure;
         }

         @Override
         public void reportReloadFailure(Throwable ignored) {
         }
      };

      assertSame(
         fallbackFailure,
         assertThrows(IllegalArgumentException.class, () -> reloadable.reload(pass))
      );
      assertNull(pass.program);
   }

   @Test
   void fatalCompileErrorsStillPropagate() {
      BaseShaderPass pass = new BaseShaderPass("vertex", "fragment");
      CompiledShaderProgram existing = new CompiledShaderProgram(31);
      pass.program = existing;
      AssertionError fatal = new AssertionError("fatal");

      ReloadableShaderPass reloadable = new ReloadableShaderPass() {
         @Override
         public CompiledShaderProgram compileProgram() {
            throw fatal;
         }
      };

      assertSame(fatal, assertThrows(AssertionError.class, () -> reloadable.reload(pass)));
      assertSame(existing, pass.program);
   }

}
