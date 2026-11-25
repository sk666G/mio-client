package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.utils.inventory.TradeDefinition;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class TradeRegistryTest {
   @Test
   @SuppressWarnings("unchecked")
   void unregisterByNameRemovesTheStoredTradeCaseInsensitively() {
      TradeRegistry registry = new TradeRegistry();
      TradeDefinition trade = trade("Mending");
      registry.getRegistry().add(trade);

      assertTrue(registry.unregisterByName("mEnDiNg"));
      assertTrue(registry.getRegistry().isEmpty());
   }

   @Test
   @SuppressWarnings("unchecked")
   void unregisterByNameReportsMissingAndNullNames() {
      TradeRegistry registry = new TradeRegistry();
      registry.getRegistry().add(trade("Tools"));

      assertFalse(registry.unregisterByName("Armor"));
      assertFalse(registry.unregisterByName(null));
      assertEquals(1, registry.getRegistry().size());
   }

   private static TradeDefinition trade(String name) {
      return new TradeDefinition(name, null, null, 1, new ArrayList<>());
   }

   @Test
   void tradeNamesUseCaseInsensitiveIdentity() {
      TradeDefinition first = trade("Shop");
      TradeDefinition sameName = trade("shop");
      TradeDefinition different = trade("Other");

      assertTrue(TradeRegistry.hasSameName(first, sameName));
      assertFalse(TradeRegistry.hasSameName(first, different));
      assertFalse(TradeRegistry.hasSameName(first, null));
   }

}
