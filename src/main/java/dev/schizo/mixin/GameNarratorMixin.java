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
import dev.schizo.client.modules.misc.NoNarratorModule;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.NarratorStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameNarrator.class)
public class GameNarratorMixin {
   private static NoNarratorModule noNarratorModule = ClientContext.getModule(NoNarratorModule.class);

   @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
   private void forceNarratorOff(CallbackInfoReturnable<NarratorStatus> callback) {
      if (noNarratorModule.isToggled()) {
         callback.setReturnValue(NarratorStatus.OFF);
      }
   }

   @Inject(method = "updateNarratorStatus", at = @At("HEAD"), cancellable = true)
   private void suppressNarratorStatusUpdate(CallbackInfo callback) {
      if (noNarratorModule.isToggled()) {
         callback.cancel();
      }
   }
}
