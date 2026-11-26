package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NotificationsModuleTotemPathTest {
   @TempDir Path root;

   @Test
   void resolvesTxtFallbackInsideDataRoot() throws IOException {
      Path messages = Files.createDirectory(this.root.resolve("messages"));
      Path expected = Files.writeString(messages.resolve("totempop.txt"), "hello");

      Path resolved = NotificationsModule.resolveTotemMessagePath(this.root, "messages/totempop");

      assertEquals(expected, resolved);
   }

   @Test
   void rejectsTraversalOutsideDataRoot() {
      assertThrows(
         IOException.class,
         () -> NotificationsModule.resolveTotemMessagePath(this.root, "../totempop.txt")
      );
   }

   @Test
   void rejectsSymlinkedParentDirectory() throws IOException {
      Path realMessages = Files.createDirectory(this.root.resolve("real-messages"));
      Files.writeString(realMessages.resolve("totempop.txt"), "hello");
      Path linkedMessages = this.root.resolve("messages");
      try {
         Files.createSymbolicLink(linkedMessages, realMessages);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(
         IOException.class,
         () -> NotificationsModule.resolveTotemMessagePath(this.root, "messages/totempop.txt")
      );
   }
}
