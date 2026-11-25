package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.utils.inventory.TradeDefinition;
import java.util.ArrayList;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class TradeRegistryTest {
   @Test
   void unregisterByNameRemovesTheStoredTradeCaseInsensitively() {
      TradeRegistry registry = new TradeRegistry();
      TradeDefinition trade = new TradeDefinition(
         "Mending",
         Items.EMERALD,
         Items.ENCHANTED_BOOK,
         32,
         new ArrayList<>()
      );

      assertTrue(registry.registerTrade(trade));
      assertTrue(registry.unregisterByName("mEnDiNg"));
      assertTrue(registry.getRegistry().isEmpty());
   }

   @Test
   void unregisterByNameReportsMissingAndNullNames() {
      TradeRegistry registry = new TradeRegistry();
      TradeDefinition trade = new TradeDefinition(
         "Tools",
         Items.EMERALD,
         Items.DIAMOND_PICKAXE,
         24,
         new ArrayList<>()
      );
      assertTrue(registry.registerTrade(trade));

      assertFalse(registry.unregisterByName("Armor"));
      assertFalse(registry.unregisterByName(null));
      assertEquals(1, registry.getRegistry().size());
   }
}
