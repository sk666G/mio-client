package dev.schizo.client.utils.world;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class ChunkScannerLifecycleTest {
   @Test
   void stoppedOrWorldlessScannerDoesNotEnumerateLoadedChunks() {
      ChunkScanner scanner = new ChunkScanner(
         0,
         (chunkPos, chunk) -> fail("chunk callback must not run without a world"),
         null,
         null
      );

      assertDoesNotThrow(scanner::scanLoadedChunks);

      scanner.start();
      try {
         assertDoesNotThrow(scanner::scanLoadedChunks);
      } finally {
         scanner.stop();
      }
   }
}
