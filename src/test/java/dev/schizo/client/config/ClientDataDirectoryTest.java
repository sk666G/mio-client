/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientDataDirectoryTest {
   @TempDir
   Path directory;

   @Test
   void migratesRegularLegacyFilesIntoTheNewDirectory() throws IOException {
      Path legacy = this.directory.resolve("mio-fabric");
      Path destination = this.directory.resolve("schizo");
      Files.createDirectories(legacy);
      Files.writeString(legacy.resolve("settings.json"), "{\"enabled\":true}");

      assertEquals(destination, ClientDataDirectory.migrateLegacy(destination));
      assertEquals("{\"enabled\":true}", Files.readString(destination.resolve("settings.json")));
      assertTrue(Files.exists(legacy.resolve("settings.json")));
   }

   @Test
   void refusesToMigrateSymbolicLinksAndLeavesDestinationAbsent() throws IOException {
      Path legacy = this.directory.resolve("mio-fabric");
      Path destination = this.directory.resolve("schizo");
      Path outside = this.directory.resolve("outside.txt");
      Files.createDirectories(legacy);
      Files.writeString(outside, "outside");
      Files.createSymbolicLink(legacy.resolve("linked.txt"), outside);

      assertThrows(UncheckedIOException.class, () -> ClientDataDirectory.migrateLegacy(destination));
      assertFalse(Files.exists(destination));
      assertEquals("outside", Files.readString(outside));
   }
}
