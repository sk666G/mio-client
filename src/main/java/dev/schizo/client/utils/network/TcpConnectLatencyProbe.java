/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Measures a bounded TCP connection attempt without reading from the remote endpoint. */
public final class TcpConnectLatencyProbe {
   private TcpConnectLatencyProbe() {
   }

   public static int measure(InetSocketAddress remoteAddress, int connectTimeoutMillis) {
      return measure(remoteAddress, connectTimeoutMillis, Socket::new, System::nanoTime);
   }

   static int measure(
      InetSocketAddress remoteAddress,
      int connectTimeoutMillis,
      Supplier<? extends Socket> socketFactory,
      LongSupplier nanoClock
   ) {
      if (remoteAddress == null
         || remoteAddress.isUnresolved()
         || connectTimeoutMillis < 1) {
         return 0;
      }

      Objects.requireNonNull(socketFactory, "socketFactory");
      Objects.requireNonNull(nanoClock, "nanoClock");

      try (Socket socket = socketFactory.get()) {
         long startedAtNanos = nanoClock.getAsLong();
         socket.connect(remoteAddress, connectTimeoutMillis);
         long elapsedNanos = Math.max(0L, nanoClock.getAsLong() - startedAtNanos);
         long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
         return (int)Math.min(Integer.MAX_VALUE, elapsedMillis);
      } catch (IOException connectionFailure) {
         return 0;
      }
   }
}
