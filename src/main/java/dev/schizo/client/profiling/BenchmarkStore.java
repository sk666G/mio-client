package dev.schizo.client.profiling;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.schizo.client.config.BackedUpConfigWriter;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.config.ConfigPathGuard;
import dev.schizo.client.config.ConfigurationBackupManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Fixed local slots, bounded reads and backup-before-replace writes. */
public final class BenchmarkStore {
   public enum Slot { A, B }
   private static final Gson GSON = new Gson();
   private static final int MAX_BYTES = 16 * 1024;
   private final Path root;

   public BenchmarkStore(Path root) { this.root = root; }

   private static String name(Slot slot) {
      return switch (slot) {
         case A -> "benchmark-a.json";
         case B -> "benchmark-b.json";
      };
   }

   public BenchmarkSession load(Slot slot) throws IOException {
      Path target = ConfigPathGuard.resolveForRead(root, name(slot));
      if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return null;
      try {
         BenchmarkSession result = GSON.fromJson(ConfigFileReader.readUtf8(target, MAX_BYTES), BenchmarkSession.class);
         if (result == null) throw new IOException("Missing benchmark summary");
         return result;
      } catch (JsonParseException | IllegalArgumentException failure) {
         throw new IOException("Invalid benchmark summary", failure);
      }
   }

   public void save(Slot slot, BenchmarkSession session) throws IOException {
      if (session == null) throw new IllegalArgumentException("No completed benchmark");
      String json = GSON.toJson(session);
      if (json.length() > MAX_BYTES) throw new IOException("Benchmark summary exceeds size limit");
      String name = name(slot);
      Path target = ConfigPathGuard.resolveForWrite(root, name);
      BackedUpConfigWriter.writeAfterBackup(target, json, () -> new ConfigurationBackupManager(root).backupBeforeSave(name));
   }
}
