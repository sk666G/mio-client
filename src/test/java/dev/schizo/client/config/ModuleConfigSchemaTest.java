package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ModuleConfigSchemaTest {
   @Test
   void acceptsLegacyUnversionedAndCurrentConfigurations() {
      ModuleConfigSchema.validate(new JsonObject());
      ModuleConfigSchema.validate(JsonParser.parseString("{\"version\":1}").getAsJsonObject());
   }

   @Test
   void rejectsMalformedAndUnsupportedVersions() {
      assertThrows(IllegalArgumentException.class, () ->
         ModuleConfigSchema.validate(JsonParser.parseString("{\"version\":\"1\"}").getAsJsonObject()));
      assertThrows(IllegalArgumentException.class, () ->
         ModuleConfigSchema.validate(JsonParser.parseString("{\"version\":1.5}").getAsJsonObject()));
      assertThrows(IllegalArgumentException.class, () ->
         ModuleConfigSchema.validate(JsonParser.parseString("{\"version\":2}").getAsJsonObject()));
      assertThrows(IllegalArgumentException.class, () ->
         ModuleConfigSchema.validate(JsonParser.parseString("{\"version\":0}").getAsJsonObject()));
   }
}
