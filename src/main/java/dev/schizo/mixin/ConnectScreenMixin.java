/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

   @Inject(method = "connect", at = @At("HEAD"))
   private void recordServerConnection(
      Minecraft minecraft, ServerAddress address, ServerData serverData, TransferState transferState, CallbackInfo callback
   ) {
      ServerData resolvedServerData = serverData;
      if (resolvedServerData == null) {
         resolvedServerData = new ServerData("Schizo", address.getHost(), Type.OTHER);
      }

      ClientAccess.eventBus
         .post(new ConnectScreenConnectEvent(resolvedServerData.ip));
      ClientContext.serverStateManager
         .setServerData(resolvedServerData);
   }
}
