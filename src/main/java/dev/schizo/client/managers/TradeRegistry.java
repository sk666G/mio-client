/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.inventory.TradeDefinition;
import dev.schizo.client.utils.inventory.TradeOfferSnapshot;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.managers.Registry;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.NamedEntry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;
import net.minecraft.world.item.Items;

public final class TradeRegistry extends Registry<TradeDefinition> implements ClientAccess, JsonConfigurable {
   public TradeDefinition createDefault(String name) {
      return new TradeDefinition(name, Items.EMERALD, Items.DIAMOND_SWORD, 64, new ArrayList<>());
   }

   @Override
   public boolean register(Object entry) {
      return this.registerTrade((TradeDefinition)entry);
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject root)
         || !(root.get("trades") instanceof JsonArray trades)) {
         throw new IllegalArgumentException("Trade configuration must contain a trades array");
      }

      ArrayList<TradeDefinition> loaded = new ArrayList<>();
      for (JsonElement element : trades) {
         try {
            TradeDefinition trade = ClientAccess.gson.fromJson(element, TradeDefinition.class);
            if (!isValidTrade(trade)
               || loaded.stream().anyMatch(existing -> hasSameName(existing, trade))) {
               continue;
            }
            loaded.add(trade);
         } catch (RuntimeException invalidTrade) {
            ClientAccess.logger.warn("Skipping invalid trade entry", invalidTrade);
         }
      }

      this.entries.clear();
      this.entries.addAll(loaded);
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      JsonArray trades = new JsonArray();
      Iterator<TradeDefinition> iterator = this.getRegistry().iterator();
      while (iterator.hasNext()) {
         TradeDefinition trade = iterator.next();
         JsonElement tradeJson = ClientAccess.gson.toJsonTree(trade);
         trades.add(tradeJson);
      }
      root.add("trades", trades);
      return root;
   }

   public boolean unregisterByName(String name) {
      if (name == null) {
         return false;
      }

      return this.entries.removeIf(trade ->
         trade != null
            && trade.getName() != null
            && trade.getName().equalsIgnoreCase(name)
      );
   }

   @Override
   public String getConfigName() {
      return "trades.json";
   }

   @Override
   public boolean registerEntry(NamedEntry entry) {
      return this.registerTrade((TradeDefinition)entry);
   }

   public boolean matches(TradeOfferSnapshot offer) {
      Predicate<TradeDefinition> predicate = trade -> trade.matches(offer);
      return this.findFirst(predicate).isPresent();
   }

   public boolean registerTrade(TradeDefinition trade) {
      if (!isValidTrade(trade)) {
         return false;
      }
      if (this.entries.stream().anyMatch(existing -> hasSameName(existing, trade))) {
         return false;
      }

      return this.entries.add(trade);
   }

   static boolean isValidTrade(TradeDefinition trade) {
      return trade != null
         && trade.getName() != null
         && !trade.getName().isBlank()
         && trade.from() != null
         && trade.to() != null
         && trade.enchants() != null
         && trade.maxPrice() >= 1
         && trade.maxPrice() <= 64
         && (trade.from() == Items.EMERALD || trade.to() == Items.EMERALD);
   }

   static boolean hasSameName(TradeDefinition first, TradeDefinition second) {
      return first != null
         && second != null
         && first.getName() != null
         && second.getName() != null
         && first.getName().equalsIgnoreCase(second.getName());
   }

}
