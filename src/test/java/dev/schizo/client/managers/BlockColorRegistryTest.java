package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.Color;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class BlockColorRegistryTest {
   @Test
   void reloadReplacesPreviouslyLoadedColors() {
      BlockColorRegistry registry = new BlockColorRegistry();
      registry.putColor(Blocks.STONE, Color.RED);

      JsonObject root = new JsonObject();
      JsonObject blocks = new JsonObject();
      blocks.add("minecraft:dirt", rgba(12, 34, 56, 78));
      root.add("blocks", blocks);

      registry.fromJson(root);

      assertNull(registry.getColor(Blocks.STONE));
      assertEquals(new Color(12, 34, 56, 78), registry.getColor(Blocks.DIRT));
      assertEquals(1, registry.getColors().size());
   }

   @Test
   void malformedRootDoesNotDiscardExistingState() {
      BlockColorRegistry registry = new BlockColorRegistry();
      registry.putColor(Blocks.STONE, Color.RED);

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertEquals(Color.RED, registry.getColor(Blocks.STONE));
      assertEquals(1, registry.getColors().size());
   }

   @Test
   void unknownBlockDoesNotBecomeTheDefaultRegistryBlock() {
      BlockColorRegistry registry = new BlockColorRegistry();

      JsonObject root = new JsonObject();
      JsonObject blocks = new JsonObject();
      blocks.add("schizo:definitely_missing_block", rgba(1, 2, 3, 4));
      root.add("blocks", blocks);

      registry.fromJson(root);

      assertTrue(registry.getColors().isEmpty());
   }

   private static JsonArray rgba(int red, int green, int blue, int alpha) {
      JsonArray color = new JsonArray();
      color.add(red);
      color.add(green);
      color.add(blue);
      color.add(alpha);
      return color;
   }
}
