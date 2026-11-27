package dev.schizo.client.gametest.mixin;

import dev.schizo.client.bootstrap.ClientInitializer;
import dev.schizo.client.gametest.StartupTrace;
import dev.schizo.client.gametest.StartupTrace.Phase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientInitializer.class, remap = false)
abstract class ClientInitializerProbe {
   @Inject(method = "initialize", at = @At("HEAD"))
   private void mainEntered(CallbackInfo callback) {
      StartupTrace.advance(Phase.MAIN_ENTERED);
   }

   @Inject(method = "initialize", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/render/FontSupport;initialize()V", shift = At.Shift.AFTER))
   private void fontReady(CallbackInfo callback) {
      StartupTrace.advance(Phase.FONT_READY);
   }

   @Inject(method = "initialize", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/bootstrap/MixinModuleRegistry;bind(Ldev/schizo/client/managers/ModuleRegistry;)V"))
   private void registryConstructed(CallbackInfo callback) {
      StartupTrace.advance(Phase.REGISTRY_CONSTRUCTED);
   }

   @Inject(method = "initialize", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/bootstrap/MixinModuleRegistry;bind(Ldev/schizo/client/managers/ModuleRegistry;)V", shift = At.Shift.AFTER))
   private void mixinsBound(CallbackInfo callback) {
      StartupTrace.advance(Phase.MIXINS_BOUND);
   }

   @Inject(method = "initialize", at = @At("RETURN"))
   private void mainComplete(CallbackInfo callback) {
      StartupTrace.advance(Phase.MAIN_COMPLETE);
   }

   @Inject(method = "initializeClient", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/bootstrap/ModuleDependencies;bind(Ldev/schizo/client/managers/ModuleRegistry;)V", shift = At.Shift.AFTER))
   private void dependenciesBound(CallbackInfo callback) {
      StartupTrace.advance(Phase.DEPENDENCIES_BOUND);
   }

   @Inject(method = "initializeClient", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/events/bus/EventBus;post(Ljava/lang/Object;)Ljava/lang/Object;"))
   private void clientEvent(CallbackInfo callback) {
      StartupTrace.advance(Phase.CLIENT_EVENT);
   }

   @Inject(method = "initializeClient", at = @At(value = "INVOKE",
      target = "Ldev/schizo/client/managers/ConfigurationManager;load()V"))
   private void configLoading(CallbackInfo callback) {
      StartupTrace.advance(Phase.CONFIG_LOADING);
   }

   @Inject(method = "initializeClient", at = @At("RETURN"))
   private void clientComplete(CallbackInfo callback) {
      StartupTrace.advance(Phase.CLIENT_COMPLETE);
   }
}
