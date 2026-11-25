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
import com.google.gson.JsonObject;

/** Version gate for modules.json while preserving legacy unversioned configurations. */
public final class ModuleConfigSchema {
   public static final int CURRENT_VERSION = 1;

   private ModuleConfigSchema() {
   }

   public static void validate(JsonObject config) {
      if (config == null) {
         throw new IllegalArgumentException("Module configuration is missing");
      }
      JsonElement versionElement = config.get("version");
      if (versionElement == null) {
         return;
      }
      if (!versionElement.isJsonPrimitive()
         || !versionElement.getAsJsonPrimitive().isNumber()) {
         throw new IllegalArgumentException("Module configuration version must be numeric");
      }
      double rawVersion = versionElement.getAsDouble();
      if (!Double.isFinite(rawVersion) || rawVersion != Math.rint(rawVersion)) {
         throw new IllegalArgumentException("Module configuration version must be an integer");
      }
      int version = versionElement.getAsInt();
      if (version < 1 || version > CURRENT_VERSION) {
         throw new IllegalArgumentException("Unsupported module configuration version: " + version);
      }
   }
}
