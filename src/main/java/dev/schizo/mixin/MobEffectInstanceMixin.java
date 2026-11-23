/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.access.MobEffectDurationAccess;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceMixin implements MobEffectDurationAccess {
   @Shadow
   private int duration;
   @Unique
   private int schizo$initialDuration;

   @Inject(method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("TAIL"))
   private void schizo$captureInitialDuration(
      Holder effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, MobEffectInstance hiddenEffect, CallbackInfo ci
   ) {
      this.schizo$initialDuration = duration;
   }

   @Inject(method = "setDetailsFrom", at = @At("TAIL"))
   private void schizo$captureCopiedDuration(MobEffectInstance effect, CallbackInfo ci) {
      this.schizo$initialDuration = effect.getDuration();
   }

   @Inject(method = "update", at = @At(value = "FIELD", target = "Lnet/minecraft/world/effect/MobEffectInstance;duration:I", opcode = 181, shift = Shift.AFTER))
   private void schizo$captureUpdatedDuration(CallbackInfoReturnable<Boolean> ci) {
      this.schizo$initialDuration = this.duration;
   }

   @Override
   public float schizo$getDurationRatio() {
      return (float)this.duration / this.schizo$initialDuration;
   }
}
