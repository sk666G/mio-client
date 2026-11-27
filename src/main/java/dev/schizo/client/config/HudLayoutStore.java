/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.schizo.client.utils.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Stores named HUD-only layouts independently from the active client configuration. */
public final class HudLayoutStore {
   public static final int CURRENT_SCHEMA_VERSION = 1;
   private static final int MAX_WIDGETS = 512;
   private static final int MAX_LAYOUT_BYTES = 1_048_576;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private final Path directory;

   public HudLayoutStore(Path directory) {
      this.directory = directory.toAbsolutePath().normalize();
   }

   public Path getDirectory() {
      return this.directory;
   }

   public List<String> list() throws IOException {
      if (!Files.exists(this.directory, LinkOption.NOFOLLOW_LINKS)) {
         return List.of();
      }
      this.requireRealDirectory();

      List<String> names = new ArrayList<>();
      try (DirectoryStream<Path> entries = Files.newDirectoryStream(this.directory)) {
         for (Path entry : entries) {
            String filename = entry.getFileName().toString();
            if (!filename.endsWith(".json")
               || Files.isSymbolicLink(entry)
               || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
               continue;
            }

            String name = filename.substring(0, filename.length() - 5);
            if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
               continue;
            }
            try {
               names.add(PresetNameValidator.normalize(name));
            } catch (IllegalArgumentException invalidName) {
               // Ignore files that cannot be addressed safely through the layout commands.
            }
         }
      }
      names.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
      return List.copyOf(names);
   }

   public void save(String name, JsonObject layout) throws IOException {
      Path target = this.resolve(name);
      this.ensureDirectory();
      this.requireSafeTarget(target);
      this.validateLayout(layout);
      String serialized = GSON.toJson(layout);
      if (serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LAYOUT_BYTES) {
         throw new IOException("HUD layout is larger than the supported size limit");
      }
      FileUtils.writeUtf8Atomically(target, serialized);
   }

   public JsonObject load(String name) throws IOException {
      this.requireRealDirectory();
      Path target = this.resolve(name);
      this.requireSafeTarget(target);
      if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("HUD layout does not exist: " + PresetNameValidator.normalize(name));
      }

      long size = Files.size(target);
      if (size > MAX_LAYOUT_BYTES) {
         throw new IOException("HUD layout file is larger than the supported size limit");
      }

      byte[] contents;
      try (InputStream input = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
         contents = input.readNBytes(MAX_LAYOUT_BYTES + 1);
      }
      if (contents.length > MAX_LAYOUT_BYTES) {
         throw new IOException("HUD layout file is larger than the supported size limit");
      }

      try {
         JsonElement parsed = JsonParser.parseString(new String(contents, java.nio.charset.StandardCharsets.UTF_8));
         if (!parsed.isJsonObject()) {
            throw new IOException("HUD layout must contain a JSON object");
         }
         JsonObject layout = parsed.getAsJsonObject();
         this.validateLayout(layout);
         return layout;
      } catch (JsonParseException | IllegalStateException malformed) {
         throw new IOException("HUD layout contains invalid JSON", malformed);
      }
   }

   public boolean delete(String name) throws IOException {
      if (!Files.exists(this.directory, LinkOption.NOFOLLOW_LINKS)) {
         return false;
      }
      this.requireRealDirectory();
      Path target = this.resolve(name);
      this.requireSafeTarget(target);
      return Files.deleteIfExists(target);
   }

   public void rename(String oldName, String newName) throws IOException {
      this.requireRealDirectory();
      Path source = this.resolve(oldName);
      Path target = this.resolve(newName);
      this.requireSafeTarget(source);
      this.requireSafeTarget(target);
      if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("HUD layout does not exist: " + PresetNameValidator.normalize(oldName));
      }
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("A HUD layout with that name already exists");
      }

      FileUtils.moveFileNoReplace(source, target);
   }

   private Path resolve(String name) {
      return PresetNameValidator.resolve(this.directory, name);
   }

   private void validateLayout(JsonObject layout) throws IOException {
      if (layout == null || !layout.has("version") || !layout.get("version").isJsonPrimitive()
         || !layout.get("version").getAsJsonPrimitive().isNumber()
         || layout.get("version").getAsDouble() != CURRENT_SCHEMA_VERSION) {
         throw new IOException("Unsupported or missing HUD layout version");
      }
      if (!layout.has("widgets") || !layout.get("widgets").isJsonArray()) {
         throw new IOException("HUD layout is missing its widget list");
      }
      JsonArray widgets = layout.getAsJsonArray("widgets");
      if (widgets.size() > MAX_WIDGETS) {
         throw new IOException("HUD layout contains too many widgets");
      }
      java.util.Set<String> moduleNames = new java.util.HashSet<>();
      for (JsonElement widget : widgets) {
         if (!widget.isJsonObject()) {
            throw new IOException("HUD layout contains an invalid widget entry");
         }

         JsonObject widgetObject = widget.getAsJsonObject();
         String moduleName = requiredString(widgetObject, "module");
         requiredString(widgetObject, "anchor");
         requiredString(widgetObject, "horizontal");
         requiredString(widgetObject, "vertical");
         if (!moduleNames.add(moduleName)) {
            throw new IOException("HUD layout contains a duplicate widget: " + moduleName);
         }

         double x = requiredFiniteNumber(widgetObject, "x");
         double y = requiredFiniteNumber(widgetObject, "y");
         double scale = requiredFiniteNumber(widgetObject, "scale");
         if (x < 0.0D || x > 1.0D || y < 0.0D || y > 1.0D
            || scale < 0.5D || scale > 2.0D) {
            throw new IOException("HUD layout contains widget values outside the supported range");
         }
      }
   }

   private static String requiredString(JsonObject object, String key) throws IOException {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
         throw new IOException("HUD layout widget is missing a valid " + key + " value");
      }
      String text = value.getAsString();
      if (text.isBlank() || text.length() > 128) {
         throw new IOException("HUD layout widget contains an invalid " + key + " value");
      }
      return text;
   }

   private static double requiredFiniteNumber(JsonObject object, String key) throws IOException {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
         throw new IOException("HUD layout widget is missing a valid " + key + " value");
      }
      double number = value.getAsDouble();
      if (!Double.isFinite(number)) {
         throw new IOException("HUD layout widget contains a non-finite " + key + " value");
      }
      return number;
   }

   private void ensureDirectory() throws IOException {
      Path root = this.directory.getParent();
      if (root == null) {
         throw new IOException("HUD layout directory has no parent: " + this.directory);
      }
      ConfigPathGuard.ensureDirectory(root, this.directory);
   }

   private void requireRealDirectory() throws IOException {
      if (Files.isSymbolicLink(this.directory)
         || !Files.isDirectory(this.directory, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("HUD layout path is not a real directory: " + this.directory);
      }
   }

   private void requireSafeTarget(Path target) throws IOException {
      if (Files.isSymbolicLink(target)) {
         throw new IOException("HUD layout files cannot be symbolic links");
      }
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
         && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("HUD layout path is not a regular file");
      }
   }
}
