package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PresetImportScreenPathTest {
   @Test
   void recognizesJsonSuffixRegardlessOfCase() {
      assertTrue(PresetImportScreen.isJsonPresetPath(Path.of("layout.json")));
      assertTrue(PresetImportScreen.isJsonPresetPath(Path.of("layout.JSON")));
      assertTrue(PresetImportScreen.isJsonPresetPath(Path.of("layout.JsOn")));
   }

   @Test
   void rejectsMisleadingOrMissingJsonSuffixes() {
      assertFalse(PresetImportScreen.isJsonPresetPath(Path.of("layout.json.txt")));
      assertFalse(PresetImportScreen.isJsonPresetPath(Path.of(".json")));
      assertFalse(PresetImportScreen.isJsonPresetPath(Path.of("layout")));
      assertFalse(PresetImportScreen.isJsonPresetPath(null));
   }

   @Test
   void stripsOnlyTheFinalJsonSuffix() {
      assertEquals("backup.json", PresetImportScreen.presetNameFromPath(Path.of("backup.json.JSON")));
      assertEquals("layout", PresetImportScreen.presetNameFromPath(Path.of("layout.JsOn")));
      assertEquals("layout.txt", PresetImportScreen.presetNameFromPath(Path.of("layout.txt")));
   }
}
