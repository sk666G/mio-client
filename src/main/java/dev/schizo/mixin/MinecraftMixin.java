/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.bootstrap.MixinModuleRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.modules.combat.ArrowsModule;
import dev.schizo.client.modules.combat.AutoClickerModule;
import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.player.AutoEatModule;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.screens.ConfirmGameCloseScreen;
import dev.schizo.client.events.ScreenChangeEvent;
import dev.schizo.client.events.MinecraftTickEvent;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.combat.NoHitDelayModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.client.UIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
   @Unique
   private Screen previousScreen;
   @Shadow
   public int missTime;
   @Shadow
   @Nullable
   public Screen screen;
   private static boolean shutdownSaveStarted = false;

   @Inject(method = "startAttack", at = @At("HEAD"))
   public void resetAttackMissTime(CallbackInfoReturnable<Boolean> callback) {
      if (MixinModuleRegistry.minecraftNoHitDelayModule.isToggled()
         || MixinModuleRegistry.minecraftAutoClickerModule.isToggled()) {
         this.missTime = 0;
      }
   }

   @Inject(method = "destroy", at = @At("HEAD"), require = 1)
   public void saveConfigurationOnDestroy(CallbackInfo callback) {
      if (!shutdownSaveStarted
         && ClientContext.configurationManager != null) {
         shutdownSaveStarted = true;
         if (ClientContext.taskScheduler != null) {
            ClientContext.taskScheduler.shutdown();
         }
         try {
            if (ClientContext.contextProfileRegistry != null
               && !ClientContext.contextProfileRegistry.restoreBeforeShutdown()) {
               ClientAccess.logger.error(
                  "Skipping shutdown configuration save to protect the user's pre-profile settings"
               );
               return;
            }
            try {
               MixinModuleRegistry.minecraftUiModule.disable();
            } catch (RuntimeException | Error uiShutdownFailure) {
               ClientAccess.logger.error("Could not cleanly close the Schizo UI during shutdown", uiShutdownFailure);
            }
            ClientContext.configurationManager.save();
         } finally {
            ClientAccess.executorService.shutdown();
         }
      }
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;close()V"), require = 1)
   public void postDisconnectEvent(
      Screen screen, boolean ignoredDisconnectFlag1, boolean ignoredDisconnectFlag2, CallbackInfo callback
   ) {
      MinecraftDisconnectEvent event = new MinecraftDisconnectEvent();
      ClientAccess.eventBus
         .post(event);
   }

   @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
   public boolean bypassUsingItemForContinueAttack(boolean originalUsingItem) {
      return !MixinModuleRegistry.minecraftMultiTaskModule.isToggled() && originalUsingItem;
   }

   @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"), require = 0)
   public boolean bypassDestroyingForStartUseItem(boolean originalDestroying) {
      return MixinModuleRegistry.minecraftMultiTaskModule.isToggled() ? false : originalDestroying;
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 1)
   private void postBeforeScreenChange(Screen newScreen, CallbackInfo callback) {
      this.previousScreen = this.screen;
      ScreenChangeEvent.Before event = new ScreenChangeEvent.Before(
         this.previousScreen, newScreen
      );
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V", shift = Shift.AFTER), cancellable = true, require = 1)
   private void postAfterScreenInit(Screen newScreen, CallbackInfo callback) {
      ScreenChangeEvent.AfterInit event = new ScreenChangeEvent.AfterInit(
         this.previousScreen, newScreen
      );
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = "tick", at = @At("HEAD"), require = 1)
   private void postMinecraftTick(CallbackInfo callback) {
      ClientAccess.eventBus
         .post(
            MinecraftTickEvent.INSTANCE
         );
   }

   @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
   private boolean bypassUsingItemForAutoEat(boolean originalUsingItem) {
      return MixinModuleRegistry.minecraftAutoEatModule.isEating()
            && MixinModuleRegistry.minecraftAutoEatModule.isToggled()
         ? false
         : originalUsingItem;
   }

   @WrapWithCondition(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;stop()V"))
   private boolean confirmGameClose(Minecraft client) {
      if (MixinModuleRegistry.minecraftAntiQuitModule.isToggled()
         && MixinModuleRegistry.minecraftAntiQuitModule.gameCloseSetting.getValue()) {
         if (!(client.screen instanceof ConfirmGameCloseScreen)) {
            client.setScreen(new ConfirmGameCloseScreen());
         }

         return false;
      } else {
         return true;
      }
   }

   @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = Shift.AFTER), require = 1)
   private void postMinecraftRunTick(
      boolean renderLevel, CallbackInfo callback, @Local int ticksToRun
   ) {
      ClientAccess.eventBus
         .post(new MinecraftRunTickEvent(ticksToRun >= 1));
   }

   @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
   private boolean bypassUsingItemForArrows(boolean originalUsingItem) {
      return MixinModuleRegistry.minecraftArrowsModule.shouldBypassUsingItemCheck()
         ? false
         : originalUsingItem;
   }

   @Inject(method = "startUseItem", at = @At("HEAD"))
   private void completeCrystalCalculationBeforeUse(CallbackInfo callback) {
      MixinModuleRegistry.minecraftAutoCrystalModule.completeCalculation();
   }

   @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
   private boolean bypassHandsBusyForStartUseItem(boolean originalHandsBusy) {
      return MixinModuleRegistry.minecraftMultiTaskModule.isToggled() ? false : originalHandsBusy;
   }

   @ModifyExpressionValue(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
   private boolean bypassHandsBusyForStartAttack(boolean originalHandsBusy) {
      return MixinModuleRegistry.minecraftMultiTaskModule.isToggled() ? false : originalHandsBusy;
   }

   @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
   private void suppressEntityGlow(
      Entity entity, CallbackInfoReturnable<Boolean> callback
   ) {
      if (MixinModuleRegistry.minecraftNoRenderModule.isToggled()
         && MixinModuleRegistry.minecraftNoRenderModule.glowSetting.getValue()) {
         callback.setReturnValue(false);
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   public void closeDiscordClient(CallbackInfo callback) {
      MixinModuleRegistry.minecraftDiscordModule.closeClient();
   }

}
