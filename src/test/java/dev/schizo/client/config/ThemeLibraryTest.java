package dev.schizo.client.config;

import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ThemeLibraryTest {
   @TempDir Path root;
   private final UiPalette colors = new UiPalette(new Color(12,34,56,78),Color.BLACK,Color.GRAY,Color.RED,Color.BLUE,Color.WHITE);
   @Test void roundTripRenameAndDeleteWithStaleAndDuplicateGuards() throws IOException {
      ThemeLibrary store = new ThemeLibrary(root);
      var a = new ThemeLibrary.Entry("Crimson",colors);
      var b = new ThemeLibrary.Entry("Night",colors);
      assertTrue(store.load().isEmpty());
      store.replace(List.of(),List.of(a));
      assertEquals(List.of(a),new ThemeLibrary(root).load());
      assertThrows(IOException.class,() -> store.replace(List.of(),List.of(b)));
      assertThrows(IllegalArgumentException.class,() -> store.replace(List.of(a),List.of(a,new ThemeLibrary.Entry("crimson",colors))));
      store.replace(List.of(a),List.of(b));
      assertEquals(List.of(b),store.load());
      store.replace(List.of(b),List.of());
      assertTrue(store.load().isEmpty());
   }
   @Test void corruptAndOversizedFilesAreNotOverwritten() throws IOException {
      ThemeLibrary store = new ThemeLibrary(root);
      Path file=root.resolve("themes.json");
      Files.writeString(file,"invalid");
      assertThrows(IOException.class,() -> store.replace(List.of(),List.of(new ThemeLibrary.Entry("Safe",colors))));
      assertEquals("invalid",Files.readString(file));
      Files.writeString(file," ".repeat(65537));
      assertThrows(IOException.class,store::load);
      assertThrows(IllegalArgumentException.class,() -> new ThemeLibrary.Entry("../escape",colors));
   }
}
