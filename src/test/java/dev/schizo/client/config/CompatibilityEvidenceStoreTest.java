package dev.schizo.client.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityEvidenceStoreTest {
   @TempDir Path root;

   @Test
   void templatesCannotCountAsCompletedTestsOrOverwriteExistingEvidence() throws Exception {
      var store = new CompatibilityEvidenceStore(root);
      store.createTemplate("test", "AutoWalk", "a".repeat(64), "1.21.11", "0.1.0");
      var receipt = store.load("test");
      assertThrows(IllegalArgumentException.class, () -> CompatibilityEvidenceStore.validate(receipt));
      assertThrows(FileAlreadyExistsException.class, () -> store.createTemplate("test", "Other", "b".repeat(64), "1.21.11", "0.1.0"));
      assertEquals("AutoWalk", store.load("test").get("module").getAsString());
      receipt.addProperty("buildCommit", "b".repeat(40));
      receipt.addProperty("serverSoftware", "Vanilla");
      receipt.addProperty("serverVersion", "1.21.11");
      receipt.addProperty("scenario", "Disconnect while walking");
      receipt.addProperty("result", "FAILED");
      receipt.getAsJsonArray("observations").add("Movement continued unexpectedly");
      assertDoesNotThrow(() -> CompatibilityEvidenceStore.validate(receipt));
      assertThrows(java.io.IOException.class, () -> store.load("../outside"));
   }

   @Test
   void rejectsSymlinkedEvidenceDirectoryBeforeWriting() throws Exception {
      Path real = Files.createDirectory(this.root.resolve("real"));
      Path link = this.root.resolve("linked");
      try {
         Files.createSymbolicLink(link, real);
      } catch (UnsupportedOperationException | SecurityException | java.io.IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      var store = new CompatibilityEvidenceStore(link);
      assertThrows(
         java.io.IOException.class,
         () -> store.createTemplate("test", "AutoWalk", "a".repeat(64), "1.21.11", "0.1.0")
      );
      assertFalse(Files.exists(real.resolve("test.json")));
   }

   @Test
   void rejectsSymlinkedEvidenceTarget() throws Exception {
      Path real = this.root.resolve("real.json");
      Files.writeString(real, "{}");
      Path link = this.root.resolve("test.json");
      try {
         Files.createSymbolicLink(link, real);
      } catch (UnsupportedOperationException | SecurityException | java.io.IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      var store = new CompatibilityEvidenceStore(this.root);
      assertThrows(java.io.IOException.class, () -> store.load("test"));
      assertThrows(
         java.io.IOException.class,
         () -> store.createTemplate("test", "AutoWalk", "a".repeat(64), "1.21.11", "0.1.0")
      );
   }
}
