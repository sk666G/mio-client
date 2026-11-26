package dev.schizo.client.utils.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BlockBreakTrackerSessionBoundaryTest {
   @Test
   void connectAndDisconnectBothInvalidateQueuedSessionWorkAndClearEntries() {
      BlockBreakTracker tracker = new BlockBreakTracker();
      try {
         long initialGeneration = tracker.sessionGeneration();
         tracker.getEntries().add(new BlockBreakEntry(7, BlockPos.ZERO, false));

         tracker.onMinecraftDisconnectEvent(new MinecraftDisconnectEvent());

         assertEquals(initialGeneration + 1L, tracker.sessionGeneration());
         assertTrue(tracker.getEntries().isEmpty());

         tracker.getEntries().add(new BlockBreakEntry(8, BlockPos.ZERO, false));
         tracker.onConnectScreenConnectEvent(new ConnectScreenConnectEvent("example.invalid"));

         assertEquals(initialGeneration + 2L, tracker.sessionGeneration());
         assertTrue(tracker.getEntries().isEmpty());
      } finally {
         ClientAccess.eventBus.unsubscribe(tracker);
      }
   }
}
