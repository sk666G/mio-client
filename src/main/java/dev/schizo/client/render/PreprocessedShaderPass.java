/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.RenderTargetRegistry;
import dev.schizo.client.modules.render.ShaderModule;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;

public abstract class PreprocessedShaderPass extends PostProcessShaderPass {
   private static final Supplier<RenderTarget> PRE_TARGET = RenderTargetRegistry.createLazyRenderTarget("zio_pre_pass");
   private static volatile PrePass coveragePass;
   private static volatile PrePass distancePass;
   private final PrePass prePass;

   private static RenderTarget preTarget() {
      return PRE_TARGET.get();
   }

   protected static PrePass coveragePass() {
      PrePass pass = coveragePass;
      if (pass != null) {
         return pass;
      }
      synchronized (PreprocessedShaderPass.class) {
         pass = coveragePass;
         if (pass == null) {
            pass = new PrePass("multi/coverage.frag", "u_Coverage", true);
            coveragePass = pass;
         }
         return pass;
      }
   }

   protected static PrePass distancePass() {
      PrePass pass = distancePass;
      if (pass != null) {
         return pass;
      }
      synchronized (PreprocessedShaderPass.class) {
         pass = distancePass;
         if (pass == null) {
            pass = new PrePass("multi/distance.frag", "u_Distance", false);
            distancePass = pass;
         }
         return pass;
      }
   }

   protected static ShaderModule shaderModule() {
      return ClientContext.getModule(ShaderModule.class);
   }

   protected PreprocessedShaderPass(String vertexResource, String fragmentResource, PrePass prePass) {
      super(vertexResource, fragmentResource);
      this.prePass = prePass;
   }

   @Override
   protected void prepareOutput() {
      if (!shaderModule().shouldSkipHandOutline() && shaderModule().lineWidthSetting.getValue() > 0 && this.prePass != null) {
         this.prePass.apply(postTarget());
      }
   }

   @Override
   public void updateUniforms() {
      super.updateUniforms();
      if (this.prePass != null) {
         this.uniforms.set(this.prePass.samplerName, 16, preTarget());
      }
   }

   protected static final class PrePass extends RenderTargetShaderStage {
      private final String samplerName;
      private final boolean coverage;

      private PrePass(String fragmentResource, String samplerName, boolean coverage) {
         super("base.vert", fragmentResource, preTarget());
         this.samplerName = samplerName;
         this.coverage = coverage;
      }

      @Override
      public void updateUniforms() {
         super.updateUniforms();
         this.uniforms.set("u_Width", shaderModule().lineWidthSetting.getValue());
         if (this.coverage) {
            this.uniforms.set("u_Step", shaderModule().qualitySetting.getValue());
         }
      }
   }
}
