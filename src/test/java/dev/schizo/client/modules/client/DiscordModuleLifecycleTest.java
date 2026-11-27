package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiscordModuleLifecycleTest {
   @Test
   void completedConnectionIsOwnedOnlyByCurrentEnabledGeneration() {
      Object future = new Object();
      Object client = new Object();

      assertTrue(
         DiscordModule.ownsCompletedConnection(
            future, future, client, client, true
         )
      );
      assertFalse(
         DiscordModule.ownsCompletedConnection(
            future, future, client, client, false
         )
      );
   }

   @Test
   void staleFutureOrReplacedClientDoesNotOwnLateConnection() {
      Object oldFuture = new Object();
      Object currentFuture = new Object();
      Object oldClient = new Object();
      Object currentClient = new Object();

      assertFalse(
         DiscordModule.ownsCompletedConnection(
            oldFuture, currentFuture, oldClient, oldClient, true
         )
      );
      assertFalse(
         DiscordModule.ownsCompletedConnection(
            currentFuture, currentFuture, oldClient, currentClient, true
         )
      );
   }

   @Test
   void nullCompletionCannotBecomeOwned() {
      Object future = new Object();
      Object client = new Object();

      assertFalse(
         DiscordModule.ownsCompletedConnection(
            future, future, null, client, true
         )
      );
   }
}
