package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HudLayoutStoreTest {
   @TempDir Path temporaryDirectory;

   private JsonObject emptyLayout() {
      JsonObject layout = new JsonObject();
      layout.addProperty("version", HudLayoutStore.CURRENT_SCHEMA_VERSION);
      layout.add("widgets", new JsonArray());
      return layout;
   }

   private JsonObject layoutWithWidget() {
      JsonObject widget = new JsonObject();
      widget.addProperty("module", "Metrics");
      widget.addProperty("anchor", "TOP_LEFT");
      widget.addProperty("x", 0.1F);
      widget.addProperty("y", 0.2F);
      widget.addProperty("horizontal", "LEFT");
      widget.addProperty("vertical", "TOP");
      widget.addProperty("scale", 1.0F);

      JsonObject layout = this.emptyLayout();
      layout.getAsJsonArray("widgets").add(widget);
      return layout;
   }

   @Test
   void savesListsLoadsRenamesAndDeletesLayouts() throws IOException {
      Path directory = this.temporaryDirectory.resolve("hud-layouts");
      HudLayoutStore store = new HudLayoutStore(directory);
      JsonObject layout = this.layoutWithWidget();

      store.save("Gameplay", layout);
      assertEquals(java.util.List.of("Gameplay"), store.list());
      assertEquals(new Gson().toJson(layout), new Gson().toJson(store.load("Gameplay")));

      store.rename("Gameplay", "Minimal");
      assertEquals(java.util.List.of("Minimal"), store.list());
      assertEquals(new Gson().toJson(layout), new Gson().toJson(store.load("Minimal")));

      assertTrue(store.delete("Minimal"));
      assertFalse(store.delete("Minimal"));
      assertTrue(store.list().isEmpty());
   }

   @Test
   void rejectsTraversalAndReservedFileNames() {
      HudLayoutStore store = new HudLayoutStore(this.temporaryDirectory.resolve("hud-layouts"));
      JsonObject layout = this.emptyLayout();

      for (String name : new String[] {"", ".", "..", "../outside", "foo..bar", "CON", "bad/name", "trailing."}) {
         assertThrows(IllegalArgumentException.class, () -> store.save(name, layout), name);
      }
   }

   @Test
   void rejectsUnsupportedVersionsAndMalformedDocuments() throws IOException {
      HudLayoutStore store = new HudLayoutStore(this.temporaryDirectory.resolve("hud-layouts"));
      JsonObject unsupported = this.emptyLayout();
      unsupported.addProperty("version", 999);
      assertThrows(IOException.class, () -> store.save("unsupported", unsupported));

      Files.createDirectories(store.getDirectory());
      Files.writeString(store.getDirectory().resolve("broken.json"), "{");
      assertThrows(IOException.class, () -> store.load("broken"));
   }

   @Test
   void rejectsMalformedWidgetEntriesAndDuplicateModules() throws IOException {
      HudLayoutStore store = new HudLayoutStore(this.temporaryDirectory.resolve("hud-layouts"));
      JsonObject malformed = this.emptyLayout();
      JsonObject widget = new JsonObject();
      widget.addProperty("module", "Metrics");
      malformed.getAsJsonArray("widgets").add(widget);
      assertThrows(IOException.class, () -> store.save("malformed", malformed));

      JsonObject duplicate = this.layoutWithWidget();
      duplicate.getAsJsonArray("widgets").add(duplicate.getAsJsonArray("widgets").get(0).deepCopy());
      assertThrows(IOException.class, () -> store.save("duplicate", duplicate));
   }

   @Test
   void rejectsTooManyWidgetsAndOversizedLayoutFiles() throws IOException {
      HudLayoutStore store = new HudLayoutStore(this.temporaryDirectory.resolve("hud-layouts"));
      JsonObject tooMany = this.emptyLayout();
      JsonArray widgets = tooMany.getAsJsonArray("widgets");
      for (int index = 0; index < 513; index++) {
         widgets.add(this.layoutWithWidget().getAsJsonArray("widgets").get(0));
      }
      assertThrows(IOException.class, () -> store.save("too-many", tooMany));

      Files.createDirectories(store.getDirectory());
      Files.write(store.getDirectory().resolve("large.json"), new byte[1_048_577]);
      assertThrows(IOException.class, () -> store.load("large"));
   }

   @Test
   void renameDoesNotOverwriteAnExistingLayout() throws IOException {
      HudLayoutStore store = new HudLayoutStore(this.temporaryDirectory.resolve("hud-layouts"));
      store.save("first", this.emptyLayout());
      store.save("second", this.emptyLayout());

      assertThrows(IOException.class, () -> store.rename("first", "second"));
      assertEquals(java.util.List.of("first", "second"), store.list());
   }

   @Test
   void refusesSymbolicLinkLayoutDirectoriesAndFiles() throws IOException {
      Path realDirectory = this.temporaryDirectory.resolve("real");
      Files.createDirectories(realDirectory);
      Path externalFile = this.temporaryDirectory.resolve("outside.json");
      Files.writeString(externalFile, "{\"version\":1,\"widgets\":[]}");

      Path linkedDirectory = this.temporaryDirectory.resolve("linked");
      Path linkedFile = realDirectory.resolve("link.json");
      try {
         Files.createSymbolicLink(linkedDirectory, realDirectory);
         Files.createSymbolicLink(linkedFile, externalFile);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(IOException.class, () -> new HudLayoutStore(linkedDirectory).list());
      HudLayoutStore store = new HudLayoutStore(realDirectory);
      assertThrows(IOException.class, () -> store.load("link"));
      assertThrows(IOException.class, () -> store.save("link", this.emptyLayout()));
      assertThrows(IOException.class, () -> store.delete("link"));
      assertTrue(Files.exists(externalFile));
   }
}
