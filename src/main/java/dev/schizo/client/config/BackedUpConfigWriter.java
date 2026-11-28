package dev.schizo.client.config;

import dev.schizo.client.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Prevents replacing live configuration when its required pre-save backup failed. */
public final class BackedUpConfigWriter {
   private BackedUpConfigWriter() {
   }

   @FunctionalInterface
   public interface BackupOperation {
      void run() throws IOException;
   }

   public static void writeAfterBackup(
      Path target,
      String serializedJson,
      BackupOperation backup
   ) throws IOException {
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(serializedJson, "serializedJson");
      Objects.requireNonNull(backup, "backup");

      try {
         backup.run();
      } catch (IOException | IllegalArgumentException backupFailure) {
         throw new IOException(
            "Refusing to replace configuration because its pre-save backup failed",
            backupFailure
         );
      }

      FileUtils.writeUtf8Atomically(target, serializedJson);
   }
}
