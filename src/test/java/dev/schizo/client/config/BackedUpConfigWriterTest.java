package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackedUpConfigWriterTest {
   @TempDir
   Path directory;

   @Test
   void failedBackupPreservesExistingConfiguration() throws Exception {
      Path target = Files.writeString(this.directory.resolve("modules.json"), "{\"original\":true}");

      IOException failure = assertThrows(IOException.class, () ->
         BackedUpConfigWriter.writeAfterBackup(
            target,
            "{\"replacement\":true}",
            () -> { throw new IOException("backup directory is unsafe"); }
         )
      );

      assertTrue(failure.getMessage().contains("pre-save backup failed"));
      assertEquals("{\"original\":true}", Files.readString(target));
   }

   @Test
   void invalidBackupNameAlsoPreservesExistingConfiguration() throws Exception {
      Path target = Files.writeString(this.directory.resolve("socials.json"), "{\"original\":true}");

      assertThrows(IOException.class, () ->
         BackedUpConfigWriter.writeAfterBackup(
            target,
            "{\"replacement\":true}",
            () -> { throw new IllegalArgumentException("invalid backup path"); }
         )
      );

      assertEquals("{\"original\":true}", Files.readString(target));
   }

   @Test
   void successfulBackupReadsOldContentsBeforeWritingNewConfiguration() throws Exception {
      Path target = Files.writeString(this.directory.resolve("waypoints.json"), "{\"revision\":1}");
      Path backup = this.directory.resolve("saved-original.json");
      AtomicInteger backups = new AtomicInteger();

      BackedUpConfigWriter.writeAfterBackup(
         target,
         "{\"revision\":2}",
         () -> {
            backups.incrementAndGet();
            Files.copy(target, backup);
         }
      );

      assertEquals(1, backups.get());
      assertEquals("{\"revision\":1}", Files.readString(backup));
      assertEquals("{\"revision\":2}", Files.readString(target));
   }

   @Test
   void initialWriteWorksWhenBackupCheckFindsNoPriorFile() throws Exception {
      Path target = this.directory.resolve("new.json");

      BackedUpConfigWriter.writeAfterBackup(target, "{}", () -> {});

      assertEquals("{}", Files.readString(target));
   }
}
