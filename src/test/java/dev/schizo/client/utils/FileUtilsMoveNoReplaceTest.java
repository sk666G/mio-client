package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
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
   void supportedHardLinkMovePublishesCompletedContentsWithTheSameFileIdentity() throws Exception {
      Path source = Files.writeString(this.directory.resolve("source-large.json"), "before-rename");
      Path probe = this.directory.resolve("hardlink-probe");
      try {
         Files.createLink(probe, source);
      } catch (UnsupportedOperationException | java.io.IOException unavailable) {
         Assumptions.assumeTrue(false, "This filesystem does not support hard links");
      }
      Files.deleteIfExists(probe);

      Object originalKey = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
      Assumptions.assumeTrue(originalKey != null, "This provider does not expose stable file keys");

      Path target = this.directory.resolve("renamed.json");
      FileUtils.moveFileNoReplace(source, target);

      assertFalse(Files.exists(source, LinkOption.NOFOLLOW_LINKS));
      assertEquals("before-rename", Files.readString(target));
      assertEquals(originalKey,
         Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey());
   }

   @Test
   void rejectsSymlinkSourcesWithoutPublishingATarget() throws Exception {
      Path actual = Files.writeString(this.directory.resolve("real.json"), "safe");
      Path symlink = this.directory.resolve("symlink.json");
      try {
         Files.createSymbolicLink(symlink, actual);
      } catch (UnsupportedOperationException | java.io.IOException | SecurityException unavailable) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable");
      }

      Path target = this.directory.resolve("renamed.json");
      assertThrows(java.io.IOException.class, () -> FileUtils.moveFileNoReplace(symlink, target));
      assertFalse(Files.exists(target, LinkOption.NOFOLLOW_LINKS));
      assertEquals("safe", Files.readString(actual));
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
