package com.jagrosh.discordipc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

class IPCClientCancellationTest {
   @Test
   void cancelledConnectionCannotStartOrContinue() {
      IPCClient client = new IPCClient(1055222411819241572L);

      client.cancelConnect();

      assertThrows(CancellationException.class, client::checkConnectActive);
      assertThrows(CancellationException.class, () -> client.connect());
   }

   @Test
   void cancellingAnIdleClientIsSafeAndIdempotent() {
      IPCClient client = new IPCClient(1055222411819241572L);

      client.cancelConnect();
      client.cancelConnect();

      assertThrows(CancellationException.class, client::checkConnectActive);
   }
}
