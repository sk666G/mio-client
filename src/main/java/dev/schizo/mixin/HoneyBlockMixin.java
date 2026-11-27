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
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.movement.NoSlowModule;
import net.minecraft.world.level.block.HoneyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HoneyBlock.class)
public class HoneyBlockMixin {

   @ModifyExpressionValue(
      method = "entityInside",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/HoneyBlock;isSlidingDown(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Z")
   )
   private boolean suppressHoneySliding(boolean originalSliding) {
      return MixinModuleRegistry.honeyBlockNoSlowModule.isToggled()
            && MixinModuleRegistry.honeyBlockNoSlowModule.honeySetting.getValue()
         ? false
         : originalSliding;
   }

}
