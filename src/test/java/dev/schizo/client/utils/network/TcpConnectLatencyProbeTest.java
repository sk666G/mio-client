package dev.schizo.client.utils.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TcpConnectLatencyProbeTest {
   private static final InetSocketAddress SERVER = new InetSocketAddress("127.0.0.1", 25565);

   @Test
   void measuresConnectDurationAndClosesTheSocket() {
      FakeSocket socket = new FakeSocket();
      AtomicInteger clockReads = new AtomicInteger();
      long[] times = {100L, 25_000_100L};

      int result = TcpConnectLatencyProbe.measure(
         SERVER,
         1500,
         () -> socket,
         () -> times[clockReads.getAndIncrement()]
      );

      assertEquals(25, result);
      assertTrue(socket.closed);
      assertEquals(SERVER, socket.connectedAddress);
      assertEquals(1500, socket.timeoutMillis);
   }

   @Test
   void returnsZeroAndClosesTheSocketWhenConnectFails() {
      FakeSocket socket = new FakeSocket();
      socket.failure = new IOException("connection refused");

      int result = TcpConnectLatencyProbe.measure(
         SERVER,
         1500,
         () -> socket,
         () -> 1L
      );

      assertEquals(0, result);
      assertTrue(socket.closed);
   }

   @Test
   void skipsInvalidTargetsWithoutCreatingASocket() {
      AtomicInteger creations = new AtomicInteger();

      assertEquals(0, TcpConnectLatencyProbe.measure(
         null,
         1500,
         () -> {
            creations.incrementAndGet();
            return new FakeSocket();
         },
         () -> 0L
      ));
      assertEquals(0, TcpConnectLatencyProbe.measure(
         SERVER,
         0,
         () -> {
            creations.incrementAndGet();
            return new FakeSocket();
         },
         () -> 0L
      ));

      assertEquals(0, creations.get());
   }

   @Test
   void clampsNegativeClockDeltasToZero() {
      FakeSocket socket = new FakeSocket();
      AtomicInteger clockReads = new AtomicInteger();
      long[] times = {100L, 50L};

      assertEquals(0, TcpConnectLatencyProbe.measure(
         SERVER,
         1000,
         () -> socket,
         () -> times[clockReads.getAndIncrement()]
      ));
      assertTrue(socket.closed);
   }

   private static final class FakeSocket extends Socket {
      private SocketAddress connectedAddress;
      private int timeoutMillis;
      private IOException failure;
      private boolean closed;

      @Override
      public void connect(SocketAddress endpoint, int timeout) throws IOException {
         this.connectedAddress = endpoint;
         this.timeoutMillis = timeout;
         if (this.failure != null) {
            throw this.failure;
         }
      }

      @Override
      public synchronized void close() {
         this.closed = true;
      }
   }
}
