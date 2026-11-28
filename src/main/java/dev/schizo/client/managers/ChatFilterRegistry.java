/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.chat.ChatFilterEntry;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.modules.misc.ChatFilterModule;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.Setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;

public class ChatFilterRegistry implements ClientAccess, JsonConfigurable {
   private static ChatFilterModule chatFilterModule;
   private final List<ChatFilterEntry> filters = Collections.synchronizedList(new ArrayList<>());

   public int size() {
      synchronized (this.filters) {
         return this.filters.size();
      }
   }

   public Stream<String> streamIds() {
      synchronized (this.filters) {
         return this.filters.stream().map(ChatFilterEntry::id).toList().stream();
      }
   }

   public List<String> describeFilters() {
      synchronized (this.filters) {
         return this.filters.stream().map(entry -> "%s%s%s: \"%s\"".formatted(
            ChatFormatting.GRAY, entry.id(), ChatFormatting.RESET, entry.filter())).toList();
      }
   }

   public static void bindModules(ModuleRegistry registry) {
      chatFilterModule = (ChatFilterModule)java.util.Objects.requireNonNull(
         registry.getModule(ChatFilterModule.class),
         "Required module is not registered: ChatFilterModule"
      );
   }

   public boolean isEmpty() {
      synchronized (this.filters) {
         return this.filters.isEmpty();
      }
   }

   public void add(String id, String filter) {
      synchronized (this.filters) {
         ChatFilterEntry entry = new ChatFilterEntry(id, filter);
         this.filters.add(entry);
      }
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject root)
         || !(root.get("filters") instanceof JsonArray serializedFilters)) {
         throw new IllegalArgumentException("Chat-filter configuration must contain a filters array");
      }

      List<ChatFilterEntry> loaded = new ArrayList<>();
      for (JsonElement filterElement : serializedFilters) {
         if (!(filterElement instanceof JsonObject filterObject)
            || !filterObject.has("id")
            || !filterObject.has("filter")) {
            continue;
         }

         try {
            String id = filterObject.get("id").getAsString();
            String filter = filterObject.get("filter").getAsString();
            if (loaded.stream().noneMatch(existing -> existing.id().equalsIgnoreCase(id))) {
               loaded.add(new ChatFilterEntry(id, filter));
            }
         } catch (RuntimeException invalidFilter) {
            ClientAccess.logger.warn("Skipping invalid chat-filter entry", invalidFilter);
         }
      }

      synchronized (this.filters) {
         this.filters.clear();
         this.filters.addAll(loaded);
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      JsonArray serializedFilters = new JsonArray();
      synchronized (this.filters) {
         for (ChatFilterEntry entry : this.filters) {
            JsonObject filterObject = new JsonObject();
            filterObject.addProperty("id", entry.id());
            filterObject.addProperty("filter", entry.filter());
            serializedFilters.add(filterObject);
         }
      }
      root.add("filters", serializedFilters);
      return root;
   }

   @Override
   public String getConfigName() {

      return "chatfilter.json";
   }

   public void remove(ChatFilterEntry entry) {
      synchronized (this.filters) {
         this.filters.remove(entry);
      }
   }

   public boolean matchesMessage(String message) {
      boolean caseSensitive = Boolean.TRUE.equals(chatFilterModule.caseSensitiveSetting.getValue());
      String comparedMessage = caseSensitive ? message : message.toLowerCase(Locale.ROOT);

      synchronized (this.filters) {
         for (ChatFilterEntry entry : this.filters) {
            String filter = entry.filter();
            String comparedFilter = caseSensitive ? filter : filter.toLowerCase(Locale.ROOT);
            if (comparedMessage.contains(comparedFilter)) {
               return true;
            }
         }
         return false;
      }
   }

   public boolean containsId(String id) {
      synchronized (this.filters) {
         for (ChatFilterEntry entry : this.filters) {
            if (entry.id().equalsIgnoreCase(id)) {
               return true;
            }
         }
         return false;
      }
   }

   public Optional<ChatFilterEntry> findById(String id) {
      synchronized (this.filters) {
         Stream<ChatFilterEntry> matchingFilters = this.filters.stream().filter(entry -> entry.id().equalsIgnoreCase(id));
         return matchingFilters.findAny();
      }
   }

}
