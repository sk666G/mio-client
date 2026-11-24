package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyDirectoryMigrationTest {
   @TempDir
   Path temporary;

   @Test
   void movesAValidLegacyDirectoryAndItsFiles() throws IOException {
      Path source = temporary.resolve("autoez");
      Files.createDirectories(source.resolve("nested"));
      Files.writeString(source.resolve("nested/message.txt"), "hello");

      assertEquals(
         LegacyDirectoryMigration.Result.MOVED,
         LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "killsay")
      );

      assertFalse(Files.exists(source));
      assertEquals("hello", Files.readString(temporary.resolve("killsay/nested/message.txt")));
   }

   @Test
   void leavesTheLegacyDirectoryWhenTheDestinationAlreadyExists() throws IOException {
      Path source = Files.createDirectory(temporary.resolve("autoez"));
      Path target = Files.createDirectory(temporary.resolve("killsay"));
      Files.writeString(source.resolve("legacy.txt"), "legacy");
      Files.writeString(target.resolve("current.txt"), "current");

      assertEquals(
         LegacyDirectoryMigration.Result.TARGET_EXISTS,
         LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "killsay")
      );

      assertTrue(Files.exists(source.resolve("legacy.txt")));
      assertEquals("current", Files.readString(target.resolve("current.txt")));
   }

   @Test
   void reportsMissingLegacySourceWithoutCreatingTheDestination() throws IOException {
      assertEquals(
         LegacyDirectoryMigration.Result.SOURCE_MISSING,
         LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "killsay")
      );

      assertFalse(Files.exists(temporary.resolve("killsay")));
   }

   @Test
   void refusesASymbolicLinkSource() throws IOException {
      Path outside = Files.createDirectory(temporary.resolve("outside"));
      Path link = temporary.resolve("autoez");
      try {
         Files.createSymbolicLink(link, outside);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         return;
      }

      assertEquals(
         LegacyDirectoryMigration.Result.UNSAFE_SOURCE,
         LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "killsay")
      );
      assertTrue(Files.isSymbolicLink(link));
      assertFalse(Files.exists(temporary.resolve("killsay")));
   }

   @Test
   void refusesDirectoriesContainingSymbolicLinks() throws IOException {
      Path source = Files.createDirectory(temporary.resolve("autoez"));
      Path outside = Files.writeString(temporary.resolve("outside.txt"), "outside");
      Path link = source.resolve("linked.txt");
      try {
         Files.createSymbolicLink(link, outside);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         return;
      }

      assertEquals(
         LegacyDirectoryMigration.Result.UNSAFE_SOURCE,
         LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "killsay")
      );
      assertTrue(Files.isSymbolicLink(link));
      assertFalse(Files.exists(temporary.resolve("killsay")));
   }

   @Test
   void rejectsUnsafeChildNamesAndInvalidRoots() throws IOException {
      assertThrows(IllegalArgumentException.class,
         () -> LegacyDirectoryMigration.moveDirectory(temporary, "../outside", "killsay"));
      assertThrows(IllegalArgumentException.class,
         () -> LegacyDirectoryMigration.moveDirectory(temporary, "autoez", "autoez"));
      Path file = Files.writeString(temporary.resolve("not-a-directory"), "x");
      assertThrows(IOException.class,
         () -> LegacyDirectoryMigration.moveDirectory(file, "autoez", "killsay"));
   }
}
