package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class ConfigurationSchemaTest {
   @Test
   void legacyUnversionedObjectLoadsAsCurrentWithoutMutatingSource() {
      JsonObject source = new JsonObject();
      source.addProperty("value", 3);

      JsonObject migrated = ConfigurationSchema.prepareForLoad("test.json", source);

      assertFalse(source.has(ConfigurationSchema.VERSION_FIELD));
      assertEquals(ConfigurationSchema.CURRENT_VERSION,
         migrated.get(ConfigurationSchema.VERSION_FIELD).getAsInt());
      assertEquals(3, migrated.get("value").getAsInt());
   }

   @Test
   void saveStampDoesNotMutateCallerObject() {
      JsonObject source = new JsonObject();
      source.addProperty("name", "schizo");

      JsonObject stamped = ConfigurationSchema.stampForSave(source);

      assertFalse(source.has(ConfigurationSchema.VERSION_FIELD));
      assertEquals(ConfigurationSchema.CURRENT_VERSION,
         stamped.get(ConfigurationSchema.VERSION_FIELD).getAsInt());
   }

   @Test
   void rejectsFutureFractionalNegativeAndNonNumericVersions() {
      JsonObject future = new JsonObject();
      future.addProperty(ConfigurationSchema.VERSION_FIELD, ConfigurationSchema.CURRENT_VERSION + 1);
      assertThrows(IllegalArgumentException.class,
         () -> ConfigurationSchema.prepareForLoad("test.json", future));

      JsonObject fractional = new JsonObject();
      fractional.addProperty(ConfigurationSchema.VERSION_FIELD, 0.5D);
      assertThrows(IllegalArgumentException.class,
         () -> ConfigurationSchema.prepareForLoad("test.json", fractional));

      JsonObject negative = new JsonObject();
      negative.addProperty(ConfigurationSchema.VERSION_FIELD, -1);
      assertThrows(IllegalArgumentException.class,
         () -> ConfigurationSchema.prepareForLoad("test.json", negative));

      JsonObject text = new JsonObject();
      text.add(ConfigurationSchema.VERSION_FIELD, new JsonPrimitive("1"));
      assertThrows(IllegalArgumentException.class,
         () -> ConfigurationSchema.prepareForLoad("test.json", text));
   }
}
