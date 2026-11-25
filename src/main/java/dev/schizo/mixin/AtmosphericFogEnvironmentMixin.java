/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.modules.render.SkyboxModule;
import java.awt.Color;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {
   private static SkyboxModule skyboxModule() {
      return ClientContext.requireModule(SkyboxModule.class);
   }

   @ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"))
   private int blendConfiguredFogColor(int originalColor) {
      if (skyboxModule().isToggled()
         && skyboxModule().isCurrentDimensionEnabled()) {
         Color configuredColor = skyboxModule().backgroundSetting.getValue();
         Color baseColor = new Color(originalColor, true);
         return ColorUtils.blend(
               configuredColor, baseColor, configuredColor.getAlpha() / 255.0F
            )
            .hashCode();
      } else {
         return originalColor;
      }
   }
}
