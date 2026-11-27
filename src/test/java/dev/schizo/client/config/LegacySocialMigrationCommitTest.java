package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacySocialMigrationCommitTest {
   @TempDir
   Path directory;

   @Test
   void commitCreatesSocialsThenDeletesLegacyFile() throws Exception {
      Path legacy = this.directory.resolve("friends.json");
      Path socials = this.directory.resolve("socials.json");
      Files.writeString(legacy, "legacy");

      LegacySocialMigration.commitMigration(legacy, socials, "{\"socials\":[]}");

      assertFalse(Files.exists(legacy));
      assertEquals("{\"socials\":[]}", Files.readString(socials));
   }

   @Test
   void commitNeverReplacesConcurrentSocialsFileOrDeletesLegacyFile() throws Exception {
      Path legacy = this.directory.resolve("friends.json");
      Path socials = this.directory.resolve("socials.json");
      Files.writeString(legacy, "legacy");
      Files.writeString(socials, "newer");

      assertThrows(FileAlreadyExistsException.class,
         () -> LegacySocialMigration.commitMigration(legacy, socials, "{\"socials\":[\"migrated\"]}"));

      assertEquals("newer", Files.readString(socials));
      assertEquals("legacy", Files.readString(legacy));
   }
}
