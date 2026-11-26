/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.gui.screens.DisconnectConfirmationScreen;
import dev.schizo.client.modules.exploit.IllegalDisconnectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
   @Unique
   private static boolean disconnectConfirmed;

   protected PauseScreenMixin(Component title) {
      super(title);
   }

   // Mojang leaves the disconnect lambda unnamed.
   @Inject(method = "method_72129()V", at = @At("HEAD"), cancellable = true)
   private void confirmOrInterceptDisconnect(CallbackInfo callback) {
      Minecraft client = Minecraft.getInstance();
      AntiQuitModule antiQuitModule = MixinModuleRegistry.getModule(AntiQuitModule.class);
      IllegalDisconnectModule illegalDisconnectModule = MixinModuleRegistry.getModule(IllegalDisconnectModule.class);
      if (antiQuitModule != null
         && antiQuitModule.isToggled()
         && antiQuitModule.disconnectSetting.getValue()
         && !disconnectConfirmed) {
         client.setScreen(new DisconnectConfirmationScreen(() -> {
            disconnectConfirmed = true;
            client.getReportingContext().draftReportHandled(client, this, () -> this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
         }));
         callback.cancel();
      } else {
         if (illegalDisconnectModule != null
            && illegalDisconnectModule.isToggled()
            && client.player != null
            && !client.isLocalServer()) {
            illegalDisconnectModule.requestDisconnect();
            callback.cancel();
         }
      }
   }
}
