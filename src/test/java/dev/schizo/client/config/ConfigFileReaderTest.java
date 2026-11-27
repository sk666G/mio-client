package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileReaderTest {
   @TempDir
   Path temporary;

   @Test
   void readsRegularUtf8Files() throws IOException {
      Path file = Files.writeString(temporary.resolve("modules.json"), "{\"ok\": \"✓\"}");
      assertEquals("{\"ok\": \"✓\"}", ConfigFileReader.readUtf8(file, 1024));
   }

   @Test
   void acceptsAFileExactlyAtTheLimit() throws IOException {
      String value = "a".repeat(64);
      Path file = Files.writeString(temporary.resolve("config.json"), value, StandardCharsets.UTF_8);
      assertEquals(value, ConfigFileReader.readUtf8(file, 64));
   }

   @Test
   void rejectsMalformedUtf8InsideAnOtherwiseValidString() throws IOException {
      Path file = Files.write(temporary.resolve("invalid.json"), new byte[]{'"', (byte)0xc3, '"'});
      assertThrows(IOException.class, () -> ConfigFileReader.readUtf8(file, 3));
   }

   @Test
   void acceptsAnEncodedReplacementCharacter() throws IOException {
      Path file = Files.writeString(temporary.resolve("valid.json"), "\uFFFD");
      assertEquals("\uFFFD", ConfigFileReader.readUtf8(file, 3));
   }

   @Test
   void rejectsAFileLargerThanTheLimit() throws IOException {
      Path file = Files.writeString(temporary.resolve("large.json"), "12345");
      assertThrows(IOException.class, () -> ConfigFileReader.readUtf8(file, 4));
   }

   @Test
   void rejectsSymbolicLinksWithoutReadingTheirTargets() throws IOException {
      Path target = Files.writeString(temporary.resolve("target.json"), "{\"outside\": true}");
      Path link = temporary.resolve("linked.json");
      try {
         Files.createSymbolicLink(link, target);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         return;
      }
      assertThrows(IOException.class, () -> ConfigFileReader.readUtf8(link, 1024));
   }

   @Test
   void rejectsDirectoriesAndMissingPaths() throws IOException {
      Path directory = Files.createDirectory(temporary.resolve("folder"));
      Path missing = temporary.resolve("missing.json");
      assertThrows(IOException.class, () -> ConfigFileReader.readUtf8(directory, 1024));
      assertThrows(IOException.class, () -> ConfigFileReader.readUtf8(missing, 1024));
   }

   @Test
   void rejectsInvalidSizeLimits() throws IOException {
      Path file = Files.writeString(temporary.resolve("config.json"), "{}");
      assertThrows(IllegalArgumentException.class, () -> ConfigFileReader.readUtf8(file, -1));
      assertThrows(IllegalArgumentException.class,
         () -> ConfigFileReader.readUtf8(file, Integer.MAX_VALUE));
   }
}
