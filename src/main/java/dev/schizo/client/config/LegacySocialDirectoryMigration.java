package dev.schizo.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.slf4j.Logger;

public final class LegacySocialDirectoryMigration {
   public enum Result {
      MOVED,
      MISSING,
      SYMBOLIC_LINK,
      DESTINATION_EXISTS,
      UNEXPECTED_ENTRY,
      FAILED
   }

   private LegacySocialDirectoryMigration() {
   }

   public static Result migrate(Path legacyDirectory, Path destination, Logger logger) {
      if (Files.isSymbolicLink(legacyDirectory)) {
         logger.warn("Skipping legacy social migration because {} is a symbolic link", legacyDirectory);
         return Result.SYMBOLIC_LINK;
      }
      if (!Files.exists(legacyDirectory, LinkOption.NOFOLLOW_LINKS)) {
         return Result.MISSING;
      }
      if (!Files.isDirectory(legacyDirectory, LinkOption.NOFOLLOW_LINKS)) {
         logger.warn("Skipping legacy social migration; {} is not a directory", legacyDirectory);
         return Result.UNEXPECTED_ENTRY;
      }
      if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
         logger.warn(
            "Could not migrate legacy social data from {} because destination {} already exists",
            legacyDirectory, destination
         );
         return Result.DESTINATION_EXISTS;
      }

      try {
         Files.move(legacyDirectory, destination);
         return Result.MOVED;
      } catch (IOException failure) {
         logger.error("Could not migrate legacy social data from {} to {}", legacyDirectory, destination, failure);
         return Result.FAILED;
      }
   }
}
