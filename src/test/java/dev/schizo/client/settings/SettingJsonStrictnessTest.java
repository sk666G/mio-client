package dev.schizo.client.settings;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class SettingJsonStrictnessTest {
   @Test
   void booleanSettingRejectsStringCoercion() {
      BooleanSetting setting = new BooleanSetting("Enabled", true);

      assertThrows(IllegalArgumentException.class, () -> setting.fromJson(new JsonPrimitive("false")));
      assertTrue(setting.getValue());
   }

   @Test
   void stringSettingRejectsNumericCoercion() {
      StringSetting setting = new StringSetting("Label", "good");

      assertThrows(IllegalArgumentException.class, () -> setting.fromJson(new JsonPrimitive(12)));
      assertEquals("good", setting.getValue());
   }

   @Test
   void identifierSettingRequiresStringFieldsAndPreservesLastGoodValue() {
      IdentifierSetting setting = new IdentifierSetting(
         "Sound",
         new dev.schizo.client.utils.collections.NamedIdentifier("minecraft", "entity.player.levelup")
      );
      JsonObject malformed = new JsonObject();
      malformed.addProperty("name", 12);
      malformed.addProperty("category", "minecraft");

      assertThrows(IllegalArgumentException.class, () -> setting.fromJson(malformed));
      assertEquals("entity.player.levelup", setting.getValue().getName());
      assertEquals("minecraft", setting.getValue().getCategory());
   }
}
