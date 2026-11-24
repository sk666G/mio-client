/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.modules.render.ChamsModule;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.GlintTextureTransformEvent;
import dev.schizo.client.managers.ModuleRegistry;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(TextureTransform.class)
public class TextureTransformMixin {

   @ModifyArgs(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;scale(F)Lorg/joml/Matrix4f;", remap = false))
   private static void modifyGlintScale(Args arguments) {
      GlintTextureTransformEvent event = GlintTextureTransformEvent.resetAndGet();
      ClientAccess.eventBus.post(event);
      arguments.set(0, arguments.<Float>get(0) * event.getScale());
   }

   @ModifyArgs(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;translation(FFF)Lorg/joml/Matrix4f;", remap = false))
   private static void applyGlintProgressTranslation(Args arguments) {
      if (MixinModuleRegistry.textureTransformChamsModule.speedSetting
            .isAtMinimum()
         && ChamsModule.renderingGlintBatch) {
         float progress = (MixinModuleRegistry.textureTransformChamsModule.progressSetting.getValue())
               .intValue()
            / 100.0F;
         arguments.set(0, -progress);
         arguments.set(1, progress);
      }
   }

}
