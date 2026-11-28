package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

class MetricsModuleTest {
   @Test
   void integerMetricFormattingPreservesExistingColorAndSuffixLayout() {
      assertEquals(
         "Ping " + ChatFormatting.WHITE + "42ms",
         MetricsModule.formatColoredInteger("Ping ", 42, "ms")
      );
      assertEquals(
         "FPS " + ChatFormatting.WHITE + "144",
         MetricsModule.formatColoredInteger("FPS ", 144, "")
      );
   }
}
