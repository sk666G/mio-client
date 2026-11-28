/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Safely migrates a legacy child directory without following symbolic links or replacing data. */
public final class LegacyDirectoryMigration {
   private LegacyDirectoryMigration() {
   }

   public static Result moveDirectory(Path root, String sourceName, String targetName) throws IOException {
      Path normalizedRoot = root.toAbsolutePath().normalize();
      validateChildName(sourceName);
      validateChildName(targetName);
      if (sourceName.equals(targetName)) {
         throw new IllegalArgumentException("Source and target directory names must differ");
      }
      if (Files.isSymbolicLink(normalizedRoot)
         || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Migration root is not a real directory");
      }

      Path source = normalizedRoot.resolve(sourceName).normalize();
      Path target = normalizedRoot.resolve(targetName).normalize();
      if (!normalizedRoot.equals(source.getParent()) || !normalizedRoot.equals(target.getParent())) {
         throw new IllegalArgumentException("Migration paths must be direct children of the root");
      }
      if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
         return Result.SOURCE_MISSING;
      }
      if (Files.isSymbolicLink(source) || !Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
         return Result.UNSAFE_SOURCE;
      }
      if (containsUnsafeEntry(source)) {
         return Result.UNSAFE_SOURCE;
      }
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
         return Result.TARGET_EXISTS;
      }

      try {
         // No REPLACE_EXISTING or ATOMIC_MOVE: the provider must not replace a target created
         // between the existence check and this move.
         Files.move(source, target);
         return Result.MOVED;
      } catch (FileAlreadyExistsException targetCreatedConcurrently) {
         return Result.TARGET_EXISTS;
      }
   }

   private static boolean containsUnsafeEntry(Path source) throws IOException {
      boolean[] unsafe = {false};
      Files.walkFileTree(source, new SimpleFileVisitor<>() {
         @Override
         public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
               unsafe[0] = true;
               return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
               unsafe[0] = true;
               return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
         }
      });
      return unsafe[0];
   }

   private static void validateChildName(String name) {
      if (name == null || !name.matches("[A-Za-z0-9_-]{1,64}")) {
         throw new IllegalArgumentException("Migration directory name is invalid");
      }
   }

   public enum Result {
      MOVED,
      SOURCE_MISSING,
      TARGET_EXISTS,
      UNSAFE_SOURCE
   }
}
