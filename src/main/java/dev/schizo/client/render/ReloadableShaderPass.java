/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.utils.chat.ChatUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;

public interface ReloadableShaderPass {
   String RELOAD_FAILURE_MESSAGE = "Failed to compile shader, check Minecraft logs for more details.";

   CompiledShaderProgram compileProgram();

   default CompiledShaderProgram compileFallback(BaseShaderPass pass) {
      String fallbackVertexRead = RenderTargetUtils.readShaderSource(
         pass.vertexResource
      );
      String fallbackFragmentRead = RenderTargetUtils.readShaderSource(
         pass.fragmentResource
      );
      return CompiledShaderProgram.compile(fallbackVertexRead, fallbackFragmentRead);
   }

   default void reportReloadFailure(Throwable failure) {
      String errorMessage = RELOAD_FAILURE_MESSAGE;
      String failureMessage = failure.getMessage();
      ShaderDiagnostics.logError(failureMessage);
      MutableComponent plainNotification = Component.literal(errorMessage);
      ChatFormatting errorColor = ChatFormatting.RED;
      MutableComponent coloredNotification = plainNotification.withStyle(errorColor);
      MessageSignature signature = ChatUtils.createSignature(-99);
      ChatUtils.MessagePriority priority = ChatUtils.MessagePriority.HIGH;
      ChatUtils.printChat(coloredNotification, signature, priority);
   }

   default void reload(BaseShaderPass pass) {
      CompiledShaderProgram previousProgram = pass.program;
      CompiledShaderProgram replacementProgram;
      try {
         replacementProgram = this.compileProgram();
      } catch (RuntimeException | Error primaryFailure) {
         try {
            this.reportReloadFailure(primaryFailure);
         } catch (RuntimeException | Error reportingFailure) {
            if (reportingFailure != primaryFailure) {
               primaryFailure.addSuppressed(reportingFailure);
            }
         }

         try {
            replacementProgram = this.compileFallback(pass);
         } catch (RuntimeException | Error fallbackFailure) {
            if (fallbackFailure != primaryFailure) {
               primaryFailure.addSuppressed(fallbackFailure);
            }
            ShaderDiagnostics.logError(fallbackFailure.getMessage());
            if (previousProgram != null) {
               return;
            }
            throw fallbackFailure;
         }
      }

      if (replacementProgram == null) {
         if (previousProgram != null) {
            return;
         }
         throw new IllegalStateException("Shader reload produced no replacement program");
      }

      ShaderUniforms replacementUniforms = new ShaderUniforms(replacementProgram.programId());
      pass.program = replacementProgram;
      pass.uniforms = replacementUniforms;

      if (previousProgram != null && previousProgram != replacementProgram) {
         previousProgram.delete();
      }
   }

}
