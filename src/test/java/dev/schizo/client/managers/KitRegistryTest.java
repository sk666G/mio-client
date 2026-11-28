package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.utils.inventory.Kit;
import org.junit.jupiter.api.Test;

class KitRegistryTest {
   @Test
   void registrationUsesCaseInsensitiveNamesAndRejectsReservedCurrentKey() {
      KitRegistry registry = new KitRegistry();

      assertTrue(registry.registerEntry(new Kit("Crystal")));
      assertFalse(registry.registerEntry(new Kit("crystal")));
      assertFalse(registry.registerEntry(new Kit("current")));
      assertFalse(registry.registerEntry(new Kit("CURRENT")));
      assertEquals(1, registry.getRegistry().size());
      assertEquals("Crystal", registry.findByName("CRYSTAL").orElseThrow().getName());
   }

   @Test
   void reloadReplacesOldKitsAndCanonicalizesCurrentSelection() {
      KitRegistry registry = new KitRegistry();
      assertTrue(registry.registerEntry(new Kit("old")));
      registry.setCurrentKitName("old");

      JsonObject root = new JsonObject();
      root.addProperty("current", "ALPHA");
      root.add("Alpha", new JsonArray());
      root.add("Beta", new JsonArray());

      registry.fromJson(root);

      assertTrue(registry.findByName("old").isEmpty());
      assertEquals(2, registry.getRegistry().size());
      assertEquals("Alpha", KitRegistry.getCurrentKit(registry).getName());
   }

   @Test
   void reloadWithoutCurrentSelectionClearsStaleSelection() {
      KitRegistry registry = new KitRegistry();
      assertTrue(registry.registerEntry(new Kit("old")));
      registry.setCurrentKitName("old");

      JsonObject root = new JsonObject();
      root.add("new", new JsonArray());

      registry.fromJson(root);

      assertNull(KitRegistry.getCurrentKit(registry));
      assertFalse(registry.toJson().getAsJsonObject().has("current"));
   }

   @Test
   void renamePreservesItemsAndCurrentSelection() {
      KitRegistry registry = new KitRegistry();
      Kit kit = new Kit("tools");
      Kit.ItemDescriptor descriptor = new Kit.ItemDescriptor(123, "signature");
      kit.getItemsBySlot().put(5, descriptor);
      assertTrue(registry.registerEntry(kit));
      registry.setCurrentKitName("TOOLS");

      assertTrue(registry.renameKit(kit, "utility"));

      Kit renamed = registry.findByName("UTILITY").orElseThrow();
      assertSame(descriptor, renamed.getItemsBySlot().get(5));
      assertSame(renamed, KitRegistry.getCurrentKit(registry));
      assertTrue(registry.findByName("tools").isEmpty());
   }

   @Test
   void renameRejectsCollisionAndReservedNameWithoutRemovingOriginal() {
      KitRegistry registry = new KitRegistry();
      Kit first = new Kit("first");
      Kit second = new Kit("second");
      assertTrue(registry.registerEntry(first));
      assertTrue(registry.registerEntry(second));

      assertFalse(registry.renameKit(first, "SECOND"));
      assertFalse(registry.renameKit(first, "current"));

      assertSame(first, registry.findByName("first").orElseThrow());
      assertSame(second, registry.findByName("second").orElseThrow());
      assertEquals(2, registry.getRegistry().size());
   }

   @Test
   void unregisteringCurrentKitClearsSelection() {
      KitRegistry registry = new KitRegistry();
      Kit kit = new Kit("selected");
      assertTrue(registry.registerEntry(kit));
      registry.setCurrentKitName("SELECTED");

      assertTrue(registry.unregisterKit(kit));

      assertNull(KitRegistry.getCurrentKit(registry));
      assertTrue(registry.getRegistry().isEmpty());
   }

   @Test
   void staleEqualNamedKitCannotRemoveLiveInstance() {
      KitRegistry registry = new KitRegistry();
      Kit live = new Kit("same");
      Kit stale = new Kit("same");
      assertTrue(registry.registerEntry(live));
      registry.setCurrentKitName("same");

      assertFalse(registry.unregisterKit(stale));
      assertFalse(registry.unregisterEntry(stale));

      assertSame(live, registry.findByName("same").orElseThrow());
      assertSame(live, KitRegistry.getCurrentKit(registry));
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void staleEqualNamedKitCannotReplaceOrRenameLiveInstance() {
      KitRegistry registry = new KitRegistry();
      Kit live = new Kit("same");
      Kit stale = new Kit("same");
      Kit replacement = new Kit("replacement");
      assertTrue(registry.registerEntry(live));

      assertFalse(registry.replaceKit(stale, replacement));
      assertFalse(registry.renameKit(stale, "renamed"));

      assertSame(live, registry.findByName("same").orElseThrow());
      assertTrue(registry.findByName("replacement").isEmpty());
      assertTrue(registry.findByName("renamed").isEmpty());
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void malformedRootDoesNotDiscardExistingState() {
      KitRegistry registry = new KitRegistry();
      Kit keep = new Kit("keep");
      assertTrue(registry.registerEntry(keep));
      registry.setCurrentKitName("keep");

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonArray()));

      assertSame(keep, registry.findByName("keep").orElseThrow());
      assertSame(keep, KitRegistry.getCurrentKit(registry));
   }
}
