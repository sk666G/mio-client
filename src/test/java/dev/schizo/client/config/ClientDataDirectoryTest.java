package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientDataDirectoryTest {
   @TempDir Path temporaryDirectory;

   @Test
   void copiesNestedRegularFilesAndSkipsSymbolicLinks() throws IOException {
      Path legacy = Files.createDirectory(temporaryDirectory.resolve("mio-fabric"));
      Path nested = Files.createDirectories(legacy.resolve("nested"));
      Files.writeString(nested.resolve("settings.json"), "settings");
      Path outside = Files.writeString(temporaryDirectory.resolve("outside.txt"), "outside");
      try {
         Files.createSymbolicLink(nested.resolve("outside-link"), outside);
      } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
         org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links are unavailable");
      }

      Path destination = temporaryDirectory.resolve("schizo");
      ClientDataDirectory.migrateLegacy(destination);
      assertEquals("settings", Files.readString(destination.resolve("nested/settings.json")));
      assertFalse(Files.exists(destination.resolve("nested/outside-link"), java.nio.file.LinkOption.NOFOLLOW_LINKS));
      assertEquals("outside", Files.readString(outside));
   }

   @Test
   void leavesExistingDestinationAndLegacyDataAlone() throws IOException {
      Path legacy = Files.createDirectory(temporaryDirectory.resolve("mio-fabric"));
      Files.writeString(legacy.resolve("old.txt"), "old");
      Path destination = Files.createDirectory(temporaryDirectory.resolve("schizo"));
      Files.writeString(destination.resolve("current.txt"), "current");

      assertEquals(destination, ClientDataDirectory.migrateLegacy(destination));
      assertFalse(Files.exists(destination.resolve("old.txt")));
      assertTrue(Files.exists(legacy.resolve("old.txt")));
   }

   @Test
   void rejectsLegacyRootSymlinkWithoutCreatingStagingData() throws IOException {
      Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
      Path destination = temporaryDirectory.resolve("schizo");
      try {
         Files.createSymbolicLink(temporaryDirectory.resolve("mio-fabric"), outside);
      } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
         org.junit.jupiter.api.Assumptions.abort("Symbolic links are unavailable");
      }

      assertThrows(java.io.UncheckedIOException.class, () -> ClientDataDirectory.migrateLegacy(destination));
      try (Stream<Path> files = Files.list(temporaryDirectory)) {
         assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".schizo-migration-")));
      }
   }
}
