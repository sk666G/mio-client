package dev.schizo.client.gametest.mixin;

import dev.schizo.client.gametest.StartupTrace;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftProbe {
   @Inject(method = "setScreen", at = @At("HEAD"))
   private void observeFirstCall(CallbackInfo callback) {
      StartupTrace.observe(StartupTrace.Hook.GUI_SCREEN);
   }
}
