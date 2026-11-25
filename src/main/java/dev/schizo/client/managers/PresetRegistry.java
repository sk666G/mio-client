/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.config.PresetNameValidator;
import dev.schizo.client.config.JsonStateTransaction;

import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.FileUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TreeMap;
import java.nio.file.DirectoryIteratorException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PresetRegistry extends Registry<PresetData> {
   private static final Logger LOGGER = LoggerFactory.getLogger(PresetRegistry.class);
   private final PresetCategory category;
   private final Path directory;
   private static final int MAX_PRESET_BYTES = PresetData.MAX_PRESET_BYTES;

   public PresetRegistry(PresetCategory category) {
      this(category, category.getDirectory());
   }

   PresetRegistry(Path directory) {
      this(null, directory);
   }

   private PresetRegistry(PresetCategory category, Path directory) {
      this.directory = directory.toAbsolutePath().normalize();
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
         if (preset == null || this.category == null) return false;
         try {
            applyWithRollback(
               this.category::toJson,
               this.category::fromJson,
               preset.copyContents()
            );
            return true;
         } catch (RuntimeException failure) {
            LOGGER.error(
               "Could not apply {} preset {}; restored the previous category state when possible",
               this.category.getName(),
               preset.getName(),
               failure
            );
            return false;
         }
      }
   }

   static void applyWithRollback(
      java.util.function.Supplier<JsonElement> snapshotSupplier,
      Consumer<JsonElement> applier,
      JsonElement desiredState
   ) {
      JsonStateTransaction.apply(snapshotSupplier, applier, desiredState);
   }

   public java.util.List<String> getPresetNames() {
      synchronized (this.getRegistry()) {
         return this.entries.stream().map(PresetData::getName).toList();
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
      this.requireRealDirectory();
      Path restoredPath = PresetNameValidator.resolve(this.directory, preset.getName());
      Path replacedPath = PresetNameValidator.resolve(this.directory, replacedName);
      byte[] previousBytes = null;
      if (Files.exists(restoredPath, LinkOption.NOFOLLOW_LINKS)) {
         if (Files.isSymbolicLink(restoredPath)
            || !Files.isRegularFile(restoredPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Preset path is not a regular file: " + restoredPath.getFileName());
         }
         try (InputStream input = Files.newInputStream(restoredPath, LinkOption.NOFOLLOW_LINKS)) {
            previousBytes = input.readNBytes(MAX_PRESET_BYTES + 1);
         }
         if (previousBytes.length > MAX_PRESET_BYTES) {
            throw new IOException("Existing preset exceeds the supported size limit");
         }
      }
      preset.saveToDirectory(this.directory);
      try {
         if (!replacedPath.equals(restoredPath) && Files.exists(replacedPath, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(replacedPath)
               || !Files.isRegularFile(replacedPath, LinkOption.NOFOLLOW_LINKS)) {
               throw new IOException("Replaced preset path is not a regular file: " + replacedPath.getFileName());
            }
            Files.delete(replacedPath);
         }
      } catch (IOException failure) {
         try {
            if (previousBytes == null) Files.deleteIfExists(restoredPath);
            else FileUtils.writeBytesAtomically(restoredPath, previousBytes);
         } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
         }
         throw failure;
      }
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(preset.getName())
            || candidate.getName().equalsIgnoreCase(replacedName));
         this.entries.add(preset);
      }
   }

   public void deletePresetForUndo(String name) throws IOException {
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
         var loaded = new TreeMap<String, PresetData>(String.CASE_INSENSITIVE_ORDER);
         try {
            this.requireRealDirectory();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory)) {
               for (Path file : files) {
                  String fileName = file.getFileName().toString();
                  if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                     || Files.isSymbolicLink(file)
                     || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                  String rawName = fileName.substring(0, fileName.length() - 5);
                  if (rawName.toLowerCase(Locale.ROOT).endsWith(".json")) continue;
                  try {
                     String name = PresetNameValidator.normalize(rawName);
                     String serialized = ConfigFileReader.readUtf8(file, MAX_PRESET_BYTES);
                     if (serialized.isEmpty()) continue;
                     JsonObject contents = JsonParser.parseString(serialized).getAsJsonObject();
                     PresetData existing = loaded.get(name);
                     if (existing == null || name.compareTo(existing.getName()) < 0) {
                        loaded.put(name, new PresetData(name, contents));
                     }
                  } catch (RuntimeException | IOException invalidPreset) {
                     LOGGER.warn("Skipping invalid preset file {}", file.getFileName(), invalidPreset);
                  }
               }
            }
         } catch (IOException | DirectoryIteratorException directoryFailure) {
            LOGGER.error("Could not read preset directory {}", this.directory, directoryFailure);
            return;
         }
         this.entries.clear();
         this.entries.addAll(loaded.values());
      }
   }

   public boolean copyPreset(String sourceName, String targetName) throws IOException {
      this.requireRealDirectory();
      String normalizedSource = PresetNameValidator.normalize(sourceName);
      String normalizedTarget = PresetNameValidator.normalize(targetName);
      PresetData source = this.getPreset(normalizedSource);
      if (source == null) return false;
      ensureNoCollision(normalizedTarget, null);
      PresetData copy = new PresetData(normalizedTarget, source.copyContents());
      copy.saveToDirectory(this.directory);
      synchronized (this.getRegistry()) {
         this.entries.add(copy);
      }
      return true;
   }

   private void requireRealDirectory() throws IOException {
      Path parent = this.directory.getParent();
      if (Files.isSymbolicLink(this.directory)
         || !Files.isDirectory(this.directory, LinkOption.NOFOLLOW_LINKS)
         || (parent != null && Files.isSymbolicLink(parent))) {
         throw new IOException("Preset directory is not a safe real directory: " + this.directory);
      }
   }

   private String canonicalExistingName(String requested) throws IOException {
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
      this.requireRealDirectory();
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
         try {
            this.requireRealDirectory();
         } catch (IOException unsafeDirectory) {
            LOGGER.error("Could not save presets in unsafe directory {}", this.directory, unsafeDirectory);
            return;
         }
         for (PresetData preset : this.entries) {
            try {
               preset.saveToDirectory(this.directory);
            } catch (IOException failure) {
               LOGGER.error("Could not save preset {}", preset.getName(), failure);
            }
         }
      }
   }
}
