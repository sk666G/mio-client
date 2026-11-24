package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class PistonCrystalModulePolicyTest {
   @Test
   void requiresARealPowerItemBeforeStartingTheBuildCycle() {
      assertTrue(PistonCrystalPlanner.hasPowerSource(Items.REDSTONE_BLOCK));
      assertTrue(PistonCrystalModule.hasPowerSource(Items.REDSTONE_TORCH));
      assertFalse(PistonCrystalModule.hasPowerSource(Items.AIR));
      assertFalse(PistonCrystalModule.hasPowerSource(null));
   }
}
