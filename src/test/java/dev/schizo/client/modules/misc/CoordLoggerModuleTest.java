package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.options.DimensionType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoordLoggerModuleTest {
   private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");

   @TempDir Path temporaryDirectory;


   @Test
   void coordinateSnapshotUsesNumericStateAndCanBeCleared() {
      CoordLoggerModule module = new CoordLoggerModule();
      module.cacheCoordinates(12, 64, -34, DimensionType.OVERWORLD);

      CoordLoggerModule.CoordinateSnapshot snapshot = module.snapshotCoordinates();

      assertNotNull(snapshot);
      assertEquals(12, snapshot.x());
      assertEquals(64, snapshot.y());
      assertEquals(-34, snapshot.z());
      assertEquals(DimensionType.OVERWORLD, snapshot.dimension());

      module.clearCachedCoordinates();
      assertNull(module.snapshotCoordinates());
   }

   @Test
   void moduleLifecycleClearsCachedCoordinates() {
      CoordLoggerModule module = new CoordLoggerModule();
      module.cacheCoordinates(1, 2, 3, DimensionType.THE_NETHER);
      module.onDisable();
      assertNull(module.snapshotCoordinates());

      module.cacheCoordinates(4, 5, 6, DimensionType.THE_END);
      module.onEnable();
      assertNull(module.snapshotCoordinates());
   }

   @Test
   void oversizedLogIsPreservedInsteadOfLoadedOrRewritten() throws IOException {
      Path log = temporaryDirectory.resolve("coordlogger.txt");
      byte[] contents = new byte[(int)CoordLoggerModule.MAX_COORDINATE_LOG_BYTES + 1];
      contents[0] = 'x';
      contents[contents.length - 1] = 'y';
      Files.write(log, contents);
      CoordLoggerModule module = new CoordLoggerModule();

      module.appendRetainedEntry(log, currentEntry("Disconnect"));

      assertEquals(contents.length, Files.size(log));
      byte[] after = Files.readAllBytes(log);
      assertEquals('x', after[0]);
      assertEquals('y', after[after.length - 1]);
   }

   @Test
   void malformedUtf8LogIsPreservedWhenRetentionReadFails() throws IOException {
      Path log = temporaryDirectory.resolve("coordlogger.txt");
      byte[] invalidUtf8 = new byte[]{(byte)0xC3, (byte)0x28, (byte)'\n'};
      Files.write(log, invalidUtf8);
      CoordLoggerModule module = new CoordLoggerModule();

      module.appendRetainedEntry(log, currentEntry("Disconnect"));

      assertArrayEquals(invalidUtf8, Files.readAllBytes(log));
   }

   @Test
   void legacyNonblankLinesSurviveRetentionAlongsideNewEntry() throws IOException {
      Path log = temporaryDirectory.resolve("coordlogger.txt");
      Files.writeString(log, "legacy coordinate note\n", StandardCharsets.UTF_8);
      CoordLoggerModule module = new CoordLoggerModule();
      String entry = currentEntry("Death");

      module.appendRetainedEntry(log, entry);

      String text = Files.readString(log, StandardCharsets.UTF_8);
      assertTrue(text.contains("legacy coordinate note"));
      assertTrue(text.contains(entry));
   }

   @Test
   void recognizedExpiredEntriesAreStillPruned() throws IOException {
      Path log = temporaryDirectory.resolve("coordlogger.txt");
      String expired = "X: 1, Y: 2, Z: 3 in The Overworld | Death | "
         + LocalDateTime.now().minusDays(8).format(FORMATTER);
      Files.writeString(log, expired + System.lineSeparator(), StandardCharsets.UTF_8);
      CoordLoggerModule module = new CoordLoggerModule();
      String current = currentEntry("Disconnect");

      module.appendRetainedEntry(log, current);

      String text = Files.readString(log, StandardCharsets.UTF_8);
      assertFalse(text.contains(expired));
      assertTrue(text.contains(current));
   }

   private static String currentEntry(String reason) {
      return "X: 10, Y: 64, Z: 20 in The Overworld | " + reason + " | "
         + LocalDateTime.now().format(FORMATTER);
   }
}
