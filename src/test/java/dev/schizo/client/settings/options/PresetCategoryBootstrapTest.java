package dev.schizo.client.settings.options;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PresetCategoryBootstrapTest {
   @Test
   void enumInitializationDoesNotRequireConfigurationManagerBootstrap() {
      assertEquals(PresetCategory.HUD, PresetCategory.findByName("hud"));
      assertEquals("Hud", PresetCategory.HUD.getName());
      assertEquals(PresetCategory.MACRO, PresetCategory.findByName("MACRO"));
   }
}
