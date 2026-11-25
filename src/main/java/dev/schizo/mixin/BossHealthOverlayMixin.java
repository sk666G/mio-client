/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.SkyboxModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {


   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void suppressBossBars(GuiGraphics graphics, CallbackInfo callback) {
      if (schizo$noRenderModule().isToggled()
         && schizo$noRenderModule().bossBarsSetting.getValue()) {
         callback.cancel();
      }
   }

   @Inject(method = "shouldDarkenScreen", at = @At("HEAD"), cancellable = true)
   private void suppressBossBarDarkening(CallbackInfoReturnable<Boolean> callback) {
      if (schizo$noRenderModule().isToggled()
            && schizo$noRenderModule().bossBarsSetting.getValue()
         || schizo$skyboxModule().isToggled()
            && schizo$skyboxModule().isCurrentDimensionEnabled()) {
         callback.setReturnValue(false);
         callback.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static SkyboxModule schizo$skyboxModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(SkyboxModule.class);
   }
}
