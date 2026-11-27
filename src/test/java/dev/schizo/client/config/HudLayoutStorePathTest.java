package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HudLayoutStorePathTest {
   @TempDir
   Path temporaryDirectory;

   @Test
   void saveCreatesManagedLayoutDirectory() throws Exception {
      Path layoutDirectory = this.temporaryDirectory.resolve("hud-layouts");
      HudLayoutStore store = new HudLayoutStore(layoutDirectory);

      store.save("default", emptyLayout());

      assertTrue(Files.isDirectory(layoutDirectory));
      assertTrue(Files.isRegularFile(layoutDirectory.resolve("default.json")));
   }

   @Test
   void saveRejectsSymlinkedLayoutRoot() throws Exception {
      Path realRoot = this.temporaryDirectory.resolve("real-root");
      Files.createDirectory(realRoot);
      Path linkedRoot = this.temporaryDirectory.resolve("linked-root");
      try {
         Files.createSymbolicLink(linkedRoot, realRoot);
      } catch (UnsupportedOperationException | IOException unsupported) {
         org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links are unavailable");
         return;
      }

      HudLayoutStore store = new HudLayoutStore(linkedRoot.resolve("hud-layouts"));

      assertThrows(IOException.class, () -> store.save("default", emptyLayout()));
      assertFalse(Files.exists(realRoot.resolve("hud-layouts")));
   }

   @Test
   void saveRejectsNonDirectoryLayoutPath() throws Exception {
      Path layoutDirectory = this.temporaryDirectory.resolve("hud-layouts");
      Files.writeString(layoutDirectory, "not a directory");
      HudLayoutStore store = new HudLayoutStore(layoutDirectory);

      assertThrows(IOException.class, () -> store.save("default", emptyLayout()));
   }

   private static JsonObject emptyLayout() {
      JsonObject layout = new JsonObject();
      layout.addProperty("version", HudLayoutStore.CURRENT_SCHEMA_VERSION);
      layout.add("widgets", new JsonArray());
      return layout;
   }
}
