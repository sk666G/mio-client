/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
   @Mutable
   @Accessor("shift")
   void schizo$setSneak(boolean shift);

   @Mutable
   @Accessor("sprint")
   void schizo$setSprint(boolean sprint);

   @Mutable
   @Accessor("jump")
   void schizo$setJump(boolean jump);

   @Mutable
   @Accessor("forward")
   void schizo$setForward(boolean forward);

   @Mutable
   @Accessor("backward")
   void schizo$setBack(boolean backward);

   @Mutable
   @Accessor("left")
   void schizo$setLeft(boolean left);

   @Mutable
   @Accessor("right")
   void schizo$setRight(boolean right);
}
