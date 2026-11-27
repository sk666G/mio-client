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

class FileUtilsBytesCreateNewTest {
   @TempDir
   Path directory;

   @Test
   void binaryCreateNewNeverReplacesExistingTarget() throws Exception {
      Path target = this.directory.resolve("sound.ogg");
      Files.write(target, new byte[]{1, 2, 3});

      assertThrows(FileAlreadyExistsException.class,
         () -> FileUtils.writeBytesCreateNew(target, new byte[]{9, 9, 9}));

      assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(target));
   }

   @Test
   void concurrentBinaryCreateNewWritersHaveExactlyOneWinner() throws Exception {
      Path target = this.directory.resolve("race.bin");
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
                  FileUtils.writeBytesCreateNew(target, new byte[]{(byte)writer});
                  successes.incrementAndGet();
               } catch (FileAlreadyExistsException expectedCollision) {
                  // Exactly one writer owns CREATE_NEW.
               }
               return null;
            }));
         }

         start.countDown();
         for (Future<?> future : futures) {
            future.get();
         }

         assertEquals(1, successes.get());
         assertEquals(1, Files.readAllBytes(target).length);
      } finally {
         executor.shutdownNow();
      }
   }
}
