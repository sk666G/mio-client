package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class LegacySocialDirectoryMigrationTest {
   @TempDir Path temporaryDirectory;

   @Test
   void movesDirectoryWithoutReplacingExistingData() throws IOException {
      Path legacy = Files.createDirectory(temporaryDirectory.resolve("autoez"));
      Files.writeString(legacy.resolve("message.txt"), "preserve me");
      Path destination = temporaryDirectory.resolve("killsay");

      assertEquals(
         LegacySocialDirectoryMigration.Result.MOVED,
         LegacySocialDirectoryMigration.migrate(legacy, destination, LoggerFactory.getLogger(getClass()))
      );
      assertEquals("preserve me", Files.readString(destination.resolve("message.txt")));
      assertFalse(Files.exists(legacy));
   }

   @Test
   void existingDestinationLeavesBothTreesUntouched() throws IOException {
      Path legacy = Files.createDirectory(temporaryDirectory.resolve("autoez"));
      Files.writeString(legacy.resolve("legacy.txt"), "legacy");
      Path destination = Files.createDirectory(temporaryDirectory.resolve("killsay"));
      Files.writeString(destination.resolve("current.txt"), "current");

      assertEquals(
         LegacySocialDirectoryMigration.Result.DESTINATION_EXISTS,
         LegacySocialDirectoryMigration.migrate(legacy, destination, LoggerFactory.getLogger(getClass()))
      );
      assertTrue(Files.exists(legacy.resolve("legacy.txt")));
      assertEquals("current", Files.readString(destination.resolve("current.txt")));
   }

   @Test
   void symbolicLinkRootIsNotFollowed() throws IOException {
      Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
      Files.writeString(outside.resolve("private.txt"), "outside");
      Path legacy = temporaryDirectory.resolve("autoez");
      try {
         Files.createSymbolicLink(legacy, outside);
      } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
         org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links are unavailable");
      }

      Path destination = temporaryDirectory.resolve("killsay");
      assertEquals(
         LegacySocialDirectoryMigration.Result.SYMBOLIC_LINK,
         LegacySocialDirectoryMigration.migrate(legacy, destination, LoggerFactory.getLogger(getClass()))
      );
      assertFalse(Files.exists(destination));
      assertEquals("outside", Files.readString(outside.resolve("private.txt")));
   }

   @Test
   void rejectsRegularFileAsUnexpectedLegacyEntry() throws IOException {
      Path legacy = Files.writeString(temporaryDirectory.resolve("autoez"), "not a directory");

      assertEquals(
         LegacySocialDirectoryMigration.Result.UNEXPECTED_ENTRY,
         LegacySocialDirectoryMigration.migrate(
            legacy, temporaryDirectory.resolve("killsay"), LoggerFactory.getLogger(getClass())
         )
      );
      assertEquals("not a directory", Files.readString(legacy));
   }

   @Test
   void reportsMoveFailureAndPreservesSource() throws IOException {
      Path legacy = Files.createDirectory(temporaryDirectory.resolve("autoez"));
      Files.writeString(legacy.resolve("legacy.txt"), "keep");
      Path blockedParent = Files.writeString(temporaryDirectory.resolve("not-a-directory"), "block");
      Path destination = blockedParent.resolve("killsay");

      assertEquals(
         LegacySocialDirectoryMigration.Result.FAILED,
         LegacySocialDirectoryMigration.migrate(legacy, destination, LoggerFactory.getLogger(getClass()))
      );
      assertEquals("keep", Files.readString(legacy.resolve("legacy.txt")));
   }
}
