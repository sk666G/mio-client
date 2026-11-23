/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsAtomicWriteTest {
   @TempDir
   Path directory;

   @Test
   void createsAndReplacesUtf8Files() throws IOException {
      Path target = this.directory.resolve("settings.json");

      FileUtils.writeUtf8Atomically(target, "first");
      assertEquals("first", Files.readString(target));

      FileUtils.writeUtf8Atomically(target, "second");
      assertEquals("second", Files.readString(target));
   }

   @Test
   void refusesToReplaceASymbolicLink() throws IOException {
      Path target = this.directory.resolve("settings.json");
      Files.createSymbolicLink(target, this.directory.resolve("outside.json"));

      assertThrows(IOException.class, () -> FileUtils.writeUtf8Atomically(target, "unsafe"));
   }

   @Test
   void refusesToWriteThroughASymbolicLinkDirectory() throws IOException {
      Path linkedDirectory = this.directory.resolve("linked-directory");
      Files.createSymbolicLink(linkedDirectory, this.directory);

      assertThrows(IOException.class,
         () -> FileUtils.writeUtf8Atomically(linkedDirectory.resolve("settings.json"), "unsafe"));
   }
}
