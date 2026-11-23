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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetDataPathTest {
   @TempDir
   Path directory;

   @Test
   void resolvesAValidPresetNameInsideItsDirectory() throws IOException {
      Path expected = this.directory.toAbsolutePath().normalize().resolve("combat.json");
      assertEquals(expected, PresetData.resolvePresetPath(this.directory, "combat"));
   }

   @Test
   void rejectsNamesThatCouldEscapeOrConfuseThePresetDirectory() {
      String[] invalidNames = {
         "", " ", ".", "..", "../outside", "..\\outside",
         "nested/preset", "nested\\preset", "trailing.", "trailing ",
         "bad:name", "bad*name", "bad?name", "line\nbreak"
      };

      for (String invalidName : invalidNames) {
         assertThrows(IOException.class, () -> PresetData.resolvePresetPath(this.directory, invalidName), invalidName);
      }
   }

   @Test
   void rejectsASymbolicLinkAsThePresetDirectory() throws IOException {
      Path linkedDirectory = this.directory.resolve("linked-directory");
      Files.createSymbolicLink(linkedDirectory, this.directory);
      assertThrows(IOException.class, () -> PresetData.resolvePresetPath(linkedDirectory, "combat"));
   }

   @Test
   void rejectsASymbolicLinkAsThePresetFile() throws IOException {
      Path linkedFile = this.directory.resolve("combat.json");
      Files.createSymbolicLink(linkedFile, this.directory.resolve("outside.json"));
      assertThrows(IOException.class, () -> PresetData.resolvePresetPath(this.directory, "combat"));
   }
}
