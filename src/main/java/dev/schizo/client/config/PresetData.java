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
import dev.schizo.client.settings.NamedEntry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
      Path parent = targetPath.getParent();
      Files.createDirectories(parent);

      Gson gson = ClientAccess.gson;
      String serializedContents = gson.toJson(this.contents());
      Path temporary = Files.createTempFile(parent, ".schizo-preset-", ".tmp");
      try {
         Files.writeString(temporary, serializedContents, StandardCharsets.UTF_8);
         try {
            Files.move(temporary, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(temporary);
      }
   }
}
