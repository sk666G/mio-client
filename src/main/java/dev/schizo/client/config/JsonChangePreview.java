package dev.schizo.client.config;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

/** Bounded JSON field comparison for previews. Values stay out of labels and logs. */
public final class JsonChangePreview {
   private final List<String> paths = new ArrayList<>();
   private int visited;
   private boolean truncated;

   private JsonChangePreview() {}

   public static Result compare(JsonElement current, JsonElement desired) {
      var preview = new JsonChangePreview();
      preview.visit(current, desired, "", 0);
      return new Result(List.copyOf(preview.paths), preview.truncated);
   }

   private void visit(JsonElement current, JsonElement desired, String path, int depth) {
      if (++this.visited > 2048 || depth > 32 || this.paths.size() >= 64) {
         this.truncated = true;
         return;
      }
      if (desired != null && desired.isJsonObject()) {
         for (var entry : desired.getAsJsonObject().entrySet()) {
            if (this.visited > 2048 || this.paths.size() >= 64) { this.truncated = true; break; }
            JsonElement previous = current != null && current.isJsonObject() ? current.getAsJsonObject().get(entry.getKey()) : null;
            String key = entry.getKey().replace("~", "~0").replace("/", "~1");
            this.visit(previous, entry.getValue(), path + "/" + key, depth + 1);
         }
      } else if (desired != null && desired.isJsonArray()) {
         var values = desired.getAsJsonArray();
         for (int i = 0; i < values.size(); i++) {
            if (this.visited > 2048 || this.paths.size() >= 64) { this.truncated = true; break; }
            JsonElement previous = current != null && current.isJsonArray() && i < current.getAsJsonArray().size()
               ? current.getAsJsonArray().get(i) : null;
            this.visit(previous, values.get(i), path + "/" + i, depth + 1);
         }
         if (current == null || !current.isJsonArray() || current.getAsJsonArray().size() != values.size()) this.add(path);
      } else if (!java.util.Objects.equals(current, desired)) {
         this.add(path);
      }
   }

   private void add(String path) {
      if (this.paths.size() < 64) this.paths.add(path.isEmpty() ? "/" : path);
      else this.truncated = true;
   }

   public record Result(List<String> paths, boolean truncated) {}
}
