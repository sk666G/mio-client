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
import dev.schizo.client.modules.misc.CustomDeathTextModule;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {
   private static CustomDeathTextModule customDeathTextModule() {
      return ClientContext.requireModule(CustomDeathTextModule.class);
   }
   @Mutable
   @Shadow
   @Final
   private Component causeOfDeath;

   @Inject(method = "<init>", at = @At("RETURN"))
   public void applyCustomDeathText(
      Component deathMessage, boolean hardcore, LocalPlayer localPlayer, CallbackInfo callback
   ) {
      if (customDeathTextModule() != null
         && customDeathTextModule().isToggled()) {
         this.causeOfDeath = customDeathTextModule().getDeathText();
      }
   }
}
