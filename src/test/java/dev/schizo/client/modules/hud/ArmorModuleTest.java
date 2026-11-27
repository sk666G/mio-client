package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmorModuleTest {
   @Test
   void formatsPlainDurabilityWithoutTemporaryPercentRemoval() {
      assertEquals("73", ArmorModule.formatDurabilityText(73, false));
      assertEquals("0", ArmorModule.formatDurabilityText(0, false));
      assertEquals("-5", ArmorModule.formatDurabilityText(-5, false));
   }

   @Test
   void appendsPercentageOnlyWhenEnabled() {
      assertEquals("73%", ArmorModule.formatDurabilityText(73, true));
      assertEquals("100%", ArmorModule.formatDurabilityText(100, true));
   }
}
