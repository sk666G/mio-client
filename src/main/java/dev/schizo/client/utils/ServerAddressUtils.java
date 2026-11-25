/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import dev.schizo.client.diagnostics.RuntimeDiagnostics;
import dev.schizo.client.ClientAccess;
import java.net.SocketAddress;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

public final class ServerAddressUtils {
   private static final String fallbackServerAddress;

   static {
      fallbackServerAddress = "singleplayer";
   }

   private ServerAddressUtils() {}

   public static String getCurrentServerAddress() {
      String address = null;

      try {
         ClientPacketListener packetListener = ClientAccess.schizoMinecraft.getConnection();
         Connection connection = packetListener.getConnection();
         SocketAddress remoteAddress = connection.getRemoteAddress();
         address = remoteAddress.toString();

         int separatorIndex = address.indexOf(':');
         if (separatorIndex > 0) {
            address = address.substring(0, separatorIndex);
         }

         while (address.endsWith(".")) {
            address = address.substring(0, address.length() - 1);
         }
      } catch (RuntimeException addressFailure) {
         RuntimeDiagnostics.record(
            RuntimeDiagnostics.Area.INTEGRATION,
            "Server address detection",
            addressFailure
         );
      }

      return address != null && !address.isEmpty() ? address : fallbackServerAddress;
   }
}
