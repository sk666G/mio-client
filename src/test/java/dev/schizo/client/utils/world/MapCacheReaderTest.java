package dev.schizo.client.utils.world;

import static org.junit.jupiter.api.Assertions.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapCacheReaderTest {
   @TempDir Path directory;

   private Path writeCache(int count, int payloadLength) throws IOException {
      Path path = directory.resolve("cache.data");
      try (var output = new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(path)))) {
         output.writeInt(count);
         output.writeUTF("map");
         output.writeByte(3);
         output.write(new byte[payloadLength]);
      }
      return path;
   }

   @Test
   void readsLegacyDeflatedRecordsAndRequiresTheExactPayload() throws IOException {
      Path path = writeCache(1, 16384);
      assertEquals(16387, MapCacheReader.read(path, (scale, colors) -> scale + colors.length).get("map").intValue());
      for (int length : new int[]{16383, 16385}) {
         writeCache(1, length);
         assertThrows(IOException.class, () -> MapCacheReader.read(path, (scale, colors) -> colors));
      }
   }

   @Test
   void rejectsImpossibleCountsBeforeDecoding() throws IOException {
      for (int count : new int[]{-1, MapCacheReader.MAX_MAPS + 1}) {
         Path path = writeCache(count, 0);
         assertThrows(IOException.class, () -> MapCacheReader.read(path, (scale, colors) -> fail("Unexpected decode")));
      }
   }
}
