/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.render.ESPModule;

public class ESPOutlineShaderPass extends PostProcessShaderPass {
   private static ESPModule espModule;

   @Override
   public void updateUniforms() {
      super.updateUniforms();
      ShaderUniforms widthUniforms = this.uniforms;

      widthUniforms.set("u_Width", 2);
      ShaderUniforms glowUniforms = this.uniforms;

      glowUniforms.set("u_GlowMultiplier", 1.0F);
      ShaderUniforms fillOpacityUniforms = this.uniforms;

      float fillOpacity = (espModule.blockFillOpacitySetting.getValue());
      fillOpacityUniforms.set("u_FillOpacity", fillOpacity);
   }

   public static void bindModules(ModuleRegistry registry) {
      espModule = (ESPModule)registry.getModule(ESPModule.class);
   }

   public ESPOutlineShaderPass() {
      super("base.vert", "outline.frag");

      this.initializeProgram();
   }
}
