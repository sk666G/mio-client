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
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.movement.NoSlowModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public class SlimeBlockMixin {

   @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
   private void suppressSlimeStepEffect(
      Level level, BlockPos blockPos, BlockState blockState, Entity entity, CallbackInfo callback
   ) {
      if (MixinModuleRegistry.slimeBlockNoslow.isToggled() && MixinModuleRegistry.slimeBlockNoslow.slimeSetting.getValue()) {
         callback.cancel();
      }
   }

   @Inject(method = "bounceUp", at = @At("HEAD"), cancellable = true)
   private void suppressSlimeBounce(Entity entity, CallbackInfo callback) {
      if (MixinModuleRegistry.slimeBlockNoslow.isToggled() && MixinModuleRegistry.slimeBlockNoslow.slimeSetting.getValue() && MixinModuleRegistry.slimeBlockNoslow.noSlimeBounceSetting.getValue()) {
         callback.cancel();
      }
   }

}
