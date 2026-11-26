package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigPathGuardTest {
   @TempDir Path root;

   @Test
   void writeCreatesMissingNestedParentsWithoutLeavingTheRoot() throws IOException {
      Path target = ConfigPathGuard.resolveForWrite(this.root, "presets/auto.json");

      assertEquals(this.root.resolve("presets").resolve("auto.json"), target);
      assertTrue(Files.isDirectory(this.root.resolve("presets")));
      assertFalse(Files.exists(target));
   }

   @Test
   void concurrentWritersCanShareMissingParentDirectories() throws Exception {
      var pool = Executors.newFixedThreadPool(4);
      try {
         var ready = new CountDownLatch(4);
         var start = new CountDownLatch(1);
         var writers = new ArrayList<Future<Path>>();
         Path directory = root.resolve("profiles").resolve("shared");
         for (int i = 0; i < 4; i++) {
            final int index = i;
            writers.add(pool.submit(() -> {
               ready.countDown();
               if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
               if (index % 2 == 0) return ConfigPathGuard.ensureDirectory(root, directory);
               return ConfigPathGuard.resolveForWrite(root, "profiles/shared/" + index + ".json").getParent();
            }));
         }
         assertTrue(ready.await(10, TimeUnit.SECONDS));
         start.countDown();
         for (Future<Path> writer : writers) {
            assertEquals(directory, writer.get(10, TimeUnit.SECONDS));
         }
         assertTrue(Files.isDirectory(directory));
      } finally {
         pool.shutdownNow();
      }
   }

   @Test
   void readDoesNotCreateMissingParents() throws IOException {
      Path target = ConfigPathGuard.resolveForRead(this.root, "presets/auto.json");

      assertEquals(this.root.resolve("presets").resolve("auto.json"), target);
      assertFalse(Files.exists(this.root.resolve("presets")));
   }

   @Test
   void ensureDirectoryCreatesNestedRealDirectories() throws IOException {
      Path target = this.root.resolve("sounds").resolve("killstreaks").resolve("doublekill_2");

      Path resolved = ConfigPathGuard.ensureDirectory(this.root, target);

      assertEquals(target, resolved);
      assertTrue(Files.isDirectory(target));
   }

   @Test
   void ensureDirectoryRejectsSymlinkedParent() throws IOException {
      Path realDirectory = Files.createDirectory(this.root.resolve("real-sounds"));
      Path linkedDirectory = this.root.resolve("sounds");
      try {
         Files.createSymbolicLink(linkedDirectory, realDirectory);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(
         IOException.class,
         () -> ConfigPathGuard.ensureDirectory(this.root, linkedDirectory.resolve("killstreaks"))
      );
   }

   @Test
   void rejectsTraversalAndAbsolutePaths() {
      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForWrite(this.root, "../outside.json"));
      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForWrite(this.root, this.root.resolve("absolute.json").toString()));
   }

   @Test
   void rejectsSymlinkedParentForReadsAndWrites() throws IOException {
      Path realDirectory = this.root.resolve("real-presets");
      Files.createDirectory(realDirectory);
      Files.writeString(realDirectory.resolve("auto.json"), "{}");
      Path linkedDirectory = this.root.resolve("presets");
      try {
         Files.createSymbolicLink(linkedDirectory, realDirectory);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForRead(this.root, "presets/auto.json"));
      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForWrite(this.root, "presets/auto.json"));
   }

   @Test
   void rejectsSymlinkedFinalConfigFile() throws IOException {
      Path realConfig = this.root.resolve("real.json");
      Files.writeString(realConfig, "{}");
      Path linkedConfig = this.root.resolve("modules.json");
      try {
         Files.createSymbolicLink(linkedConfig, realConfig);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForRead(this.root, "modules.json"));
      assertThrows(IOException.class, () -> ConfigPathGuard.resolveForWrite(this.root, "modules.json"));
   }
}
