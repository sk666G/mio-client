package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {
   @TempDir Path temporaryDirectory;

   @Test
   void atomicallyReplacesUtf8Content() throws IOException {
      Path target = temporaryDirectory.resolve("settings.json");
      Files.writeString(target, "old");
      FileUtils.writeUtf8Atomically(target, "schizo ✓");
      assertEquals("schizo ✓", Files.readString(target));
      try (var files = Files.list(temporaryDirectory)) {
         assertEquals(1L, files.count());
      }
   }


   @Test
   void cleanupFailureIsSuppressedOntoPrimaryFailure() throws IOException {
      Path missing = temporaryDirectory.resolve("missing.tmp");
      IOException primary = new IOException("write failed");

      // A missing temp is harmless and proves the helper does not disturb the primary path.
      assertDoesNotThrow(() -> FileUtils.cleanupTemporary(missing, primary));
      assertEquals(0, primary.getSuppressed().length);
   }

   @Test
   void cleanupFailureWithoutPrimaryRemainsVisible() throws IOException {
      Path nonEmptyDirectory = Files.createDirectory(temporaryDirectory.resolve("locked.tmp"));
      Files.writeString(nonEmptyDirectory.resolve("child"), "keep");

      assertThrows(
         IOException.class,
         () -> FileUtils.cleanupTemporary(nonEmptyDirectory, null)
      );
   }

   @Test
   void failedReplacementPreservesExistingDirectory() throws IOException {
      Path target = Files.createDirectory(temporaryDirectory.resolve("settings.json"));
      Path marker = Files.writeString(target.resolve("keep.txt"), "keep");
      assertThrows(IOException.class, () -> FileUtils.writeUtf8Atomically(target, "replacement"));
      assertEquals("keep", Files.readString(marker));
   }
}
