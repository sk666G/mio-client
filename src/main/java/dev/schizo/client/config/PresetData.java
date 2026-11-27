/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import dev.schizo.client.utils.FileUtils;

import dev.schizo.client.settings.NamedEntry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public record PresetData(String name, JsonObject contents) implements NamedEntry {
   public static final int MAX_PRESET_BYTES = 8 * 1024 * 1024;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public PresetData {
      name = PresetNameValidator.normalize(name);
      contents = Objects.requireNonNull(contents, "Preset contents are required").deepCopy();
   }

   @Override
   public String getName() {
      return this.name;
   }

   /** Returns an isolated JSON snapshot for filters and loaders that mutate their input. */
   public JsonObject copyContents() {
      return this.contents.deepCopy();
   }

   /** The record never exposes its mutable internal JSON object. */
   public JsonObject contents() {
      return this.copyContents();
   }

   public void saveToDirectory(Path directory) throws IOException {
      Path targetPath = this.resolveSafeTarget(directory);
      FileUtils.writeUtf8Atomically(targetPath, this.serializeForSave());
   }

   /** Saves a brand-new preset and fails if another writer already created the target. */
   public void saveNewToDirectory(Path directory) throws IOException {
      Path targetPath = this.resolveSafeTarget(directory);
      FileUtils.writeUtf8CreateNew(targetPath, this.serializeForSave());
   }

   private Path resolveSafeTarget(Path directory) throws IOException {
      Path root = directory.toAbsolutePath().normalize();
      Path parent = root.getParent();
      if (Files.isSymbolicLink(root)
         || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
         || (parent != null && Files.isSymbolicLink(parent))) {
         throw new IOException("Preset directory is not a safe real directory: " + root);
      }

      Path targetPath = PresetNameValidator.resolve(root, this.name);
      if (Files.isSymbolicLink(targetPath)
         || (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)
            && !Files.isRegularFile(targetPath, LinkOption.NOFOLLOW_LINKS))) {
         throw new IOException("Preset target is not a regular file: " + targetPath.getFileName());
      }
      return targetPath;
   }

   private String serializeForSave() throws IOException {
      String serialized = GSON.toJson(this.contents);
      if (serialized.getBytes(StandardCharsets.UTF_8).length > MAX_PRESET_BYTES) {
         throw new IOException("Preset exceeds the supported size limit");
      }
      return serialized;
   }
}
