/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.Map;

/** Keeps automatic context presets from taking ownership of user-controlled module activation. */
final class ContextProfilePresetPolicy {
   private ContextProfilePresetPolicy() {
   }

   static JsonElement withoutModuleEnabledState(JsonElement state) {
      if (state == null || !state.isJsonObject()) {
         return state;
      }
      JsonObject root = state.getAsJsonObject();
      Iterator<Map.Entry<String, JsonElement>> entries = root.entrySet().iterator();
      while (entries.hasNext()) {
         Map.Entry<String, JsonElement> entry = entries.next();
         if (entry.getKey().equals("client") || entry.getKey().equals("category")) {
            continue;
         }
         if (entry.getValue().isJsonObject()) {
            entry.getValue().getAsJsonObject().remove("enabled");
         }
      }
      return root;
   }
}
