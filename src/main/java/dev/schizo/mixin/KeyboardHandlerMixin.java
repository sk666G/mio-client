/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.modules.misc.BetterChatModule;
import dev.schizo.client.gui.screens.CommandPaletteScreen;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.InputPressEvent;
import dev.schizo.client.events.KeyboardHandlerCharTypedEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V", shift = Shift.BEFORE), cancellable = true)
   private void postKeyPressEvent(
      long windowHandle, int action, KeyEvent keyEvent, CallbackInfo callback
   ) {
      if (this.minecraft.screen == null
         && action == GLFW.GLFW_PRESS
         && keyEvent.key() == GLFW.GLFW_KEY_K
         && GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
         CommandPaletteScreen.openPalette();
         callback.cancel();
         return;
      }

      if (keyEvent.key() >= 0 && this.minecraft.screen == null && action == 1) {
         InputPressEvent event = new InputPressEvent(keyEvent.key(), false);
         ClientAccess.eventBus
            .post(event);
         if (event.isCancelled()) {
            callback.cancel();
         }
      }
   }

   @Inject(method = "charTyped", at = @At("HEAD"))
   public void postCharTypedEvent(
      long windowHandle, CharacterEvent characterEvent, CallbackInfo callback
   ) {
      if (this.minecraft.screen == null && windowHandle == this.minecraft.getWindow().handle()) {
         KeyboardHandlerCharTypedEvent event = new KeyboardHandlerCharTypedEvent(characterEvent.codepoint());
         ClientAccess.eventBus
            .post(event);
      }
   }

   @Inject(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V", shift = Shift.BEFORE))
   private void beginDebugChatClear(
      KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callback
   ) {
      BetterChatModule.clearingChatFromDebugKey = true;
   }

   @Inject(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V", shift = Shift.AFTER))
   private void endDebugChatClear(
      KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callback
   ) {
      BetterChatModule.clearingChatFromDebugKey = false;
   }
}
