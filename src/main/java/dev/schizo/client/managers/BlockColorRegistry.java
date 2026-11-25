/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.config.ColorJsonConverter;
import dev.schizo.client.settings.JsonConfigurable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public final class BlockColorRegistry implements JsonConfigurable {
   private static final ColorJsonConverter COLOR_CODEC;
   private final Map<Block, Color> colors = new HashMap<>();

   public Map<Block, Color> getColors() {
      return this.colors;
   }

   static {
      COLOR_CODEC = new ColorJsonConverter();
   }

   public Color getColor(Block block) {
      return this.colors.get(block);
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject root)) {
         throw new IllegalArgumentException("Block-color configuration must be a JSON object");
      }

      JsonElement blocksElement = root.get("blocks");
      if (!(blocksElement instanceof JsonObject blocksJson)) {
         throw new IllegalArgumentException("Block-color configuration must contain a blocks object");
      }

      Map<Block, Color> loaded = new HashMap<>();
      blocksJson.asMap().forEach((blockId, colorJson) -> {
         try {
            Color color = COLOR_CODEC.fromJson(colorJson);
            DefaultedRegistry<Block> blockRegistry = BuiltInRegistries.BLOCK;
            Identifier identifier = Identifier.parse(blockId);
            Block block = blockRegistry.getValue(identifier);
            Optional<ResourceKey<Block>> resolvedKey = blockRegistry.getResourceKey(block);
            if (resolvedKey.isEmpty() || !resolvedKey.get().identifier().equals(identifier)) {
               ClientAccess.logger.warn("Ignoring block color for unknown block {}", blockId);
               return;
            }
            loaded.put(block, color);
         } catch (RuntimeException invalidEntry) {
            ClientAccess.logger.warn("Ignoring invalid block color entry {}", blockId, invalidEntry);
         }
      });

      this.colors.clear();
      this.colors.putAll(loaded);
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      JsonObject blocksJson = new JsonObject();
      this.colors.forEach((block, color) -> {
         Registry<Block> blockRegistry = BuiltInRegistries.BLOCK;
         Optional<ResourceKey<Block>> resourceKeyOption = blockRegistry.getResourceKey(block);
         resourceKeyOption.ifPresent(resourceKey -> {
            Identifier identifier = resourceKey.identifier();
            String blockId = identifier.toString();
            JsonElement encodedColor = COLOR_CODEC.toJson(color);
            blocksJson.add(blockId, encodedColor);
         });
      });
      root.add("blocks", blocksJson);
      return root;
   }

   @Override
   public String getConfigName() {
      return "colors.json";
   }

   public void putColor(Block block, Color color) {
      if (color == null) {
         this.colors.remove(block);
      } else {
         this.colors.put(block, color);
      }
   }
}
