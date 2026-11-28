package dev.schizo.client.config;

import dev.schizo.client.managers.ContextProfileRegistry.Rule;
import dev.schizo.client.settings.options.PresetCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContextRulePreviewTest {
   @Test void replacementPreservesOrderAndOriginalRules() {
      Rule original = Rule.create("*", "any", "any", PresetCategory.COLORS, "old");
      Rule proposed = Rule.create("*", "any", "any", PresetCategory.COLORS, "new");
      var result = ContextRulePreview.simulate(List.of(original), proposed);
      assertTrue(result.replaces());
      assertEquals(List.of(original), result.original());
      assertEquals(List.of(original), result.before());
      assertEquals(List.of(proposed), result.after());
      assertEquals("singleplayer", result.server());
   }

   @Test void specificityAndAllOverlapUseActualApplicationOrder() {
      Rule broad = Rule.create("*", "any", "any", PresetCategory.COLORS, "broad");
      Rule all = Rule.create("*", "any", "any", PresetCategory.ALL, "general");
      Rule specific = Rule.create("singleplayer", "overworld", "survival", PresetCategory.COLORS, "specific");
      var result = ContextRulePreview.simulate(List.of(broad, all), specific);
      assertFalse(result.replaces());
      assertEquals(List.of(all, specific), result.after());
      assertEquals(1, result.shadowed());
      assertTrue(result.allOverlap());
   }
}
