package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class PistonCrystalModulePolicyTest {
   @Test
   void requiresARealPowerItemBeforeStartingTheBuildCycle() {
      assertTrue(PistonCrystalPlanner.hasPowerSource(Items.REDSTONE_BLOCK));
      assertTrue(PistonCrystalPlanner.hasPowerSource(Items.REDSTONE_TORCH));
      assertFalse(PistonCrystalPlanner.hasPowerSource(Items.AIR));
      assertFalse(PistonCrystalPlanner.hasPowerSource(null));
   }
}
