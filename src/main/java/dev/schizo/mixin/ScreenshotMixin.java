/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.misc.ExtraScreenshotModule;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screenshot.class)
public class ScreenshotMixin {

   // Mojang leaves the screenshot save lambda unnamed.
   @Inject(method = "method_22691(Lcom/mojang/blaze3d/platform/NativeImage;Ljava/io/File;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
   private static void processExtraScreenshot(
      NativeImage image, File directory, Consumer messageConsumer, CallbackInfo callback
   ) {
      if (schizo$extraScreenshotModule().isToggled()) {
         Util.ioPool()
            .execute(
               () -> schizo$extraScreenshotModule().processScreenshot(
                  image
               )
            );
         if (schizo$extraScreenshotModule().shouldDeleteFile()) {
            callback.cancel();
         }
      }
   }

   @ModifyVariable(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private static Consumer suppressVanillaScreenshotMessage(Consumer messageConsumer) {
      return schizo$extraScreenshotModule().isToggled()
            && !schizo$extraScreenshotModule().isModeNone()
         ? ignoredMessage -> {}
         : messageConsumer;
   }

   @org.spongepowered.asm.mixin.Unique
   private static ExtraScreenshotModule schizo$extraScreenshotModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(ExtraScreenshotModule.class);
   }
}
