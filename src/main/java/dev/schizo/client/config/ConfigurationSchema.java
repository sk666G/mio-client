package dev.schizo.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Version marker and migration boundary for top-level Schizo configuration documents. */
public final class ConfigurationSchema {
   public static final String VERSION_FIELD = "_schemaVersion";
   public static final int CURRENT_VERSION = 1;

   private ConfigurationSchema() {
   }

   public static JsonObject prepareForLoad(String configName, JsonObject source) {
      if (source == null) {
         throw new IllegalArgumentException("Configuration root must be a JSON object");
      }
      JsonObject working = source.deepCopy();
      int version = readVersion(working);
      if (version > CURRENT_VERSION) {
         throw new IllegalArgumentException(
            "Configuration " + configName + " uses unsupported future schema version " + version
         );
      }

      while (version < CURRENT_VERSION) {
         working = migrate(configName, version, working);
         version++;
      }
      working.addProperty(VERSION_FIELD, CURRENT_VERSION);
      return working;
   }

   public static JsonObject stampForSave(JsonObject source) {
      if (source == null) {
         throw new IllegalArgumentException("Configuration root must be a JSON object");
      }
      JsonObject stamped = source.deepCopy();
      stamped.addProperty(VERSION_FIELD, CURRENT_VERSION);
      return stamped;
   }

   private static int readVersion(JsonObject object) {
      JsonElement element = object.get(VERSION_FIELD);
      if (element == null) {
         return 0;
      }
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
         throw new IllegalArgumentException("Configuration schema version must be an integer");
      }
      double numeric = element.getAsDouble();
      if (!Double.isFinite(numeric) || numeric != Math.rint(numeric)
         || numeric < 0.0D || numeric > Integer.MAX_VALUE) {
         throw new IllegalArgumentException("Configuration schema version must be a non-negative integer");
      }
      return (int)numeric;
   }

   private static JsonObject migrate(String configName, int fromVersion, JsonObject source) {
      return switch (fromVersion) {
         case 0 -> source; // Legacy unversioned files already use the version-1 document shape.
         default -> throw new IllegalArgumentException(
            "No migration path for " + configName + " from schema version " + fromVersion
         );
      };
   }
}
