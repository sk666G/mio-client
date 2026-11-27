package dev.schizo.client.gametest.mixin;

import dev.schizo.client.gametest.StartupTrace;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputProbe {
   @Inject(method = "tick", at = @At("HEAD"))
   private void observeFirstCall(CallbackInfo callback) {
      StartupTrace.observe(StartupTrace.Hook.INPUT_TICK);
   }
}
