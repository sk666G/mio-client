/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.config.PresetData;
import dev.schizo.client.config.PresetNameValidator;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

public final class PresetRegistry extends Registry<PresetData> implements ClientAccess {
   private final PresetCategory category;
   private final Path directory;

   public PresetRegistry(PresetCategory category) {
      this.directory = category.getDirectory().toAbsolutePath().normalize();
      this.category = category;
   }

   public boolean applyPreset(String name) {
      final String normalized;
      try {
         normalized = PresetNameValidator.normalize(name);
      } catch (IllegalArgumentException invalidName) {
         return false;
      }
      synchronized (this.getRegistry()) {
         PresetData preset = this.findPreset(normalized);
         if (preset == null) return false;
         this.category.fromJson(preset.contents());
         return true;
      }
   }

   public PresetData getPreset(String name) {
      final String normalized;
      try {
         normalized = PresetNameValidator.normalize(name);
      } catch (IllegalArgumentException invalidName) {
         return null;
      }
      synchronized (this.getRegistry()) {
         return this.findPreset(normalized);
      }
   }

   private PresetData findPreset(String name) {
      for (PresetData candidate : this.entries) {
         if (candidate.getName().equalsIgnoreCase(name)) return candidate;
      }
      return null;
   }

   public void restorePreset(PresetData preset) throws IOException {
      this.restorePreset(preset, preset.getName());
   }

   public void restorePreset(PresetData preset, String replacedName) throws IOException {
      Path restoredPath = PresetNameValidator.resolve(this.directory, preset.getName());
      Path replacedPath = PresetNameValidator.resolve(this.directory, replacedName);
      if (Files.exists(restoredPath, LinkOption.NOFOLLOW_LINKS)
         && !Files.isRegularFile(restoredPath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Preset path is not a regular file: " + restoredPath.getFileName());
      }
      byte[] previousBytes = Files.exists(restoredPath, LinkOption.NOFOLLOW_LINKS)
         ? Files.readAllBytes(restoredPath) : null;
      preset.saveToDirectory(this.directory);
      try {
         if (!replacedPath.equals(restoredPath) && Files.exists(replacedPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(replacedPath);
         }
      } catch (IOException failure) {
         try {
            if (previousBytes == null) Files.deleteIfExists(restoredPath);
            else FileUtils.writeUtf8Atomically(restoredPath, new String(previousBytes, java.nio.charset.StandardCharsets.UTF_8));
         } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
         }
         throw failure;
      }
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(preset.getName()));
         this.entries.add(preset);
      }
   }

   public void deletePresetForUndo(String name) throws IOException {
      String normalized = PresetNameValidator.normalize(name);
      Path path = PresetNameValidator.resolve(this.directory, normalized);
      if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
         if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Preset path is not a regular file: " + path.getFileName());
         }
         Files.delete(path);
      }
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(normalized));
      }
   }

   public void renamePresetForUndo(String sourceName, String targetName, PresetData previousTarget, PresetData source) throws IOException {
      if (source == null) throw new IOException("Preset no longer exists: " + sourceName);
      String normalizedSource = PresetNameValidator.normalize(sourceName);
      String normalizedTarget = PresetNameValidator.normalize(targetName);
      Path sourcePath = PresetNameValidator.resolve(this.directory, normalizedSource);
      Path targetPath = PresetNameValidator.resolve(this.directory, normalizedTarget);
      if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Preset file no longer exists: " + sourcePath.getFileName());
      }
      ensureNoCollision(normalizedTarget, normalizedSource);
      Files.move(sourcePath, targetPath);
      try {
         if (previousTarget != null) previousTarget.saveToDirectory(this.directory);
      } catch (IOException failure) {
         try {
            Files.move(targetPath, sourcePath);
         } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
         }
         throw failure;
      }
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(normalizedSource)
            || candidate.getName().equalsIgnoreCase(normalizedTarget));
         this.entries.add(source);
         if (previousTarget != null) this.entries.add(previousTarget);
      }
   }

   public void savePreset(String name) throws IOException {
      String normalized = this.canonicalExistingName(PresetNameValidator.normalize(name));
      Path path = PresetNameValidator.resolve(this.directory, normalized);
      ensureNoCollision(normalized, normalized);
      PresetData preset = new PresetData(normalized, this.category.toJson().getAsJsonObject());
      preset.saveToDirectory(this.directory);
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(normalized));
         this.entries.add(preset);
      }
   }

   public boolean deletePreset(String name) throws IOException {
      String normalized = PresetNameValidator.normalize(name);
      Path path = PresetNameValidator.resolve(this.directory, normalized);
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Preset file does not exist: " + path.getFileName());
      }
      Files.delete(path);
      synchronized (this.getRegistry()) {
         return this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(normalized));
      }
   }

   public boolean renamePreset(String oldName, String newName) throws IOException {
      String normalizedOld = PresetNameValidator.normalize(oldName);
      String normalizedNew = PresetNameValidator.normalize(newName);
      if (normalizedOld.equalsIgnoreCase(normalizedNew)) return false;

      PresetData source = this.getPreset(normalizedOld);
      if (source == null) throw new IOException("Preset does not exist: " + normalizedOld);
      Path sourcePath = PresetNameValidator.resolve(this.directory, normalizedOld);
      Path targetPath = PresetNameValidator.resolve(this.directory, normalizedNew);
      if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Preset file does not exist: " + sourcePath.getFileName());
      }
      ensureNoCollision(normalizedNew, normalizedOld);
      Files.move(sourcePath, targetPath);
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(normalizedOld));
         this.entries.add(new PresetData(normalizedNew, source.contents()));
      }
      return true;
   }

   public void loadPresets() {
      synchronized (this.getRegistry()) {
         this.entries.clear();
         try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory)) {
            for (Path file : files) {
               String fileName = file.getFileName().toString();
               if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                  || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) == 0L) continue;
               String rawName = fileName.substring(0, fileName.length() - 5);
               try {
                  String name = PresetNameValidator.normalize(rawName);
                  JsonObject contents = JsonParser.parseString(FileUtils.readUtf8(file)).getAsJsonObject();
                  if (this.findPreset(name) == null) this.entries.add(new PresetData(name, contents));
               } catch (RuntimeException | IOException invalidPreset) {
                  ClientAccess.logger.warn("Skipping invalid preset file {}", file.getFileName(), invalidPreset);
               }
            }
         } catch (IOException directoryFailure) {
            ClientAccess.logger.error("Could not read preset directory {}", this.directory, directoryFailure);
         }
      }
   }

   public boolean copyPreset(String sourceName, String targetName) throws IOException {
      String normalizedSource = PresetNameValidator.normalize(sourceName);
      String normalizedTarget = PresetNameValidator.normalize(targetName);
      PresetData source = this.getPreset(normalizedSource);
      if (source == null) return false;
      ensureNoCollision(normalizedTarget, null);
      Path targetPath = PresetNameValidator.resolve(this.directory, normalizedTarget);
      FileUtils.writeUtf8Atomically(targetPath, ClientAccess.gson.toJson(source.contents()));
      synchronized (this.getRegistry()) {
         this.entries.add(new PresetData(normalizedTarget, source.contents().deepCopy()));
      }
      return true;
   }

   private String canonicalExistingName(String requested) throws IOException {
      synchronized (this.getRegistry()) {
         PresetData existing = this.findPreset(requested);
         if (existing != null) return existing.getName();
      }
      if (!Files.isDirectory(this.directory)) return requested;
      try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory)) {
         for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
               String existing = fileName.substring(0, fileName.length() - 5);
               if (existing.equalsIgnoreCase(requested)) return existing;
            }
         }
      }
      return requested;
   }

   private void ensureNoCollision(String name, String ignoredName) throws IOException {
      Path target = PresetNameValidator.resolve(this.directory, name);
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
         && (ignoredName == null || !name.equalsIgnoreCase(ignoredName))) {
         throw new FileAlreadyExistsException(target.toString());
      }
      if (!Files.isDirectory(this.directory)) return;
      try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory)) {
         for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) continue;
            String existing = fileName.substring(0, fileName.length() - 5);
            if (existing.equalsIgnoreCase(name) && (ignoredName == null || !existing.equalsIgnoreCase(ignoredName))) {
               throw new FileAlreadyExistsException(file.toString());
            }
         }
      }
      synchronized (this.getRegistry()) {
         for (PresetData existing : this.entries) {
            if (existing.getName().equalsIgnoreCase(name)
               && (ignoredName == null || !existing.getName().equalsIgnoreCase(ignoredName))) {
               throw new FileAlreadyExistsException(target.toString());
            }
         }
      }
   }

   public void savePresetByExactName(String name) throws IOException {
      String normalized = PresetNameValidator.normalize(name);
      PresetData preset = this.getPreset(normalized);
      if (preset == null) throw new IOException("Preset does not exist: " + normalized);
      preset.saveToDirectory(this.directory);
   }

   public void forEachPreset(Consumer consumer) {
      synchronized (this.getRegistry()) {
         this.getRegistry().forEach(consumer);
      }
   }

   public void saveAllPresets() {
      synchronized (this.getRegistry()) {
         for (PresetData preset : this.entries) {
            try {
               preset.saveToDirectory(this.directory);
            } catch (IOException failure) {
               ClientAccess.logger.error("Could not save preset {}", preset.getName(), failure);
            }
         }
      }
   }
}
