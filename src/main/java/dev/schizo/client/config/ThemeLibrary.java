package dev.schizo.client.config;

import com.google.gson.*;
import dev.schizo.client.gui.PaletteHex;
import dev.schizo.client.modules.client.UiPalette;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Bounded fixed-path theme library. Every mutation checks the expected on-disk snapshot. */
public final class ThemeLibrary {
   private static final String FILE = "themes.json";
   private static final int MAX_BYTES = 65536, MAX_THEMES = 64;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final Path root;
   public record Entry(String name, UiPalette colors) {
      public Entry {
         name = PresetNameValidator.normalize(name);
         Objects.requireNonNull(colors);
      }
   }
   public ThemeLibrary(Path root) { this.root = root; }

   public synchronized List<Entry> load() throws IOException {
      Path target = ConfigPathGuard.resolveForRead(root, FILE);
      if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return List.of();
      try {
         JsonObject doc = JsonParser.parseString(ConfigFileReader.readUtf8(target, MAX_BYTES)).getAsJsonObject();
         if (doc.size() != 2 || !doc.get("version").isJsonPrimitive()
            || !doc.get("version").getAsJsonPrimitive().isNumber() || doc.get("version").getAsDouble() != 1)
            throw new IllegalArgumentException("Unsupported theme library");
         JsonArray array = doc.getAsJsonArray("themes");
         if (array.size() > MAX_THEMES) throw new IllegalArgumentException("Too many themes");
         List<Entry> entries = new ArrayList<>();
         for (JsonElement element : array) {
            JsonObject e = element.getAsJsonObject();
            if (e.size() != 7) throw new IllegalArgumentException("Invalid theme fields");
            entries.add(new Entry(string(e,"name"),new UiPalette(PaletteHex.parse(string(e,"accent")),
               PaletteHex.parse(string(e,"background")),PaletteHex.parse(string(e,"button")),
               PaletteHex.parse(string(e,"enabled")),PaletteHex.parse(string(e,"tint")),PaletteHex.parse(string(e,"gradient")))));
         }
         validate(entries);
         return List.copyOf(entries);
      } catch (RuntimeException invalid) { throw new IOException("Invalid theme library", invalid); }
   }

   private static String string(JsonObject object, String key) {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
         throw new IllegalArgumentException("Missing theme field");
      return value.getAsString();
   }

   private static void validate(List<Entry> entries) {
      if (entries.size() > MAX_THEMES) throw new IllegalArgumentException("Theme limit reached");
      Set<String> names = new HashSet<>();
      for (Entry entry : entries) {
         if (!names.add(entry.name().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Theme name already exists");
      }
   }

   public synchronized void replace(List<Entry> expected, List<Entry> next) throws IOException {
      List<Entry> snapshot = List.copyOf(next);
      validate(snapshot);
      if (!load().equals(expected)) throw new IOException("Theme library changed; reload before editing");
      if (snapshot.equals(expected)) return;
      JsonObject doc = new JsonObject();
      doc.addProperty("version", 1);
      JsonArray array = new JsonArray();
      for (Entry entry : snapshot) {
         JsonObject e = new JsonObject();
         e.addProperty("name",entry.name());
         UiPalette c = entry.colors();
         e.addProperty("accent",PaletteHex.format(c.accent())); e.addProperty("background",PaletteHex.format(c.background()));
         e.addProperty("button",PaletteHex.format(c.button())); e.addProperty("enabled",PaletteHex.format(c.enabled()));
         e.addProperty("tint",PaletteHex.format(c.tint())); e.addProperty("gradient",PaletteHex.format(c.gradient()));
         array.add(e);
      }
      doc.add("themes",array);
      String json = GSON.toJson(doc);
      if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IOException("Theme library exceeds size limit");
      Path target = ConfigPathGuard.resolveForWrite(root,FILE);
      BackedUpConfigWriter.writeAfterBackup(target,json,() -> new ConfigurationBackupManager(root).backupBeforeSave(FILE));
   }
}
