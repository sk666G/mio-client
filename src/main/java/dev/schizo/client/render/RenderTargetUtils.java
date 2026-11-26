/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.ClientAccess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.schizo.mixin.MinecraftAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.commons.io.IOUtils;

public final class RenderTargetUtils implements ClientAccess {
   private static final Deque<TargetState> savedTargets = new ArrayDeque<>();

   public static void popFramebuffer() {
      ((LevelRendererFramebufferAccess)ClientAccess.schizoMinecraft.levelRenderer).schizo$popFramebuffer();
   }

   public static void restoreRenderTargets() {
      TargetState previous = savedTargets.pop();
      Throwable cleanupFailure = null;

      try {
         popFramebuffer();
      } catch (RuntimeException | Error failure) {
         cleanupFailure = failure;
      }

      try {
         setMainRenderTarget(previous.mainTarget());
      } catch (RuntimeException | Error failure) {
         cleanupFailure = combineFailures(cleanupFailure, failure);
      }

      RenderSystem.outputColorTextureOverride = previous.colorOverride();
      RenderSystem.outputDepthTextureOverride = previous.depthOverride();

      rethrow(cleanupFailure);
   }

   private RenderTargetUtils() {
      throw new AssertionError();
   }

   public static void pushFramebuffer(RenderTarget target) {
      ((LevelRendererFramebufferAccess)ClientAccess.schizoMinecraft.levelRenderer)
         .schizo$pushFramebuffer(target);
   }

   public static void overrideRenderTargets(RenderTarget target) {
      TargetState previous = new TargetState(
         ClientAccess.schizoMinecraft.getMainRenderTarget(),
         RenderSystem.outputColorTextureOverride,
         RenderSystem.outputDepthTextureOverride
      );
      pushFramebuffer(target);
      savedTargets.push(previous);
      try {
         setMainRenderTarget(target);
         // RenderType prefers these overrides over its render target.
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
      } catch (RuntimeException | Error failure) {
         savedTargets.pop();
         Throwable rollbackFailure = null;
         try {
            popFramebuffer();
         } catch (RuntimeException | Error cleanupFailure) {
            rollbackFailure = cleanupFailure;
         }
         try {
            setMainRenderTarget(previous.mainTarget());
         } catch (RuntimeException | Error cleanupFailure) {
            rollbackFailure = combineFailures(rollbackFailure, cleanupFailure);
         }
         RenderSystem.outputColorTextureOverride = previous.colorOverride();
         RenderSystem.outputDepthTextureOverride = previous.depthOverride();
         if (rollbackFailure != null) {
            failure.addSuppressed(rollbackFailure);
         }
         throw failure;
      }
   }

   private static Throwable combineFailures(Throwable existing, Throwable additional) {
      if (existing == null) {
         return additional;
      }
      existing.addSuppressed(additional);
      return existing;
   }

   private static void rethrow(Throwable failure) {
      if (failure instanceof RuntimeException runtimeFailure) {
         throw runtimeFailure;
      }
      if (failure instanceof Error error) {
         throw error;
      }
   }

   private record TargetState(RenderTarget mainTarget, GpuTextureView colorOverride, GpuTextureView depthOverride) {}

   public static void setMainRenderTarget(RenderTarget target) {
      ((MinecraftAccessor)ClientAccess.schizoMinecraft).schizo$setFramebuffer(target);
   }

   public static String readShaderSource(String resourcePath) {
      try (InputStream stream = BaseShaderPass.class.getResourceAsStream("/assets/schizo/shaders/" + resourcePath)) {
         if (stream == null) {
            dev.schizo.client.ClientAccess.logger.error("Bundled shader source is missing: {}", resourcePath);
            return "";
         }
         return IOUtils.toString(stream, StandardCharsets.UTF_8);
      } catch (IOException failure) {
         dev.schizo.client.ClientAccess.logger.error("Could not read bundled shader source {}", resourcePath, failure);
         return "";
      }
   }
}
