package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompatibilityEvidenceStoreCreateTest {
   @TempDir
   Path directory;

   @Test
   void concurrentTemplateCreationHasExactlyOneWinner() throws Exception {
      CompatibilityEvidenceStore store =
         new CompatibilityEvidenceStore(this.directory.resolve("evidence"));
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
                  store.createTemplate(
                     "same",
                     "module-" + writer,
                     "a".repeat(64),
                     "1.21.11",
                     "test-client"
                  );
                  successes.incrementAndGet();
               } catch (FileAlreadyExistsException expectedCollision) {
                  // Another command created this receipt first.
               }
               return null;
            }));
         }

         start.countDown();
         for (Future<?> future : futures) {
            future.get();
         }

         assertEquals(1, successes.get());
         var stored = store.load("same");
         assertEquals("operator-reported", stored.get("evidenceType").getAsString());
         assertEquals("UNTESTED", stored.get("result").getAsString());
         assertTrue(stored.get("module").getAsString().startsWith("module-"));
      } finally {
         executor.shutdownNow();
      }
   }
}
