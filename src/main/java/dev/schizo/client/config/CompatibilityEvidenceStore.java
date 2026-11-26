package dev.schizo.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;

/** Operator-reported test receipts. A valid receipt never changes module certification. */
public final class CompatibilityEvidenceStore {
   private final Path directory;

   public CompatibilityEvidenceStore(Path directory) {
      this.directory = directory.toAbsolutePath().normalize();
   }

   private void ensureDirectory() throws IOException {
      Path parent = this.directory.getParent();
      if (parent == null) {
         throw new IOException("Evidence directory has no parent");
      }
      ConfigPathGuard.ensureDirectory(parent, this.directory);
   }

   private Path path(String name) throws IOException {
      if (Files.isSymbolicLink(this.directory)
         || !Files.isDirectory(this.directory, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Evidence directory must be a real directory");
      }
      Path target = PresetNameValidator.resolve(this.directory, name);
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
         && (Files.isSymbolicLink(target)
            || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))) {
         throw new IOException("Evidence target is not a regular file");
      }
      return target;
   }

   public void createTemplate(String name, String module, String settingsHash, String minecraft, String client) throws IOException {
      this.ensureDirectory();
      Path target = this.path(name);
      JsonObject receipt = new JsonObject();
      receipt.addProperty("version", 1);
      receipt.addProperty("evidenceType", "operator-reported");
      receipt.addProperty("module", module);
      receipt.addProperty("settingsSha256", settingsHash);
      receipt.addProperty("minecraftVersion", minecraft);
      receipt.addProperty("clientVersion", client);
      receipt.addProperty("testedAt", Instant.now().toString());
      receipt.addProperty("result", "UNTESTED");
      for (String field : new String[]{"buildCommit", "serverSoftware", "serverVersion", "scenario"}) receipt.add(field, null);
      receipt.add("plugins", new JsonArray());
      receipt.add("observations", new JsonArray());
      Path temporary = Files.createTempFile(this.directory, ".evidence-", ".tmp");
      try {
         Files.writeString(temporary, receipt.toString(), StandardCharsets.UTF_8);
         // The non-replacing move preserves any receipt already at the requested name.
         Files.move(temporary, target);
      } finally {
         Files.deleteIfExists(temporary);
      }
   }

   public JsonObject load(String name) throws IOException {
      try {
         return JsonParser.parseString(ConfigFileReader.readUtf8(this.path(name), 256 * 1024)).getAsJsonObject();
      } catch (RuntimeException invalid) {
         throw new IOException("Evidence must contain a JSON object", invalid);
      }
   }

   public static void validate(JsonObject receipt) {
      if (!receipt.has("version") || receipt.get("version").getAsDouble() != 1) {
         throw new IllegalArgumentException("Unsupported evidence version");
      }
      if (!text(receipt, "evidenceType").equals("operator-reported")) throw new IllegalArgumentException("Invalid evidence type");
      for (String field : new String[]{"module", "minecraftVersion", "clientVersion", "serverSoftware", "serverVersion", "scenario"}) text(receipt, field);
      if (!text(receipt, "buildCommit").matches("[a-fA-F0-9]{40}")) throw new IllegalArgumentException("A full tested commit SHA is required");
      if (!text(receipt, "settingsSha256").matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException("Invalid settings SHA-256");
      Instant.parse(text(receipt, "testedAt"));
      String result = text(receipt, "result");
      if (!result.equals("PASSED") && !result.equals("FAILED")) throw new IllegalArgumentException("Record PASSED or FAILED after testing");
      if (!receipt.has("plugins") || !receipt.get("plugins").isJsonArray()) throw new IllegalArgumentException("Record plugin names and versions, or an empty list for vanilla");
      for (var plugin : receipt.getAsJsonArray("plugins")) {
         if (!plugin.isJsonObject()) throw new IllegalArgumentException("Plugin entries require name and version");
         text(plugin.getAsJsonObject(), "name");
         text(plugin.getAsJsonObject(), "version");
      }
      if (!receipt.has("observations") || !receipt.get("observations").isJsonArray()
         || receipt.getAsJsonArray("observations").isEmpty()) throw new IllegalArgumentException("Record test observations");
      for (var observation : receipt.getAsJsonArray("observations")) {
         if (!observation.isJsonPrimitive() || !observation.getAsJsonPrimitive().isString()
            || observation.getAsString().isBlank()) throw new IllegalArgumentException("Observations must be nonempty strings");
      }
   }

   private static String text(JsonObject receipt, String field) {
      var value = receipt.get(field);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
         || value.getAsString().isBlank() || value.getAsString().length() > 4096) {
         throw new IllegalArgumentException("Missing or invalid " + field);
      }
      return value.getAsString();
   }
}
