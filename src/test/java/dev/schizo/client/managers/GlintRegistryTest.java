package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GlintRegistryTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void missingLegacyListPreservesExistingSide() {
      GlintRegistry registry = new GlintRegistry();
      registry.getItems(GlintRegistry.GlintMode.NEVER).getValue().add(Items.DIAMOND);
      registry.getItems(GlintRegistry.GlintMode.ALWAYS).getValue().add(Items.NETHER_STAR);

      JsonObject partial = new JsonObject();
      JsonArray replacementNever = new JsonArray();
      replacementNever.add("minecraft:stone");
      partial.add("never", replacementNever);

      registry.fromJson(partial);

      assertEquals(
         java.util.Set.of(Items.STONE),
         java.util.Set.copyOf(registry.getItems(GlintRegistry.GlintMode.NEVER).getValue())
      );
      assertEquals(
         java.util.Set.of(Items.NETHER_STAR),
         java.util.Set.copyOf(registry.getItems(GlintRegistry.GlintMode.ALWAYS).getValue())
      );
   }

   @Test
   void malformedSecondListDoesNotPartiallyReplaceExistingState() {
      GlintRegistry registry = new GlintRegistry();
      registry.getItems(GlintRegistry.GlintMode.NEVER).getValue().add(Items.DIAMOND);
      registry.getItems(GlintRegistry.GlintMode.ALWAYS).getValue().add(Items.NETHER_STAR);

      JsonObject malformed = new JsonObject();
      JsonArray replacementNever = new JsonArray();
      replacementNever.add("minecraft:stone");
      malformed.add("never", replacementNever);
      malformed.addProperty("always", "not-an-array");

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(malformed));

      assertEquals(
         java.util.Set.of(Items.DIAMOND),
         java.util.Set.copyOf(registry.getItems(GlintRegistry.GlintMode.NEVER).getValue())
      );
      assertEquals(
         java.util.Set.of(Items.NETHER_STAR),
         java.util.Set.copyOf(registry.getItems(GlintRegistry.GlintMode.ALWAYS).getValue())
      );
   }
}
