package com.jagrosh.discordipc.entities.pipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Packet;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DiscordIpcTransportCancellationTest {
   @Test
   void cancellingClientAbortsABlockedUnixHandshakeRead() throws Exception {
      assumeFalse(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"));

      Path socketPath = Files.createTempDirectory("schizo-discord-ipc").resolve("discord-ipc-test");
      ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
         Thread thread = new Thread(task, "Discord-Ipc-CancellationTest");
         thread.setDaemon(true);
         return thread;
      });

      try (ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
         server.bind(UnixDomainSocketAddress.of(socketPath));
         Future<SocketChannel> accepted = executor.submit(server::accept);
         IPCClient client = new IPCClient(1055222411819241572L);
         UnixPipe pipe = new UnixPipe(client, new HashMap<>(), socketPath.toFile());
         SocketChannel peer = accepted.get(2, TimeUnit.SECONDS);
         try {
            client.trackConnectingPipe(pipe);
            Future<Packet> blockedRead = executor.submit(pipe::read);

            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), client::cancelConnect);
            assertEquals(Packet.OpCode.CLOSE, blockedRead.get(2, TimeUnit.SECONDS).getOp());
         } finally {
            peer.close();
         }
      } finally {
         executor.shutdownNow();
         Files.deleteIfExists(socketPath);
      }
   }
}
