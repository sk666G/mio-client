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
   void partialWriteFailurePreservesThePreviousFileAndRemovesTheTemporaryFile() throws IOException {
      Path target = Files.writeString(temporaryDirectory.resolve("chunks.data"), "original");
      assertThrows(IOException.class, () -> FileUtils.writeAtomically(target, temporary -> {
         Files.writeString(temporary, "partial");
         throw new IOException("injected write failure");
      }));
      assertEquals("original", Files.readString(target));
      try (var files = Files.list(temporaryDirectory)) {
         assertEquals(1L, files.count());
      }
   }

   @Test
   void failedReplacementPreservesExistingDirectory() throws IOException {
      Path target = Files.createDirectory(temporaryDirectory.resolve("settings.json"));
      Path marker = Files.writeString(target.resolve("keep.txt"), "keep");
      assertThrows(IOException.class, () -> FileUtils.writeUtf8Atomically(target, "replacement"));
      assertEquals("keep", Files.readString(marker));
   }
}
