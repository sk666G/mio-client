/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.FileUtils;
import dev.schizo.client.settings.NamedEntry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;

public record PresetData(String name, JsonObject contents) implements ClientAccess, NamedEntry {
   @Override
   public String getName() {
      return this.name;
   }

   public static Path resolvePresetPath(Path directory, String name) throws IOException {
      if (directory == null || name == null || name.isBlank() || !name.equals(name.trim())
         || name.length() > 120 || name.equals(".") || name.equals("..")
         || name.contains("/") || name.contains("\\") || name.endsWith(".") || name.endsWith(" ")
         || name.chars().anyMatch(Character::isISOControl)
         || name.chars().anyMatch(character -> "<>:\"|?*".indexOf(character) >= 0)) {
         throw new IOException("Invalid preset name");
      }

      Path root = directory.toAbsolutePath().normalize();
      Path target = root.resolve(name + ".json").normalize();
      if (!target.startsWith(root)) {
         throw new IOException("Preset path escapes its directory");
      }
      if (Files.isSymbolicLink(target)) {
         throw new IOException("Preset path must not be a symbolic link");
      }
      return target;
   }

   public void saveToDirectory(Path directory) throws IOException {
      Path targetPath = resolvePresetPath(directory, this.name);
      Gson gson = ClientAccess.gson;
      FileUtils.writeUtf8Atomically(targetPath, gson.toJson(this.contents()));
   }
}
