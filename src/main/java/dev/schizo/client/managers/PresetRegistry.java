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

import dev.schizo.client.ClientAccess;
import dev.schizo.client.managers.Registry;
import dev.schizo.client.settings.options.PresetCategory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.function.Consumer;

public final class PresetRegistry extends Registry<PresetData> implements ClientAccess {
   private final PresetCategory category;
   private final Path directory;

   public PresetRegistry(PresetCategory category) {
      this.directory = category.getDirectory();
      this.category = category;
   }

   public boolean applyPreset(String name) {
      synchronized (this.getRegistry()) {
         Iterator<PresetData> presets = this.getRegistry().iterator();
         while (presets.hasNext()) {
            PresetData preset = presets.next();
            String presetName = preset.getName();
            if (presetName.equalsIgnoreCase(name)) {
               PresetCategory category = this.category;
               JsonObject contents = preset.contents();
               category.fromJson(contents);
               return true;
            }
         }
         return false;
      }
   }

   public PresetData getPreset(String name) {
      synchronized (this.getRegistry()) {
         for (PresetData preset : this.entries) {
            if (preset.getName().equalsIgnoreCase(name)) {
               return preset;
            }
         }
         return null;
      }
   }

   public void restorePreset(PresetData preset) throws IOException {
      this.restorePreset(preset, preset.getName());
   }

   public void restorePreset(PresetData preset, String replacedName) throws IOException {
      Path replacedPath = PresetData.resolvePresetPath(this.directory, replacedName);
      Path restoredPath = PresetData.resolvePresetPath(this.directory, preset.getName());
      this.writePresetForUndo(preset);
      if (!replacedName.equals(preset.getName()) && Files.exists(replacedPath, LinkOption.NOFOLLOW_LINKS)
         && !replacedPath.equals(restoredPath)) {
         Files.delete(replacedPath);
      }

      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(preset.getName()));
         this.entries.add(preset);
      }
   }

   private void writePresetForUndo(PresetData preset) throws IOException {
      preset.saveToDirectory(this.directory);
   }

   public void deletePresetForUndo(String name) throws IOException {
      Path path = PresetData.resolvePresetPath(this.directory, name);
      Files.deleteIfExists(path);
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(name));
      }
   }

   public void renamePresetForUndo(String sourceName, String targetName, PresetData previousTarget, PresetData source) throws IOException {
      if (source == null) {
         throw new IOException("Preset no longer exists: " + sourceName);
      }

      Path sourcePath = PresetData.resolvePresetPath(this.directory, sourceName);
      Path targetPath = PresetData.resolvePresetPath(this.directory, targetName);
      if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Preset file no longer exists: " + sourcePath.getFileName());
      }
      if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Cannot restore preset because the destination already exists: " + targetPath.getFileName());
      }

      movePresetFile(sourcePath, targetPath);
      try {
         if (previousTarget != null) {
            this.writePresetForUndo(previousTarget);
         }
      } catch (IOException failure) {
         try {
            movePresetFile(targetPath, sourcePath);
         } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
         }
         throw failure;
      }

      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(sourceName)
            || candidate.getName().equalsIgnoreCase(targetName));
         this.entries.add(new PresetData(targetName, source.contents()));
         if (previousTarget != null) {
            this.entries.add(previousTarget);
         }
      }
   }

   private static void movePresetFile(Path source, Path target) throws IOException {
      try {
         Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException unsupported) {
         Files.move(source, target);
      }
   }

   public void savePreset(String name) throws IOException {
      PresetData preset = new PresetData(name, this.category.toJson().getAsJsonObject());
      preset.saveToDirectory(this.directory);
      synchronized (this.getRegistry()) {
         this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(name));
         this.entries.add(preset);
      }
   }

   public boolean deletePreset(String name) {
      try {
         Path path = PresetData.resolvePresetPath(this.directory, name);
         boolean deleted = Files.deleteIfExists(path);
         boolean removed;
         synchronized (this.getRegistry()) {
            removed = this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(name));
         }
         return deleted || removed;
      } catch (IOException invalidPath) {
         ClientAccess.logger.warn("Could not delete preset '{}': {}", name, invalidPath.getMessage());
         return false;
      }
   }

   public boolean renamePreset(String oldName, String newName) {
      if (oldName == null || newName == null || oldName.equalsIgnoreCase(newName)) {
         return false;
      }

      PresetData source;
      synchronized (this.getRegistry()) {
         source = this.getPreset(oldName);
         if (source == null || this.getPreset(newName) != null) {
            return false;
         }
      }

      try {
         Path sourcePath = PresetData.resolvePresetPath(this.directory, oldName);
         Path targetPath = PresetData.resolvePresetPath(this.directory, newName);
         if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)
            || Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
            return false;
         }

         movePresetFile(sourcePath, targetPath);
         synchronized (this.getRegistry()) {
            this.entries.removeIf(candidate -> candidate.getName().equalsIgnoreCase(oldName));
            this.entries.add(new PresetData(newName, source.contents()));
         }
         return true;
      } catch (IOException failure) {
         ClientAccess.logger.warn("Could not rename preset '{}' to '{}': {}", oldName, newName, failure.getMessage());
         return false;
      }
   }

   public void loadPresets() {
      synchronized (this.getRegistry()) {
         this.entries.clear();
         if (!Files.isDirectory(this.directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
         }

         try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory, "*.json")) {
            for (Path file : files) {
               if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                  continue;
               }

               String fileName = file.getFileName().toString();
               String presetName = fileName.substring(0, fileName.length() - ".json".length());
               try {
                  Path safePath = PresetData.resolvePresetPath(this.directory, presetName);
                  if (Files.size(safePath) == 0L) {
                     continue;
                  }

                  String jsonText = Files.readString(safePath, StandardCharsets.UTF_8);
                  JsonObject contents = JsonParser.parseString(jsonText).getAsJsonObject();
                  this.entries.add(new PresetData(presetName, contents));
               } catch (Exception entryFailure) {
                  ClientAccess.logger.warn("Skipping invalid preset file '{}': {}", fileName, entryFailure.getMessage());
               }
            }
         } catch (Exception directoryFailure) {
            ClientAccess.logger.warn("Could not load presets from '{}': {}", this.directory, directoryFailure.getMessage());
         }
      }
   }

   public boolean copyPreset(String sourceName, String targetName) {
      if (sourceName == null || targetName == null || sourceName.equalsIgnoreCase(targetName)) {
         return false;
      }

      PresetData source;
      synchronized (this.getRegistry()) {
         source = this.getPreset(sourceName);
         if (source == null || this.getPreset(targetName) != null) {
            return false;
         }
      }

      try {
         Path sourcePath = PresetData.resolvePresetPath(this.directory, sourceName);
         Path targetPath = PresetData.resolvePresetPath(this.directory, targetName);
         if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)
            || Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
            return false;
         }

         PresetData copy = new PresetData(targetName, source.contents());
         copy.saveToDirectory(this.directory);
         synchronized (this.getRegistry()) {
            this.entries.add(copy);
         }
         return true;
      } catch (IOException failure) {
         ClientAccess.logger.warn("Could not copy preset '{}' to '{}': {}", sourceName, targetName, failure.getMessage());
         return false;
      }
   }

   public void savePresetByExactName(String name) throws IOException {
      Iterator<PresetData> presets = this.entries.iterator();
      while (presets.hasNext()) {
         PresetData preset = presets.next();
         if (preset.getName().equals(name)) {
            preset.saveToDirectory(this.directory);
         }
      }
   }

   public void forEachPreset(Consumer consumer) {
      synchronized (this.getRegistry()) {
         this.getRegistry().forEach(consumer);
      }
   }

   public void saveAllPresets() {
      Iterator<PresetData> presets = this.entries.iterator();
      while (presets.hasNext()) {
         PresetData preset = presets.next();
         try {
            preset.saveToDirectory(this.directory);
         } catch (IOException writeFailure) {
            writeFailure.printStackTrace();
         }
      }
   }
}
