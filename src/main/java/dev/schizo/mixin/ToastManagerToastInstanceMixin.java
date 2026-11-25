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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/gui/components/toasts/ToastManager$ToastInstance")
public class ToastManagerToastInstanceMixin {

   @Final
   @Shadow
   private Toast toast;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   public void suppressSelectedToast(GuiGraphics graphics, int screenWidth, CallbackInfo callback) {
      if (schizo$noRenderModule().isToggled()
         && (
            this.toast.getToken() == SystemToastId.UNSECURE_SERVER_WARNING && schizo$noRenderModule().unsecureServerSetting.getValue()
               || this.toast instanceof TutorialToast && schizo$noRenderModule().tutorialToastSetting.getValue()
         )) {
         callback.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }
}
