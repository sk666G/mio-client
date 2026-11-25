/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import baritone.api.pathing.goals.GoalBlock;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.player.FreecamModule;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = "baritone/command/defaults/ComeCommand", remap = false)
public class BaritoneComeCommandMixin {

   @ModifyArgs(
      method = "execute",
      at = @At(value = "INVOKE", target = "Lbaritone/api/process/ICustomGoalProcess;setGoalAndPath(Lbaritone/api/pathing/goals/Goal;)V"),
      remap = false
   )
   private void useFreecamGoal(Args args) {
      if (schizo$freecam().isToggled()) {
         BlockPos cameraPosition = ClientAccess.schizoMinecraft.gameRenderer.getMainCamera().blockPosition();
         if (WorldUtils.isAir(cameraPosition)
            && WorldUtils.isAir(cameraPosition.below())
            && !WorldUtils.isAir(cameraPosition.below(2))) {
            args.set(0, new GoalBlock(cameraPosition.below()));
         } else {
            args.set(0, new GoalBlock(cameraPosition));
         }
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static FreecamModule schizo$freecam() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(FreecamModule.class);
   }
}
