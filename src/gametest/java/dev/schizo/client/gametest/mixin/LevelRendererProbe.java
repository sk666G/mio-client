package dev.schizo.client.gametest.mixin;

import dev.schizo.client.gametest.StartupTrace;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererProbe {
   @Inject(method = "renderLevel", at = @At("HEAD"))
   private void observeFirstCall(CallbackInfo callback) {
      StartupTrace.observe(StartupTrace.Hook.WORLD_RENDER);
   }
}
