/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.utils.inventory.Kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

public final class KitRegistry extends Registry<Kit> implements ClientAccess, JsonConfigurable {
   private static final String CURRENT_KEY = "current";
   private static final int PLAYER_INVENTORY_SLOTS = 36;
   private String currentKitName;

   public void setCurrentKitName(String name) {
      this.currentKitName = name;
   }

   public static String getPotionSignature(ItemStack stack) {
      DataComponentType<PotionContents> potionContentsType = DataComponents.POTION_CONTENTS;
      PotionContents potionContents = stack.get(potionContentsType);
      if (potionContents != null) {
         ArrayList effectSignatures = new ArrayList();
         Consumer effectConsumer = (java.util.function.Consumer<net.minecraft.world.effect.MobEffectInstance>)(effect -> {
            String descriptionId = effect.getDescriptionId();
            int displayedAmplifier = effect.getAmplifier() + 1;
            effectSignatures.add(descriptionId + " " + displayedAmplifier);
         });

         potionContents.forEachEffect(effectConsumer, 1.0F);
         effectSignatures.sort(Comparator.comparing((Function<String, String>)effectSignature -> effectSignature));
         return String.join("", effectSignatures);
      } else {
         return null;
      }
   }

   public static Kit getCurrentKit(KitRegistry registry) {
      if (registry == null || registry.currentKitName == null) {
         return null;
      }
      return registry.findByName(registry.currentKitName).orElse(null);
   }

   @Override
   public boolean registerEntry(NamedEntry entry) {
      if (!(entry instanceof Kit kit) || !isValidName(kit.getName())) {
         return false;
      }
      if (this.findByName(kit.getName()).isPresent()) {
         return false;
      }
      return this.entries.add(kit);
   }

   public Optional<Kit> findByName(String name) {
      if (!isValidName(name)) {
         return Optional.empty();
      }
      return this.entries.stream()
         .filter(kit -> kit != null && kit.getName() != null && kit.getName().equalsIgnoreCase(name))
         .findFirst();
   }

   @Override
   public boolean unregisterEntry(NamedEntry entry) {
      return entry instanceof Kit kit && this.unregisterKit(kit);
   }

   public boolean unregisterKit(Kit kit) {
      if (kit == null) {
         return false;
      }
      boolean removed = this.entries.removeIf(candidate -> candidate == kit);
      if (removed && this.currentKitName != null && this.currentKitName.equalsIgnoreCase(kit.getName())) {
         this.currentKitName = null;
      }
      return removed;
   }

   public boolean replaceKit(Kit previous, Kit replacement) {
      if (previous == null
         || replacement == null
         || !isValidName(replacement.getName())
         || !this.isRegisteredInstance(previous)) {
         return false;
      }

      for (Kit existing : this.entries) {
         if (existing != previous && hasSameName(existing, replacement)) {
            return false;
         }
      }

      boolean wasCurrent = this.currentKitName != null
         && previous.getName() != null
         && this.currentKitName.equalsIgnoreCase(previous.getName());

      if (this.entries instanceof List<?> rawList) {
         @SuppressWarnings("unchecked")
         List<Kit> kits = (List<Kit>)rawList;
         int previousIndex = -1;
         for (int index = 0; index < kits.size(); index++) {
            if (kits.get(index) == previous) {
               previousIndex = index;
               break;
            }
         }
         if (previousIndex < 0) {
            return false;
         }
         kits.set(previousIndex, replacement);
      } else {
         if (!this.entries.removeIf(candidate -> candidate == previous)) {
            return false;
         }
         if (!this.registerEntry(replacement)) {
            this.registerEntry(previous);
            return false;
         }
      }

      if (wasCurrent) {
         this.currentKitName = replacement.getName();
      }
      return true;
   }

   public boolean renameKit(Kit kit, String targetName) {
      if (kit == null || !isValidName(targetName)) {
         return false;
      }

      Kit renamed = new Kit(targetName);
      renamed.getItemsBySlot().putAll(kit.getItemsBySlot());
      return this.replaceKit(kit, renamed);
   }

   public boolean upsertKit(Kit kit) {
      if (kit == null || !isValidName(kit.getName())) {
         return false;
      }

      Optional<Kit> existing = this.findByName(kit.getName());
      return existing.isPresent()
         ? this.replaceKit(existing.get(), kit)
         : this.registerEntry(kit);
   }

   private boolean isRegisteredInstance(Kit expected) {
      for (Kit registered : this.entries) {
         if (registered == expected) {
            return true;
         }
      }
      return false;
   }

   static boolean hasSameName(Kit first, Kit second) {
      return first != null
         && second != null
         && first.getName() != null
         && second.getName() != null
         && first.getName().equalsIgnoreCase(second.getName());
   }

   static boolean isValidName(String name) {
      return name != null
         && !name.isBlank()
         && !CURRENT_KEY.equalsIgnoreCase(name);
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject root)) {
         throw new IllegalArgumentException("Kit configuration must be a JSON object");
      }

      String requestedCurrent = null;
      JsonElement currentElement = root.get(CURRENT_KEY);
      if (currentElement != null
         && currentElement.isJsonPrimitive()
         && currentElement.getAsJsonPrimitive().isString()) {
         requestedCurrent = currentElement.getAsString();
      }

      List<Kit> loaded = new ArrayList<>();
      for (Entry<String, JsonElement> kitEntry : root.entrySet()) {
         String kitName = kitEntry.getKey();
         if (CURRENT_KEY.equalsIgnoreCase(kitName)
            || !isValidName(kitName)
            || !(kitEntry.getValue() instanceof JsonArray itemArray)) {
            continue;
         }

         Kit kit = new Kit(kitName);
         for (JsonElement itemJson : itemArray) {
            if (!(itemJson instanceof JsonObject slotObject)
               || !slotObject.has("slot")
               || !slotObject.has("item")) {
               continue;
            }

            try {
               int slot = slotObject.get("slot").getAsInt();
               if (slot < 0 || slot >= PLAYER_INVENTORY_SLOTS) {
                  continue;
               }
               Kit.ItemDescriptor descriptor = parseDescriptor(slotObject.get("item"));
               if (descriptor != null) {
                  kit.getItemsBySlot().put(slot, descriptor);
               }
            } catch (RuntimeException invalidItem) {
               ClientAccess.logger.warn("Skipping invalid item in kit {}", kitName, invalidItem);
            }
         }

         if (loaded.stream().noneMatch(existing -> hasSameName(existing, kit))) {
            loaded.add(kit);
         }
      }

      this.entries.clear();
      this.entries.addAll(loaded);
      String selectedName = requestedCurrent;
      this.currentKitName = selectedName == null
         ? null
         : this.findByName(selectedName).map(Kit::getName).orElse(null);
   }

   private static Kit.ItemDescriptor parseDescriptor(JsonElement itemElement) {
      int loadedItemId;
      String potionSignature = null;

      if (itemElement instanceof JsonObject itemObject) {
         JsonElement idElement = itemObject.get("id");
         if (idElement == null || !idElement.isJsonPrimitive()) {
            return null;
         }

         JsonPrimitive idPrimitive = idElement.getAsJsonPrimitive();
         if (idPrimitive.isNumber()) {
            loadedItemId = idPrimitive.getAsInt();
            Item resolved = Item.byId(loadedItemId);
            if (Item.getId(resolved) != loadedItemId) {
               return null;
            }
         } else if (idPrimitive.isString()) {
            Identifier identifier = Identifier.parse(idPrimitive.getAsString());
            DefaultedRegistry<Item> itemRegistry = BuiltInRegistries.ITEM;
            Item resolved = itemRegistry.getValue(identifier);
            Optional<ResourceKey<Item>> resourceKey = itemRegistry.getResourceKey(resolved);
            if (resourceKey.isEmpty() || !resourceKey.get().identifier().equals(identifier)) {
               return null;
            }
            loadedItemId = Item.getId(resolved);
         } else {
            return null;
         }

         JsonElement dataElement = itemObject.get("data");
         if (dataElement != null) {
            if (!dataElement.isJsonPrimitive() || !dataElement.getAsJsonPrimitive().isString()) {
               return null;
            }
            potionSignature = dataElement.getAsString();
         }
      } else if (itemElement instanceof JsonPrimitive primitiveItem && primitiveItem.isNumber()) {
         loadedItemId = primitiveItem.getAsInt();
         Item resolved = Item.byId(loadedItemId);
         if (Item.getId(resolved) != loadedItemId) {
            return null;
         }
      } else {
         return null;
      }

      return new Kit.ItemDescriptor(loadedItemId, potionSignature);
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      if (this.currentKitName != null) {
         root.addProperty(CURRENT_KEY, this.currentKitName);
      }

      for (Kit kit : this.entries) {
         JsonArray items = new JsonArray();
         kit.getItemsBySlot().forEach((BiConsumer<Integer, Kit.ItemDescriptor>)(slot, descriptor) -> {
            JsonObject itemObject = new JsonObject();
            int itemId = descriptor.itemId();
            Item item = Item.byId(itemId);
            Optional<ResourceKey<Item>> itemResourceKey = BuiltInRegistries.ITEM.getResourceKey(item);
            itemResourceKey.ifPresent(resourceKey -> {
               Identifier identifier = resourceKey.identifier();
               itemObject.addProperty("id", identifier.toString());
            });
            if (descriptor.potionSignature() != null) {
               itemObject.addProperty("data", descriptor.potionSignature());
            }

            JsonObject slotObject = new JsonObject();
            slotObject.addProperty("slot", slot);
            slotObject.add("item", itemObject);
            items.add(slotObject);
         });
         root.add(kit.getName(), items);
      }
      return root;
   }

   @Override
   public String getConfigName() {
      return "kits.json";
   }

   private static int capturedItemId(ItemStack stack) {
      Item item = stack.getItem();
      return Item.getId(item);
   }

   public boolean captureInventory(String name) {
      if (!isValidName(name) || this.isWorldUnavailable()) {
         return false;
      }

      Kit kit = new Kit(name);
      int slot = 0;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      Inventory inventory = player.getInventory();
      NonNullList inventoryItems = inventory.getNonEquipmentItems();

      for (Iterator stacks = inventoryItems.iterator(); stacks.hasNext(); slot++) {
         ItemStack stack = (ItemStack)stacks.next();
         kit.getItemsBySlot().put(
            slot,
            new Kit.ItemDescriptor(capturedItemId(stack), getPotionSignature(stack))
         );
      }

      if (!this.upsertKit(kit)) {
         return false;
      }
      this.setCurrentKitName(kit.getName());
      return true;
   }
}
