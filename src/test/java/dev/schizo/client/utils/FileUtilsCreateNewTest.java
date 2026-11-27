package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileAlreadyExistsException;
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

class FileUtilsCreateNewTest {
   @TempDir
   Path directory;

   @Test
   void createNewNeverReplacesExistingTarget() throws Exception {
      Path target = this.directory.resolve("state.json");
      Files.writeString(target, "existing");

      assertThrows(FileAlreadyExistsException.class,
         () -> FileUtils.writeUtf8CreateNew(target, "replacement"));

      assertEquals("existing", Files.readString(target));
   }

   @Test
   void concurrentCreateNewWritersHaveExactlyOneWinner() throws Exception {
      Path target = this.directory.resolve("race.json");
      int writers = 32;
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
                  FileUtils.writeUtf8CreateNew(target, "writer-" + writer);
                  successes.incrementAndGet();
               } catch (FileAlreadyExistsException expectedCollision) {
                  // Exactly one writer owns CREATE_NEW; every other writer must lose.
               }
               return null;
            }));
         }

         start.countDown();
         for (Future<?> future : futures) {
            future.get();
         }

         assertEquals(1, successes.get());
         assertTrue(Files.readString(target).startsWith("writer-"));
      } finally {
         executor.shutdownNow();
      }
   }
}
