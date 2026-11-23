/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Bounded, atomic backups for top-level Schizo JSON configuration files. */
public final class ConfigurationBackupManager {
   public static final int MAX_BACKUPS_PER_CONFIG = 5;
   public static final long MAX_CONFIG_BYTES = 8L * 1024L * 1024L;

   private final Path root;
   private final Path backupDirectory;
   private long lastTimestampMillis;

   public ConfigurationBackupManager(Path root) {
      this.root = root.toAbsolutePath().normalize();
      this.backupDirectory = this.root.resolve("backups").normalize();
   }

   public Path getBackupDirectory() {
      return this.backupDirectory;
   }

   /**
    * Creates a backup of the current config file before it is overwritten.
    * Returns null when no existing config file is present or the latest backup is identical.
    */
   public synchronized BackupRecord backupBeforeSave(String configName) throws IOException {
      Path source = this.resolveConfig(configName);
      this.requireRootDirectory();
      if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
         return null;
      }
      this.requireRegularFile(source, "Configuration");
      long size = Files.size(source);
      if (size > MAX_CONFIG_BYTES) {
         throw new IOException("Configuration exceeds the backup size limit");
      }

      List<BackupRecord> existing = this.listBackups(configName);
      if (!existing.isEmpty()) {
         Path latest = this.resolveBackup(configName, existing.getFirst().fileName());
         if (Files.size(latest) == size && Files.mismatch(source, latest) == -1L) {
            return null;
         }
      }

      this.ensureBackupDirectory();
      long timestamp = this.nextTimestampMillis(configName);
      String fileName = configName + "." + String.format(Locale.ROOT, "%013d", timestamp)
         + "-" + UUID.randomUUID().toString().substring(0, 8) + ".bak";
      Path target = this.resolveBackup(configName, fileName);
      Path temporary = Files.createTempFile(this.backupDirectory, ".config-backup-", ".tmp");
      try {
         Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
         this.moveAtomically(temporary, target, false);
      } finally {
         Files.deleteIfExists(temporary);
      }

      this.pruneBackups(configName);
      return this.toRecord(target);
   }

   /** Creates a manual backup even when the current file matches the latest backup. */
   public synchronized BackupRecord createBackup(String configName) throws IOException {
      Path source = this.resolveConfig(configName);
      this.requireRootDirectory();
      this.requireRegularFile(source, "Configuration");
      long size = Files.size(source);
      if (size > MAX_CONFIG_BYTES) {
         throw new IOException("Configuration exceeds the backup size limit");
      }

      this.ensureBackupDirectory();
      long timestamp = this.nextTimestampMillis(configName);
      String fileName = configName + "." + String.format(Locale.ROOT, "%013d", timestamp)
         + "-" + UUID.randomUUID().toString().substring(0, 8) + ".bak";
      Path target = this.resolveBackup(configName, fileName);
      Path temporary = Files.createTempFile(this.backupDirectory, ".config-backup-", ".tmp");
      try {
         Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
         this.moveAtomically(temporary, target, false);
      } finally {
         Files.deleteIfExists(temporary);
      }

      this.pruneBackups(configName);
      return this.toRecord(target);
   }

   public synchronized List<BackupRecord> listBackups(String configName) throws IOException {
      this.resolveConfig(configName);
      if (!Files.exists(this.backupDirectory, LinkOption.NOFOLLOW_LINKS)) {
         return List.of();
      }
      this.requireRealBackupDirectory();

      String prefix = configName + ".";
      List<BackupRecord> backups = new ArrayList<>();
      try (DirectoryStream<Path> entries = Files.newDirectoryStream(this.backupDirectory)) {
         for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (!name.startsWith(prefix) || !name.endsWith(".bak")
               || Files.isSymbolicLink(entry)
               || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
               continue;
            }
            backups.add(this.toRecord(entry));
         }
      }
      backups.sort(Comparator.comparingLong(BackupRecord::createdAtMillis).reversed()
         .thenComparing(BackupRecord::fileName, Comparator.reverseOrder()));
      return List.copyOf(backups);
   }

   /**
    * Restores a named backup atomically. The current live config is backed up first.
    * The caller should reload or restart the client after restore.
    */
   public synchronized void restoreBackup(String configName, String backupFileName) throws IOException {
      Path destination = this.resolveConfig(configName);
      Path source = this.resolveBackup(configName, backupFileName);
      this.requireRootDirectory();
      this.requireRealBackupDirectory();
      this.requireRegularFile(source, "Backup");

      long size = Files.size(source);
      if (size > MAX_CONFIG_BYTES) {
         throw new IOException("Backup exceeds the supported size limit");
      }
      this.requireJsonObject(source);

      // Preserve the current live config before replacing it with the selected backup.
      if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
         this.backupBeforeSave(configName);
      }
      if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
         && !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration target is not a regular file");
      }

      Path temporary = Files.createTempFile(this.root, ".config-restore-", ".tmp");
      try {
         Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
         this.moveAtomically(temporary, destination, true);
      } finally {
         Files.deleteIfExists(temporary);
      }
   }

   public synchronized int clearBackups(String configName) throws IOException {
      this.resolveConfig(configName);
      List<BackupRecord> backups = this.listBackups(configName);
      int deleted = 0;
      for (BackupRecord backup : backups) {
         Path path = this.resolveBackup(configName, backup.fileName());
         this.requireRegularFile(path, "Backup");
         if (Files.deleteIfExists(path)) {
            deleted++;
         }
      }
      return deleted;
   }

   private Path resolveConfig(String configName) {
      if (configName == null || !configName.matches("[A-Za-z0-9_-][A-Za-z0-9._-]{0,123}\\.json")
         || configName.contains("..")) {
         throw new IllegalArgumentException("Invalid configuration file name");
      }
      Path target = this.root.resolve(configName).normalize();
      if (!target.getParent().equals(this.root)) {
         throw new IllegalArgumentException("Configuration path escaped its root");
      }
      return target;
   }

   private Path resolveBackup(String configName, String backupFileName) throws IOException {
      this.resolveConfig(configName);
      if (backupFileName == null || !backupFileName.startsWith(configName + ".")
         || !backupFileName.endsWith(".bak")
         || backupFileName.contains("/") || backupFileName.contains("\\")
         || backupFileName.contains("..")) {
         throw new IOException("Invalid backup file name");
      }
      Path target = this.backupDirectory.resolve(backupFileName).normalize();
      if (!target.getParent().equals(this.backupDirectory)) {
         throw new IOException("Backup path escaped its directory");
      }
      return target;
   }

   private void requireRootDirectory() throws IOException {
      if (Files.isSymbolicLink(this.root)
         || !Files.isDirectory(this.root, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration root is not a real directory");
      }
   }

   private void ensureBackupDirectory() throws IOException {
      this.requireRootDirectory();
      if (!Files.exists(this.backupDirectory, LinkOption.NOFOLLOW_LINKS)) {
         Files.createDirectories(this.backupDirectory);
      }
      this.requireRealBackupDirectory();
   }

   private void requireRealBackupDirectory() throws IOException {
      if (Files.isSymbolicLink(this.backupDirectory)
         || !Files.isDirectory(this.backupDirectory, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration backup path is not a real directory");
      }
   }

   private void requireRegularFile(Path file, String description) throws IOException {
      if (Files.isSymbolicLink(file)
         || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException(description + " is not a regular file");
      }
   }

   private void requireJsonObject(Path file) throws IOException {
      byte[] bytes;
      try (InputStream input = Files.newInputStream(file)) {
         bytes = input.readNBytes((int)MAX_CONFIG_BYTES + 1);
      }
      if (bytes.length > MAX_CONFIG_BYTES) {
         throw new IOException("Backup exceeds the supported size limit");
      }
      try {
         JsonElement json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
         if (!json.isJsonObject()) {
            throw new IOException("Backup must contain a JSON object");
         }
      } catch (RuntimeException malformed) {
         throw new IOException("Backup contains invalid JSON", malformed);
      }
   }

   private BackupRecord toRecord(Path backup) throws IOException {
      String fileName = backup.getFileName().toString();
      long timestamp = -1L;
      int separator = fileName.indexOf('.', fileName.indexOf('.') + 1);
      if (separator >= 0) {
         int dash = fileName.indexOf('-', separator + 1);
         if (dash > separator + 1) {
            try {
               timestamp = Long.parseLong(fileName.substring(separator + 1, dash));
            } catch (NumberFormatException ignoredInvalidTimestamp) {
               timestamp = -1L;
            }
         }
      }
      if (timestamp < 0L) {
         FileTime modified = Files.getLastModifiedTime(backup, LinkOption.NOFOLLOW_LINKS);
         timestamp = modified.toMillis();
      }
      return new BackupRecord(fileName, timestamp, Files.size(backup));
   }

   private long nextTimestampMillis(String configName) throws IOException {
      long newestExisting = 0L;
      for (BackupRecord backup : this.listBackups(configName)) {
         newestExisting = Math.max(newestExisting, backup.createdAtMillis());
      }
      this.lastTimestampMillis = Math.max(
         System.currentTimeMillis(),
         Math.max(this.lastTimestampMillis + 1L, newestExisting + 1L)
      );
      return this.lastTimestampMillis;
   }

   private void pruneBackups(String configName) throws IOException {
      List<BackupRecord> backups = this.listBackups(configName);
      for (int index = MAX_BACKUPS_PER_CONFIG; index < backups.size(); index++) {
         Path obsolete = this.resolveBackup(configName, backups.get(index).fileName());
         this.requireRegularFile(obsolete, "Backup");
         Files.deleteIfExists(obsolete);
      }
   }

   private void moveAtomically(Path source, Path target, boolean replaceExisting) throws IOException {
      try {
         if (replaceExisting) {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } else {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
         }
      } catch (AtomicMoveNotSupportedException unsupported) {
         if (replaceExisting) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
         } else {
            Files.move(source, target);
         }
      }
   }

   public record BackupRecord(String fileName, long createdAtMillis, long sizeBytes) {
   }
}
