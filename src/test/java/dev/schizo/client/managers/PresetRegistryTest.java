package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.config.PresetData;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetRegistryTest {
   @TempDir Path directory;

   @Test
   void rejectsDuplicateRenameAndLeavesBothPresetsAndRegistryIntact() throws IOException {
      PresetData source = preset("source");
      PresetData target = preset("target");
      source.saveToDirectory(directory);
      target.saveToDirectory(directory);
      PresetRegistry registry = new PresetRegistry(directory);
      registry.loadPresets();

      assertThrows(FileAlreadyExistsException.class, () -> registry.renamePreset("source", "target"));
      assertTrue(Files.exists(directory.resolve("source.json")));
      assertTrue(Files.exists(directory.resolve("target.json")));
      assertNotNull(registry.getPreset("source"));
      assertNotNull(registry.getPreset("target"));
   }

   @Test
   void rejectsTraversalAndMissingFilesWithoutMutatingRegistry() throws IOException {
      PresetData source = preset("source");
      source.saveToDirectory(directory);
      PresetRegistry registry = new PresetRegistry(directory);
      registry.loadPresets();

      assertThrows(IllegalArgumentException.class, () -> registry.renamePreset("source", "../outside"));
      assertThrows(IOException.class, () -> registry.deletePreset("missing"));
      assertTrue(Files.exists(directory.resolve("source.json")));
      assertNotNull(registry.getPreset("source"));
   }

   @Test
   void renameUndoAndRestoreKeepDiskAndMemoryInSync() throws IOException {
      PresetData original = preset("before");
      original.saveToDirectory(directory);
      PresetRegistry registry = new PresetRegistry(directory);
      registry.loadPresets();

      assertTrue(registry.renamePreset("before", "after"));
      assertFalse(Files.exists(directory.resolve("before.json")));
      assertTrue(Files.exists(directory.resolve("after.json")));
      registry.renamePresetForUndo("after", "before", null, original);
      assertTrue(Files.exists(directory.resolve("before.json")));
      assertFalse(Files.exists(directory.resolve("after.json")));
      assertNotNull(registry.getPreset("before"));

      Files.delete(directory.resolve("before.json"));
      registry.restorePreset(original);
      assertTrue(Files.exists(directory.resolve("before.json")));
      assertNotNull(registry.getPreset("before"));
   }

   private static PresetData preset(String name) {
      JsonObject contents = new JsonObject();
      contents.addProperty("value", name);
      return new PresetData(name, contents);
   }
}
