package dev.schizo.client.gametest.mixin;

import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientContext.class, remap = false)
abstract class ClientContextLookupProbe {
   @Shadow public static volatile ModuleRegistry moduleRegistry;

   @Inject(method = "getModule", at = @At("HEAD"))
   private static void checkPublishedRegistry(Class<?> type, CallbackInfoReturnable<Module> callback) {
      if (moduleRegistry == null) {
         throw new AssertionError("ClientContext.getModule before publication: " + type.getName());
      }
   }
}
