package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.config.PresetData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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

   @Test
   void concurrentCopiesHaveExactlyOneWinner() throws Exception {
      new PresetData("source", payload("source")).saveNewToDirectory(this.directory);

      int writers = 16;
      List<PresetRegistry> registries = new ArrayList<>();
      for (int index = 0; index < writers; index++) {
         PresetRegistry registry = new PresetRegistry(this.directory);
         registry.loadPresets();
         registries.add(registry);
      }

      var executor = Executors.newFixedThreadPool(writers);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger successes = new AtomicInteger();
      List<Future<?>> futures = new ArrayList<>();
      try {
         for (PresetRegistry registry : registries) {
            futures.add(executor.submit(() -> {
               start.await();
               try {
                  if (registry.copyPreset("source", "race")) {
                     successes.incrementAndGet();
                  }
               } catch (java.nio.file.FileAlreadyExistsException expectedCollision) {
                  // Another registry won the create-only race.
               }
               return null;
            }));
         }

         start.countDown();
         for (Future<?> future : futures) {
            future.get();
         }

         assertEquals(1, successes.get());
         String stored = Files.readString(this.directory.resolve("race.json"));
         assertTrue(stored.contains("\"value\": \"source\""));
      } finally {
         executor.shutdownNow();
      }
   }

   private static JsonObject payload(String value) {
      JsonObject json = new JsonObject();
      json.addProperty("value", value);
      return json;
   }
}
