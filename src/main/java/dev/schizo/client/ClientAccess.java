/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client;

import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.utils.inventory.ItemRegistryJsonAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ClientAccess {
   Logger logger = LoggerFactory.getLogger(SchizoClient.NAME);
   Minecraft schizoMinecraft = Minecraft.getInstance();
   Gson gson = new GsonBuilder()
      .registerTypeAdapter(Item.class, new ItemRegistryJsonAdapter.ItemAdapter())
      .registerTypeAdapter(BlockItem.class, new ItemRegistryJsonAdapter.BlockItemAdapter())
      .setPrettyPrinting()
      .create();
   EventBus eventBus = new EventBus();
   ExecutorService executorService = Executors.newCachedThreadPool(
      Thread.ofPlatform().daemon().name("Schizo-Worker-", 0L).factory()
   );

   default boolean isWorldUnavailable() {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      return minecraft == null || minecraft.player == null || minecraft.level == null;
   }

   static String getVersion() {
      return SchizoClient.VERSION;
   }

   static boolean isLocalPlayer(Object candidate) {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      return minecraft != null && minecraft.player == candidate;
   }

}
