/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.events.ParticleEngineAddEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
   private static NoRenderModule noRenderModule() {
      return ClientContext.requireModule(NoRenderModule.class);
   }

   @Shadow
   @Nullable
   protected abstract Particle makeParticle(ParticleOptions options, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

   @Inject(method = "add", at = @At("HEAD"), cancellable = true)
   private void postParticleAddEvent(Particle particle, CallbackInfo callback) {
      if (!ClientAccess.eventBus.hasListeners(ParticleEngineAddEvent.class)) {
         return;
      }
      ParticleEngineAddEvent event = new ParticleEngineAddEvent(particle);
      ClientAccess.eventBus.post(event);
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
   private void suppressSelectedParticle(
      ParticleOptions options, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> callback
   ) {
      if (noRenderModule().shouldHideParticle(
         options
      )) {
         callback.setReturnValue(this.makeParticle(options, x, y, z, velocityX, velocityY, velocityZ));
         callback.cancel();
      }
   }
}
