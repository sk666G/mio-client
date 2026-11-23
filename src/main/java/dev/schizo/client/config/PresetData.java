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
import java.nio.file.Path;
import java.util.Objects;

public record PresetData(String name, JsonObject contents) implements NamedEntry {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public PresetData {
      name = PresetNameValidator.normalize(name);
      contents = Objects.requireNonNull(contents, "Preset contents are required");
   }

   @Override
   public String getName() {
      return this.name;
   }

   public void saveToDirectory(Path directory) throws IOException {
      Path targetPath = PresetNameValidator.resolve(directory, this.name);
      FileUtils.writeUtf8Atomically(targetPath, GSON.toJson(this.contents));
   }
}
