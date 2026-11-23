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
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.render.NoRenderModule;
import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Material.class)
public class MaterialMixin {
   @Shadow
   @Final
   private Identifier atlasLocation;

   @Inject(method = "renderType", at = @At("HEAD"), cancellable = true)
   private void applyTranslucentHandMaterial(Function renderTypeFactory, CallbackInfoReturnable<RenderType> callback) {
      if (MixinModuleRegistry.materialNoRenderModule.getHandOpacity()
            != 1.0F
         && RenderUtils.getRenderColor()[3]
            != 255) {
         callback.setReturnValue(RenderTypes.entityTranslucent(this.atlasLocation));
         callback.cancel();
      }
   }

}
