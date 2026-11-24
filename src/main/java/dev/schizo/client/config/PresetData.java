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

import dev.schizo.client.ClientAccess;
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

   public void saveToDirectory(Path directory) throws IOException {
      String presetName = this.name;
      Path targetPath = directory.resolve(presetName + ".json");
      Gson gson = ClientAccess.gson;
      JsonObject contents = this.contents();
      String serializedContents = gson.toJson(contents);
      FileUtils.writeUtf8(targetPath, serializedContents);
   }
}
