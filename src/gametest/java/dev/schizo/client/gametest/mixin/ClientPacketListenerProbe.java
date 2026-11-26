package dev.schizo.client.gametest.mixin;

import dev.schizo.client.gametest.StartupTrace;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerProbe {
   @Inject(method = "handleLogin", at = @At("HEAD"))
   private void observeFirstCall(CallbackInfo callback) {
      StartupTrace.observe(StartupTrace.Hook.PLAY_LOGIN);
   }
}
