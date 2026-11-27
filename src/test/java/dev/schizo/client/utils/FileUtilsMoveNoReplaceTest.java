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

class FileUtilsMoveNoReplaceTest {
   @TempDir
   Path directory;

   @Test
   void moveNeverReplacesExistingTargetOrDeletesSource() throws Exception {
      Path source = this.directory.resolve("source.json");
      Path target = this.directory.resolve("target.json");
      Files.writeString(source, "source");
      Files.writeString(target, "target");

      assertThrows(FileAlreadyExistsException.class,
         () -> FileUtils.moveFileNoReplace(source, target));

      assertEquals("source", Files.readString(source));
      assertEquals("target", Files.readString(target));
   }

   @Test
   void concurrentMovesHaveExactlyOneWinnerAndKeepLosingSources() throws Exception {
      int writers = 32;
      Path target = this.directory.resolve("winner.json");
      List<Path> sources = new ArrayList<>();
      for (int index = 0; index < writers; index++) {
         Path source = this.directory.resolve("source-" + index + ".json");
         Files.writeString(source, "writer-" + index);
         sources.add(source);
      }

      var executor = Executors.newFixedThreadPool(writers);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger successes = new AtomicInteger();
      List<Future<?>> futures = new ArrayList<>();
      try {
         for (Path source : sources) {
            futures.add(executor.submit(() -> {
               start.await();
               try {
                  FileUtils.moveFileNoReplace(source, target);
                  successes.incrementAndGet();
               } catch (FileAlreadyExistsException expectedCollision) {
                  // Another source owns the destination; this source must remain untouched.
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
         assertEquals(writers - 1, sources.stream().filter(Files::exists).count());
      } finally {
         executor.shutdownNow();
      }
   }
}
