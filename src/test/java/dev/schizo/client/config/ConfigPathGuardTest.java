package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
   void readDoesNotCreateMissingParents() throws IOException {
      Path target = ConfigPathGuard.resolveForRead(this.root, "presets/auto.json");

      assertEquals(this.root.resolve("presets").resolve("auto.json"), target);
      assertFalse(Files.exists(this.root.resolve("presets")));
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
