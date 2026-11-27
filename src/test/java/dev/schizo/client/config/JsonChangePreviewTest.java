package dev.schizo.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonChangePreviewTest {
   @Test
   void showsChangedPathsWithoutExposingValuesAndBoundsLargePreviews() {
      var before = JsonParser.parseString("{\"module\":{\"setting\":1,\"kept\":true}}");
      var desired = JsonParser.parseString("{\"module\":{\"setting\":2}}");
      var result = JsonChangePreview.compare(before, desired);
      assertEquals(java.util.List.of("/module/setting"), result.paths());
      assertFalse(result.truncated());
      JsonObject large = new JsonObject();
      for (int i = 0; i < 10000; i++) large.addProperty("key" + i, "private value");
      var bounded = JsonChangePreview.compare(new JsonObject(), large);
      assertEquals(64, bounded.paths().size());
      assertTrue(bounded.truncated());
   }
}
