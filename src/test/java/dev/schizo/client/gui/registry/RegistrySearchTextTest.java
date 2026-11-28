package dev.schizo.client.gui.registry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistrySearchTextTest {
   @Test void queryIsNormalizedOnceAndUpdatesWhenTextChanges() {
      var search=new RegistrySearchText();
      String first=search.query("  MINECRAFT:Stone  ");
      assertEquals("minecraft:stone",first);
      assertSame(first,search.query(new String("  MINECRAFT:Stone  ")));
      assertEquals("stone",search.query("STONE"));
      assertEquals("",search.query("   "));
   }

   @Test void matchesHumanLabelsAndNamespacedIds() {
      String label=RegistrySearchText.normalize("Smooth Stone");
      String id=RegistrySearchText.normalize("minecraft:smooth_stone");
      assertTrue(RegistrySearchText.matches(label,id,"smooth stone"));
      assertTrue(RegistrySearchText.matches(label,id,"minecraft:smooth_"));
      assertTrue(RegistrySearchText.matches(label,id,""));
      assertFalse(RegistrySearchText.matches(label,id,"other:smooth"));
      assertFalse(RegistrySearchText.matches(label,id,"diamond"));
   }

   @Test void normalizationIsIndependentOfDefaultLocale() {
      assertEquals("item",RegistrySearchText.normalize("ITEM"));
      assertEquals("i",RegistrySearchText.normalize("I"));
   }
}
