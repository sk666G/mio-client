package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.awt.Color;
import org.junit.jupiter.api.Test;

class BlockColorRegistryTest {
   @Test
   void reloadReplacesPreviouslyLoadedColors() {
      BlockColorRegistry registry = new BlockColorRegistry();
      registry.putColor(null, Color.RED);

      JsonObject root = new JsonObject();
      root.add("blocks", new JsonObject());

      registry.fromJson(root);

      assertTrue(registry.getColors().isEmpty());
   }

   @Test
   void malformedRootDoesNotDiscardExistingState() {
      BlockColorRegistry registry = new BlockColorRegistry();
      registry.putColor(null, Color.RED);

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertEquals(Color.RED, registry.getColor(null));
      assertEquals(1, registry.getColors().size());
   }
}
