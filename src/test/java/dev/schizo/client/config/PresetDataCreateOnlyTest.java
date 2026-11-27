package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.io.IOException;
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

class PresetDataCreateOnlyTest {
   @TempDir
   Path directory;

   @Test
   void createOnlySaveNeverReplacesExistingPreset() throws Exception {
      PresetData first = new PresetData("same", payload("first"));
      PresetData second = new PresetData("same", payload("second"));

      first.saveNewToDirectory(this.directory);
      assertThrows(IOException.class, () -> second.saveNewToDirectory(this.directory));

      String stored = Files.readString(this.directory.resolve("same.json"));
      assertTrue(stored.contains("\"value\": \"first\""));
      assertFalse(stored.contains("\"value\": \"second\""));
   }

   @Test
   void concurrentCreateOnlySavesHaveExactlyOneWinner() throws Exception {
      int writers = 16;
      var executor = Executors.newFixedThreadPool(writers);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger successes = new AtomicInteger();
      List<Future<?>> futures = new ArrayList<>();
      try {
         for (int index = 0; index < writers; index++) {
            int writer = index;
            futures.add(executor.submit(() -> {
               start.await();
               try {
                  new PresetData("race", payload("writer-" + writer))
                     .saveNewToDirectory(this.directory);
                  successes.incrementAndGet();
               } catch (IOException expectedCollision) {
                  // Another writer won the create-only race.
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
         assertTrue(stored.contains("writer-"));
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
