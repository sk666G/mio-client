/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.schizo.client.config.JsonUtils;
import dev.schizo.client.utils.collections.NamedIdentifier;
import java.util.function.Predicate;

public final class IdentifierSetting extends Setting<NamedIdentifier> {

   public IdentifierSetting(String name, NamedIdentifier defaultValue) {
      super(name, defaultValue);
   }

   public IdentifierSetting(String name, NamedIdentifier defaultValue, Predicate<? super NamedIdentifier> visibility) {
      super(name, defaultValue, visibility);
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element == null || !element.isJsonObject()) {
         throw new IllegalArgumentException("Identifier setting requires a JSON object");
      }
      JsonObject object = element.getAsJsonObject();
      if (!JsonUtils.hasProperties(object, "name", "category")
         || !object.get("name").isJsonPrimitive()
         || !object.get("name").getAsJsonPrimitive().isString()
         || !object.get("category").isJsonPrimitive()
         || !object.get("category").getAsJsonPrimitive().isString()) {
         throw new IllegalArgumentException("Identifier setting requires string name and category fields");
      }
      String name = object.get("name").getAsString();
      String category = object.get("category").getAsString();
      if (name.isBlank() || category.isBlank() || name.length() > 256 || category.length() > 128) {
         throw new IllegalArgumentException("Identifier setting contains an invalid name or category");
      }
      this.setValue(new NamedIdentifier(category, name));
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();
      NamedIdentifier identifier = this.getValue();
      object.addProperty("name", identifier.getName());
      object.addProperty("category", identifier.getCategory());
      return object;
   }

   @Override
   public void parseValue(String value) {
      String[] parts = value.split(":");
      if (parts.length == 1) {
         NamedIdentifier identifier = new NamedIdentifier(value);
         this.setValue(identifier);
      } else if (parts.length == 2) {
         NamedIdentifier identifier = new NamedIdentifier(parts[0], parts[1]);
         this.setValue(identifier);
      }
   }
}
