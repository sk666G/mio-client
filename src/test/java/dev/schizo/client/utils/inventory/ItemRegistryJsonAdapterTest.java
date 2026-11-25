package dev.schizo.client.utils.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonPrimitive;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ItemRegistryJsonAdapterTest {
   @Test
   void exactIdentifierValidationRejectsFallbackMismatch() {
      Identifier requested = Identifier.parse("minecraft:diamond");
      Identifier resolved = Identifier.parse("minecraft:air");

      assertTrue(ItemRegistryJsonAdapter.identifiersMatch(requested, requested));
      assertFalse(ItemRegistryJsonAdapter.identifiersMatch(requested, resolved));
      assertFalse(ItemRegistryJsonAdapter.identifiersMatch(requested, null));
      assertFalse(ItemRegistryJsonAdapter.identifiersMatch(null, requested));
   }

   @Test
   void blankIdentifierIsRejectedBeforeRegistryLookup() {
      ItemRegistryJsonAdapter.ItemAdapter adapter = new ItemRegistryJsonAdapter.ItemAdapter();

      assertThrows(
         IllegalArgumentException.class,
         () -> adapter.deserialize(new JsonPrimitive("   "), Object.class, null)
      );
   }
}
