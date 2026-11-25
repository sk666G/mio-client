package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.config.PresetData;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

   @Test
   void presetContentsAreDefensivelyCopiedOnConstructionAndAccess() {
      JsonObject nested = new JsonObject();
      nested.addProperty("setting", "original");
      JsonObject contents = new JsonObject();
      contents.add("nested", nested);
      PresetData stored = new PresetData("isolated", contents);

      nested.addProperty("setting", "changed-after-construction");
      JsonObject exposed = stored.contents();
      exposed.getAsJsonObject("nested").addProperty("setting", "changed-after-read");
      JsonObject applied = stored.copyContents();
      applied.getAsJsonObject("nested").addProperty("setting", "filtered");

      assertEquals("original", stored.contents().getAsJsonObject("nested").get("setting").getAsString());
   }

   @Test
   void skipsOversizedPresetFilesDuringStartupLoad() throws IOException {
      Files.write(directory.resolve("oversized.json"),
         new byte[dev.schizo.client.config.PresetData.MAX_PRESET_BYTES + 1]);

      PresetRegistry registry = new PresetRegistry(directory);
      registry.loadPresets();

      assertNull(registry.getPreset("oversized"));
   }

   @Test
   void refusesToLoadFromASymlinkedPresetDirectory() throws IOException {
      Path actualDirectory = Files.createDirectory(directory.resolve("actual-presets"));
      preset("external").saveToDirectory(actualDirectory);
      Path linkedDirectory = directory.resolve("linked-presets");
      try {
         Files.createSymbolicLink(linkedDirectory, actualDirectory.toAbsolutePath());
      } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
         org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      PresetRegistry registry = new PresetRegistry(linkedDirectory);
      registry.loadPresets();

      assertNull(registry.getPreset("external"));
   }

   @Test
   void doesNotReadAnUnboundedExistingFileWhenRestoringPreset() throws IOException {
      Path existing = directory.resolve("existing.json");
      Files.write(existing,
         new byte[dev.schizo.client.config.PresetData.MAX_PRESET_BYTES + 1]);
      PresetRegistry registry = new PresetRegistry(directory);

      assertThrows(IOException.class, () -> registry.restorePreset(preset("existing")));
      assertEquals(dev.schizo.client.config.PresetData.MAX_PRESET_BYTES + 1L, Files.size(existing));
   }

   @Test
   void failedDirectoryRefreshPreservesLastCompleteRegistry() throws IOException {
      Path root = Files.createDirectory(directory.resolve("presets"));
      preset("known").saveToDirectory(root);
      PresetRegistry registry = new PresetRegistry(root);
      registry.loadPresets();
      Files.move(root, directory.resolve("moved"));
      registry.loadPresets();
      assertNotNull(registry.getPreset("known"));
      Files.createDirectory(root);
      registry.loadPresets();
      assertNull(registry.getPreset("known"));
   }

   @Test
   void restoreRollsBackExactBytesWhenReplacementCannotBeRemoved() throws IOException {
      byte[] original = {(byte)0xff, 42};
      Path target = directory.resolve("before.json");
      Files.write(target, original);
      Files.createDirectory(directory.resolve("after.json"));
      PresetRegistry registry = new PresetRegistry(directory);
      assertThrows(IOException.class, () -> registry.restorePreset(preset("before"), "after"));
      assertArrayEquals(original, Files.readAllBytes(target));
      assertNull(registry.getPreset("before"));
   }

   @Test
   void restoreRemovesReplacedEntryFromMemoryAndDisk() throws IOException {
      preset("after").saveToDirectory(directory);
      PresetRegistry registry = new PresetRegistry(directory);
      registry.loadPresets();
      registry.restorePreset(preset("before"), "after");
      assertFalse(Files.exists(directory.resolve("after.json")));
      assertNull(registry.getPreset("after"));
      assertNotNull(registry.getPreset("before"));
   }

   @Test
   void transactionalApplyPublishesSuccessfulState() {
      JsonObject before = json("value", "before");
      JsonObject desired = json("value", "after");
      AtomicReference<JsonObject> state = new AtomicReference<>(before.deepCopy());

      PresetRegistry.applyWithRollback(
         () -> state.get().deepCopy(),
         value -> state.set(value.getAsJsonObject().deepCopy()),
         desired
      );

      assertEquals("after", state.get().get("value").getAsString());
   }

   @Test
   void transactionalApplyRestoresSnapshotAfterPartialMutationFailure() {
      JsonObject before = json("value", "before");
      JsonObject desired = json("value", "after");
      AtomicReference<JsonObject> state = new AtomicReference<>(before.deepCopy());
      AtomicInteger calls = new AtomicInteger();

      RuntimeException failure = assertThrows(RuntimeException.class, () ->
         PresetRegistry.applyWithRollback(
            () -> state.get().deepCopy(),
            value -> {
               state.set(value.getAsJsonObject().deepCopy());
               if (calls.getAndIncrement() == 0) {
                  throw new IllegalStateException("apply failed after mutation");
               }
            },
            desired
         )
      );

      assertEquals("apply failed after mutation", failure.getMessage());
      assertEquals("before", state.get().get("value").getAsString());
      assertEquals(2, calls.get());
   }

   @Test
   void transactionalApplyPreservesRollbackFailureAsSuppressedContext() {
      AtomicInteger calls = new AtomicInteger();

      RuntimeException failure = assertThrows(RuntimeException.class, () ->
         PresetRegistry.applyWithRollback(
            () -> json("value", "before"),
            value -> {
               if (calls.getAndIncrement() == 0) {
                  throw new IllegalStateException("apply failed");
               }
               throw new IllegalArgumentException("rollback failed");
            },
            json("value", "after")
         )
      );

      assertEquals("apply failed", failure.getMessage());
      assertEquals(1, failure.getSuppressed().length);
      assertEquals("rollback failed", failure.getSuppressed()[0].getMessage());
   }

   @Test
   void transactionalApplyDoesNotExposeStoredDesiredPayloadToMutation() {
      JsonObject desired = json("value", "after");

      PresetRegistry.applyWithRollback(
         () -> json("value", "before"),
         value -> value.getAsJsonObject().addProperty("value", "mutated by category"),
         desired
      );

      assertEquals("after", desired.get("value").getAsString());
   }

   private static JsonObject json(String key, String value) {
      JsonObject object = new JsonObject();
      object.addProperty(key, value);
      return object;
   }

   private static PresetData preset(String name) {
      JsonObject contents = new JsonObject();
      contents.addProperty("value", name);
      return new PresetData(name, contents);
   }
}
