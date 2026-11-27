package dev.schizo.client.gametest.mixin;

import dev.schizo.client.gametest.StartupTrace;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenProbe {
   @Inject(method = "render", at = @At("HEAD"))
   private void observeFirstCall(CallbackInfo callback) {
      StartupTrace.observe(StartupTrace.Hook.TITLE_RENDER);
   }
}
