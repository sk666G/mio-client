/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class RegistrySetSetting<E> extends SetSetting<E> {
   private final Registry<E> registry;

   @SafeVarargs
   public RegistrySetSetting(String name, Registry<E> entryRegistry, E... defaultEntries) {
      super(name, defaultEntries);
      this.registry = entryRegistry;
   }

   @SafeVarargs
   public RegistrySetSetting(String name, Registry<E> entryRegistry, Predicate<? super Set<E>> visibility, E... defaultEntries) {
      super(name, visibility, defaultEntries);
      this.registry = entryRegistry;
   }

   @Override
   public E parseEntry(String text) {
      Registry<E> lookupRegistry = this.registry;
      Identifier identifier = Identifier.parse(text);
      Optional<Reference<E>> holder = lookupRegistry.get(identifier);
      return holder.map(Reference::value).orElse(null);
   }

   @Override
   public Collection<E> getAvailableEntries() {
      return this.registry.stream().toList();
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json == null || !json.isJsonArray()) {
         throw new IllegalArgumentException("Registry-set configuration must be a JSON array");
      }

      Set<E> loaded = new ObjectOpenHashSet<>();
      JsonArray array = json.getAsJsonArray();
      for (JsonElement element : array) {
         if (element == null
            || !element.isJsonPrimitive()
            || !element.getAsJsonPrimitive().isString()) {
            continue;
         }

         try {
            Identifier identifier = Identifier.parse(element.getAsString());
            Optional<Reference<E>> holder = this.registry.get(identifier);
            holder.map(Reference::value).ifPresent(loaded::add);
         } catch (RuntimeException invalidIdentifier) {
            // Skip only the malformed entry; publish the remaining valid set atomically.
         }
      }

      Set<E> values = this.getValue();
      values.clear();
      values.addAll(loaded);
   }

   public void modifyEntries(Consumer<Set<E>> editor) {
      editor.accept(this.getValue());
      this.runChangeCallback();
   }

   @Override
   public String formatEntry(E entry) {
      Registry<E> entryRegistry = this.registry;
      Identifier identifier = entryRegistry.getKey(entry);
      if (identifier == null) {
         return null;
      }

      return identifier.toShortLanguageKey();
   }

   public Registry<E> getRegistry() {
      return this.registry;
   }
}
