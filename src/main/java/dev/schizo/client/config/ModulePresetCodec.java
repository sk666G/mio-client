/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import dev.schizo.client.managers.AbstractRegistry;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.Module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.function.Predicate;

public final class ModulePresetCodec {
   public ModulePresetCodec.DecodedPreset decode(String encodedPreset) {
      JsonObject presetJson = decodePayload(encodedPreset, PresetData.MAX_PRESET_BYTES);
      String moduleConfigName = LegacyBranding.canonicalModuleName(this.readModuleConfigName(presetJson));
      AbstractRegistry registry = ClientContext.moduleRegistry;
      Predicate<Module> matchesModule = candidateModule -> {
         String candidateName = candidateModule.getConfigName();
         return candidateName.equals(moduleConfigName);
      };
      Module module = (Module)registry.findFirstOrNull(matchesModule);
      if (module == null) {
         throw new IllegalArgumentException();
      }
      return new ModulePresetCodec.DecodedPreset(module, presetJson);
   }

   static JsonObject decodePayload(String encodedPreset, int maximumDecodedBytes) {
      if (encodedPreset == null) {
         throw new IllegalArgumentException("Preset payload is required");
      }
      if (maximumDecodedBytes < 0) {
         throw new IllegalArgumentException("Maximum decoded preset size must not be negative");
      }

      String normalized = encodedPreset.trim().replace("\r", "").replace("\n", "");
      long maximumEncodedCharacters = 4L * ((maximumDecodedBytes + 2L) / 3L);
      if (normalized.length() > maximumEncodedCharacters) {
         throw new IllegalArgumentException("Preset payload exceeds the supported size limit");
      }

      byte[] decoded = Base64.getDecoder().decode(normalized);
      if (decoded.length > maximumDecodedBytes) {
         throw new IllegalArgumentException("Preset payload exceeds the supported size limit");
      }

      String jsonText = new String(decoded, StandardCharsets.UTF_8);
      JsonElement parsedJson = JsonParser.parseString(jsonText);
      if (!parsedJson.isJsonObject()) {
         throw new IllegalArgumentException("Preset payload must contain a JSON object");
      }
      return parsedJson.getAsJsonObject();
   }

   private String readModuleConfigName(JsonObject presetJson) {
      if (presetJson.has("m")) {
         return presetJson.get("m").getAsString();
      } else if (presetJson.has("module")) {
         return presetJson.get("module").getAsString();
      } else {
         throw new IllegalArgumentException();
      }
   }

   public String encode(Module module) {
      JsonElement moduleJson = module.toJson();
      JsonObject presetJson = moduleJson.getAsJsonObject();
      presetJson.remove("key");
      presetJson.remove("state");
      presetJson.remove("mouse");
      presetJson.remove("enabled");
      presetJson.remove("drawn");
      String moduleConfigName = module.getConfigName();
      presetJson.addProperty("m", moduleConfigName);
      Encoder base64Encoder = Base64.getEncoder();
      String jsonText = presetJson.toString();
      byte[] jsonBytes = jsonText.getBytes(StandardCharsets.UTF_8);
      return base64Encoder.encodeToString(jsonBytes);
   }

   public record DecodedPreset(
      Module module,
      JsonObject json
   ) {
   }
}
