/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo multiplayer compatibility policy.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.PermanentModule;
import dev.schizo.client.settings.JsonConfigurable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Opt-in multiplayer activation gate. WARN_ONLY remains the default because most modules
 * and every setting are still UNVERIFIED; users explicitly opt into the stricter policy.
 */
public final class MultiplayerCompatibilityPolicy implements JsonConfigurable {
   public static final MultiplayerCompatibilityPolicy GLOBAL = new MultiplayerCompatibilityPolicy();
   public static final int CURRENT_SCHEMA_VERSION = 1;
   public static final int MAX_ALLOWED_MODULES = 512;

   private Mode mode = Mode.WARN_ONLY;
   private final Set<String> allowedModules = new LinkedHashSet<>();

   public synchronized Mode getMode() {
      return this.mode;
   }

   public synchronized void setMode(Mode mode) {
      this.mode = Objects.requireNonNull(mode, "mode");
   }

   /** True only for a remote multiplayer session with the conservative policy active. */
   public boolean shouldBlock(Module module) {
      return module != null
         && MultiplayerCompatibilityWarnings.isMultiplayerSession()
         && this.blocksWhenConservative(module.getClass());
   }

   /** Pure policy decision, exposed separately so classification rules can be unit-tested. */
   public synchronized boolean blocksWhenConservative(Class<?> moduleType) {
      if (this.mode != Mode.CONSERVATIVE || moduleType == null
         || PermanentModule.class.isAssignableFrom(moduleType)) {
         // Permanent service modules cannot be safely toggled as a whole; their settings need review.
         return false;
      }
      String moduleClass = moduleType.getSimpleName();
      if (this.allowedModules.contains(moduleClass)) {
         return false;
      }

      ModuleCompatibility.Classification classification =
         ModuleCompatibility.assess(moduleType).classification();
      return switch (classification) {
         case SERVER_SENSITIVE, PACKET_AFFECTING, UNVERIFIED -> true;
         case CLIENT_ONLY, VISUAL_ONLY, INPUT_MODIFYING, WORLD_INTERACTION -> false;
      };
   }

   public synchronized boolean allowModule(String moduleClass) {
      String normalized = validateModuleClass(moduleClass);
      if (this.allowedModules.contains(normalized)) {
         return false;
      }
      if (this.allowedModules.size() >= MAX_ALLOWED_MODULES) {
         throw new IllegalStateException("Compatibility exception limit reached (" + MAX_ALLOWED_MODULES + ")");
      }
      return this.allowedModules.add(normalized);
   }

   public synchronized boolean removeAllowedModule(String moduleClass) {
      String normalized = validateModuleClass(moduleClass);
      return this.allowedModules.remove(normalized);
   }

   public synchronized void clearAllowedModules() {
      this.allowedModules.clear();
   }

   public synchronized List<String> getAllowedModules() {
      List<String> modules = new ArrayList<>(this.allowedModules);
      modules.sort(String.CASE_INSENSITIVE_ORDER);
      return List.copyOf(modules);
   }

   @Override
   public String getConfigName() {
      return "multiplayer-compatibility.json";
   }

   @Override
   public synchronized JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("version", CURRENT_SCHEMA_VERSION);
      root.addProperty("mode", this.mode.name().toLowerCase(Locale.ROOT));
      JsonArray allowed = new JsonArray();
      this.getAllowedModules().forEach(allowed::add);
      root.add("allowedModules", allowed);
      return root;
   }

   @Override
   public synchronized void fromJson(JsonElement json) {
      if (json == null || !json.isJsonObject()) {
         return;
      }
      JsonObject root = json.getAsJsonObject();
      JsonElement version = root.get("version");
      JsonElement modeElement = root.get("mode");
      JsonElement allowedElement = root.get("allowedModules");
      if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
         || version.getAsInt() != CURRENT_SCHEMA_VERSION
         || modeElement == null || !modeElement.isJsonPrimitive() || !modeElement.getAsJsonPrimitive().isString()
         || allowedElement == null || !allowedElement.isJsonArray()) {
         return;
      }

      final Mode parsedMode;
      try {
         parsedMode = Mode.valueOf(modeElement.getAsString().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException invalidMode) {
         return;
      }

      JsonArray values = allowedElement.getAsJsonArray();
      if (values.size() > MAX_ALLOWED_MODULES) {
         return;
      }
      Set<String> parsedAllowed = new LinkedHashSet<>();
      try {
         for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
               return;
            }
            parsedAllowed.add(validateModuleClass(value.getAsString()));
         }
      } catch (IllegalArgumentException invalidClassName) {
         return;
      }

      this.mode = parsedMode;
      this.allowedModules.clear();
      this.allowedModules.addAll(parsedAllowed);
   }

   private static String validateModuleClass(String value) {
      if (value == null || value.length() > 128
         || !value.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
         throw new IllegalArgumentException("Module class must be a simple Java class name");
      }
      return value;
   }

   public enum Mode {
      WARN_ONLY,
      CONSERVATIVE
   }
}
