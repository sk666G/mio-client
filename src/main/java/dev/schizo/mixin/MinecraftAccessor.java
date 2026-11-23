/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
   @Invoker("startAttack")
   boolean attack();

   @Invoker("startUseItem")
   void interact();

   @Accessor("rightClickDelay")
   void setItemUseCooldown(int rightClickDelay);

   @Accessor("rightClickDelay")
   int getItemUseCooldown();

   @Accessor("clientLevelTeardownInProgress")
   boolean schizo$isDisconnecting();

   @Mutable
   @Accessor("mainRenderTarget")
   void schizo$setFramebuffer(RenderTarget mainRenderTarget);

   @Accessor("mainRenderTarget")
   RenderTarget schizo$getFrambuffer();
}
