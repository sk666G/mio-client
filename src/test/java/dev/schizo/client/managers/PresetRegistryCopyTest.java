package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.config.PresetData;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetRegistryCopyTest {
   @TempDir
   Path directory;

   @Test
   void copyCreatesIndependentPreset() throws Exception {
      new PresetData("source", payload("source")).saveNewToDirectory(this.directory);
      PresetRegistry registry = new PresetRegistry(this.directory);
      registry.loadPresets();

      assertTrue(registry.copyPreset("source", "copy"));

      assertNotNull(registry.getPreset("copy"));
      String stored = Files.readString(this.directory.resolve("copy.json"));
      assertTrue(stored.contains("\"value\": \"source\""));
   }

   @Test
   void copyNeverReplacesExistingTarget() throws Exception {
      new PresetData("source", payload("source")).saveNewToDirectory(this.directory);
      new PresetData("target", payload("target")).saveNewToDirectory(this.directory);
      PresetRegistry registry = new PresetRegistry(this.directory);
      registry.loadPresets();

      assertThrows(java.nio.file.FileAlreadyExistsException.class,
         () -> registry.copyPreset("source", "target"));

      String stored = Files.readString(this.directory.resolve("target.json"));
      assertTrue(stored.contains("\"value\": \"target\""));
      assertFalse(stored.contains("\"value\": \"source\""));
   }

   private static JsonObject payload(String value) {
      JsonObject json = new JsonObject();
      json.addProperty("value", value);
      return json;
   }
}
