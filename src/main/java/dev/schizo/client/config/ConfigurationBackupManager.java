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

/** Bounded, atomic backups for safe Schizo JSON configuration paths under the data root. */
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
      this.requireSafeConfigParent(source);
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

      Path configBackupDirectory = this.ensureBackupDirectory(configName);
      long timestamp = this.nextTimestampMillis(configName);
      String fileName = this.configFileName(configName) + "." + String.format(Locale.ROOT, "%013d", timestamp)
         + "-" + UUID.randomUUID().toString().substring(0, 8) + ".bak";
      Path target = this.resolveBackup(configName, fileName);
      Path temporary = Files.createTempFile(configBackupDirectory, ".config-backup-", ".tmp");
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
      this.requireSafeConfigParent(source);
      this.requireRegularFile(source, "Configuration");
      long size = Files.size(source);
      if (size > MAX_CONFIG_BYTES) {
         throw new IOException("Configuration exceeds the backup size limit");
      }

      Path configBackupDirectory = this.ensureBackupDirectory(configName);
      long timestamp = this.nextTimestampMillis(configName);
      String fileName = this.configFileName(configName) + "." + String.format(Locale.ROOT, "%013d", timestamp)
         + "-" + UUID.randomUUID().toString().substring(0, 8) + ".bak";
      Path target = this.resolveBackup(configName, fileName);
      Path temporary = Files.createTempFile(configBackupDirectory, ".config-backup-", ".tmp");
      try {
         Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
         this.moveAtomically(temporary, target, false);
      } finally {
         Files.deleteIfExists(temporary);
      }

      this.pruneBackups(configName);
      return this.toRecord(target);
   }

   /** Inspects one registered config file without exposing or logging its contents. */
   public synchronized ConfigDiagnostic inspectConfig(String configName) {
      final Path file;
      try {
         file = this.resolveConfig(configName);
         if (Files.isSymbolicLink(this.root) || !Files.isDirectory(this.root, LinkOption.NOFOLLOW_LINKS)) {
            return new ConfigDiagnostic(configName, ConfigStatus.UNSAFE, -1L, "Configuration root is not a real directory");
         }
         if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return new ConfigDiagnostic(configName, ConfigStatus.MISSING, 0L, "File does not exist");
         }
         this.requireSafeConfigParent(file);
         if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return new ConfigDiagnostic(configName, ConfigStatus.UNSAFE, -1L, "Not a regular file");
         }

         long size = Files.size(file);
         if (size > MAX_CONFIG_BYTES) {
            return new ConfigDiagnostic(configName, ConfigStatus.TOO_LARGE, size, "File exceeds 8 MiB");
         }

         byte[] contents;
         try (InputStream input = Files.newInputStream(file)) {
            contents = input.readNBytes((int)MAX_CONFIG_BYTES + 1);
         } catch (IOException readFailure) {
            return new ConfigDiagnostic(configName, ConfigStatus.UNREADABLE, size, "File could not be read");
         }
         if (contents.length > MAX_CONFIG_BYTES) {
            return new ConfigDiagnostic(configName, ConfigStatus.TOO_LARGE, size, "File exceeds 8 MiB");
         }

         try {
            JsonElement json = JsonParser.parseString(new String(contents, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) {
               return new ConfigDiagnostic(configName, ConfigStatus.CORRUPT, size, "JSON root is not an object");
            }
            return new ConfigDiagnostic(configName, ConfigStatus.VALID, size, "Valid JSON object");
         } catch (RuntimeException malformed) {
            return new ConfigDiagnostic(configName, ConfigStatus.CORRUPT, size, "Invalid JSON");
         }
      } catch (IllegalArgumentException failure) {
         return new ConfigDiagnostic(configName, ConfigStatus.UNSAFE, -1L, "Invalid configuration name");
      } catch (IOException failure) {
         return new ConfigDiagnostic(configName, ConfigStatus.UNREADABLE, -1L, "File could not be inspected");
      }
   }

   public synchronized List<BackupRecord> listBackups(String configName) throws IOException {
      this.resolveConfig(configName);
      Path configBackupDirectory = this.backupDirectoryFor(configName);
      if (!Files.exists(configBackupDirectory, LinkOption.NOFOLLOW_LINKS)) {
         return List.of();
      }
      this.requireRealBackupDirectory(configName);

      List<BackupRecord> backups = new ArrayList<>();
      try (DirectoryStream<Path> entries = Files.newDirectoryStream(configBackupDirectory)) {
         for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (!name.matches(this.backupNamePattern(configName))
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
      this.requireSafeConfigParent(destination);
      this.requireRealBackupDirectory(configName);
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
      if (configName == null || configName.isBlank()) {
         throw new IllegalArgumentException("Invalid configuration file name");
      }

      Path relative;
      try {
         relative = Path.of(configName);
      } catch (RuntimeException invalidPath) {
         throw new IllegalArgumentException("Invalid configuration file name", invalidPath);
      }
      if (relative.isAbsolute() || relative.getNameCount() < 1 || relative.getNameCount() > 8
         || !relative.equals(relative.normalize())) {
         throw new IllegalArgumentException("Invalid configuration file name");
      }

      for (int index = 0; index < relative.getNameCount(); index++) {
         String component = relative.getName(index).toString();
         boolean finalComponent = index == relative.getNameCount() - 1;
         String pattern = finalComponent
            ? "[A-Za-z0-9_-][A-Za-z0-9._-]{0,123}\\.json"
            : "[A-Za-z0-9_-][A-Za-z0-9._-]{0,63}";
         if (!component.matches(pattern) || component.contains("..")) {
            throw new IllegalArgumentException("Invalid configuration file name");
         }
      }

      Path target = this.root.resolve(relative).normalize();
      if (!target.startsWith(this.root) || !this.root.relativize(target).equals(relative)) {
         throw new IllegalArgumentException("Configuration path escaped its root");
      }
      return target;
   }

   private String configFileName(String configName) {
      return this.root.relativize(this.resolveConfig(configName)).getFileName().toString();
   }

   private Path backupDirectoryFor(String configName) {
      Path relative = this.root.relativize(this.resolveConfig(configName));
      Path parent = relative.getParent();
      Path target = parent == null ? this.backupDirectory : this.backupDirectory.resolve(parent).normalize();
      if (!target.startsWith(this.backupDirectory)) {
         throw new IllegalArgumentException("Backup path escaped its directory");
      }
      return target;
   }

   private Path resolveBackup(String configName, String backupFileName) throws IOException {
      this.resolveConfig(configName);
      if (backupFileName == null || !backupFileName.matches(this.backupNamePattern(configName))) {
         throw new IOException("Invalid backup file name");
      }
      Path directory = this.backupDirectoryFor(configName);
      Path target = directory.resolve(backupFileName).normalize();
      if (!target.getParent().equals(directory)) {
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

   private Path ensureBackupDirectory(String configName) throws IOException {
      this.requireRootDirectory();
      Path target = this.backupDirectoryFor(configName);
      Path current = this.root;
      for (Path component : this.root.relativize(target)) {
         current = current.resolve(component);
         if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(current);
         }
         if (Files.isSymbolicLink(current)
            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Configuration backup path is not a real directory");
         }
      }
      return target;
   }

   private void requireRealBackupDirectory(String configName) throws IOException {
      Path target = this.backupDirectoryFor(configName);
      if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration backup path does not exist");
      }
      Path current = this.root;
      for (Path component : this.root.relativize(target)) {
         current = current.resolve(component);
         if (Files.isSymbolicLink(current)
            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Configuration backup path is not a real directory");
         }
      }
   }

   private void requireSafeConfigParent(Path config) throws IOException {
      Path parent = config.getParent();
      if (parent == null || !parent.startsWith(this.root)) {
         throw new IOException("Configuration parent escaped its root");
      }
      Path current = this.root;
      for (Path component : this.root.relativize(parent)) {
         current = current.resolve(component);
         if (Files.isSymbolicLink(current)
            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Configuration parent is not a real directory");
         }
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
      int marker = fileName.indexOf(".json.");
      if (marker >= 0) {
         int timestampStart = marker + ".json.".length();
         int dash = fileName.indexOf('-', timestampStart);
         if (dash > timestampStart) {
            try {
               timestamp = Long.parseLong(fileName.substring(timestampStart, dash));
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

   private String backupNamePattern(String configName) {
      return java.util.regex.Pattern.quote(this.configFileName(configName))
         + "\\.[0-9]{13}-[0-9a-f]{8}\\.bak";
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

   public enum ConfigStatus {
      VALID,
      MISSING,
      CORRUPT,
      TOO_LARGE,
      UNSAFE,
      UNREADABLE
   }

   public record ConfigDiagnostic(String configName, ConfigStatus status, long sizeBytes, String detail) {
   }
}
