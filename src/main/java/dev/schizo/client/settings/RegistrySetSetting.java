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
import java.util.function.Function;
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
      this.replaceEntries(this.parseJsonEntries(json));
   }

   public Set<E> parseJsonEntries(JsonElement json) {
      return resolveJsonEntries(json, this::parseEntry);
   }

   public void replaceEntries(Collection<? extends E> entries) {
      if (entries == null) {
         throw new IllegalArgumentException("Registry-set replacement must not be null");
      }

      Set<E> values = this.getValue();
      values.clear();
      values.addAll(entries);
   }

   static <T> Set<T> resolveJsonEntries(JsonElement json, Function<String, T> resolver) {
      if (!(json instanceof JsonArray array)) {
         throw new IllegalArgumentException("Registry-set configuration must be a JSON array");
      }
      if (resolver == null) {
         throw new IllegalArgumentException("Registry-set resolver must not be null");
      }

      Set<T> loaded = new ObjectOpenHashSet<>();
      for (JsonElement element : array) {
         if (element == null
            || !element.isJsonPrimitive()
            || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Registry-set entries must be string identifiers");
         }

         String identifierText = element.getAsString();
         T entry;
         try {
            entry = resolver.apply(identifierText);
         } catch (RuntimeException invalidIdentifier) {
            throw new IllegalArgumentException(
               "Invalid registry-set identifier: " + identifierText,
               invalidIdentifier
            );
         }

         if (entry != null) {
            loaded.add(entry);
         }
      }
      return loaded;
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
