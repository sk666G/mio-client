package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeModeManagerTest {
   @TempDir
   Path temporary;

   @Test
   void initializesNormallyWhenNoMarkerExists() throws IOException {
      SafeModeManager manager = new SafeModeManager();
      Path root = temporary.resolve("schizo");
      SafeModeManager.Status status = manager.initialize(root);

      assertTrue(Files.isDirectory(root));
      assertFalse(status.active());
      assertFalse(status.armed());
      assertFalse(manager.isActive());
   }

   @Test
   void markerActivatesSafeModeAtStartup() throws IOException {
      Path root = Files.createDirectory(temporary.resolve("schizo"));
      Files.createFile(root.resolve(SafeModeManager.MARKER_FILE_NAME));
      SafeModeManager manager = new SafeModeManager();

      SafeModeManager.Status status = manager.initialize(root);
      assertTrue(status.active());
      assertTrue(status.armed());
      assertTrue(manager.isActive());
      assertFalse(status.unsafeMarker());
   }

   @Test
   void armingDoesNotChangeTheCurrentSessionButActivatesTheNextOne() throws IOException {
      Path root = Files.createDirectory(temporary.resolve("schizo"));
      SafeModeManager current = new SafeModeManager();
      current.initialize(root);

      assertTrue(current.armForNextStartup());
      assertFalse(current.isActive());
      assertTrue(current.status().armed());

      SafeModeManager nextStartup = new SafeModeManager();
      SafeModeManager.Status nextStatus = nextStartup.initialize(root);
      assertTrue(nextStatus.active());
      assertTrue(nextStartup.isActive());
   }

   @Test
   void disarmingDuringSafeModeOnlyChangesTheNextStartup() throws IOException {
      Path root = Files.createDirectory(temporary.resolve("schizo"));
      Files.createFile(root.resolve(SafeModeManager.MARKER_FILE_NAME));
      SafeModeManager current = new SafeModeManager();
      current.initialize(root);

      assertTrue(current.disarmForNextStartup());
      assertTrue(current.isActive());
      assertFalse(current.status().armed());
      assertFalse(Files.exists(root.resolve(SafeModeManager.MARKER_FILE_NAME)));

      SafeModeManager nextStartup = new SafeModeManager();
      assertFalse(nextStartup.initialize(root).active());
   }

   @Test
   void repeatedArmAndDisarmOperationsAreIdempotent() throws IOException {
      Path root = Files.createDirectory(temporary.resolve("schizo"));
      SafeModeManager manager = new SafeModeManager();
      manager.initialize(root);

      assertTrue(manager.armForNextStartup());
      assertFalse(manager.armForNextStartup());
      assertTrue(manager.disarmForNextStartup());
      assertFalse(manager.disarmForNextStartup());
   }

   @Test
   void refusesASymbolicLinkConfigurationRoot() throws IOException {
      Path outside = Files.createDirectory(temporary.resolve("outside"));
      Path link = temporary.resolve("schizo-link");
      try {
         Files.createSymbolicLink(link, outside);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         return;
      }

      SafeModeManager manager = new SafeModeManager();
      assertThrows(IOException.class, () -> manager.initialize(link));
      assertTrue(Files.isDirectory(outside));
      assertTrue(Files.isSymbolicLink(link));
   }

   @Test
   void treatsAnUnsafeMarkerAsActiveAndCanDeleteTheLinkItself() throws IOException {
      Path root = Files.createDirectory(temporary.resolve("schizo"));
      Path outside = Files.writeString(temporary.resolve("outside.txt"), "do not touch");
      Path marker = root.resolve(SafeModeManager.MARKER_FILE_NAME);
      try {
         Files.createSymbolicLink(marker, outside);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         return;
      }

      SafeModeManager manager = new SafeModeManager();
      SafeModeManager.Status status = manager.initialize(root);
      assertTrue(status.active());
      assertTrue(status.armed());
      assertTrue(status.unsafeMarker());

      assertTrue(manager.disarmForNextStartup());
      assertFalse(Files.exists(marker, java.nio.file.LinkOption.NOFOLLOW_LINKS));
      assertEquals("do not touch", Files.readString(outside));
      assertTrue(manager.isActive());
   }

   @Test
   void failsClosedWhenStartupCannotInitializeTheConfigRoot() {
      SafeModeManager manager = new SafeModeManager();
      manager.activateFailSafe();

      assertTrue(manager.isActive());
      assertTrue(manager.status().initializationFailed());
   }
}
