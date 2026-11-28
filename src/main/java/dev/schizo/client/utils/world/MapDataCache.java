/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.world;

import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.config.ConfigPathGuard;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.utils.CompressionUtils;
import dev.schizo.client.utils.FileUtils;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.PacketEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.DataOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class MapDataCache implements ClientAccess {
   private static final long SAVE_INTERVAL_NANOS = java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
   private final Map<String, MapItemSavedData> mapDataByName = new HashMap<>();
   private final Map<String, CompletableFuture<Void>> pendingSavesByAddress = new ConcurrentHashMap<>();
   private long lastSaveNanos;
   private boolean preserveUnreadableFile;
   private String serverAddress;
   private volatile Object session = new Object();

   public MapDataCache() {
      ClientAccess.eventBus.subscribe(this);
   }

   public void load(String address) {
      this.mapDataByName.clear();
      Path filePath;
      try {
         Path mapsDirectory = ConfigPathGuard.ensureDirectory(
            ConfigurationManager.ROOT_DIRECTORY,
            ConfigurationManager.MAPS_DIRECTORY
         );
         filePath = mapsDirectory.resolve(FontRenderer.sanitizeFileName(address) + ".data");
      } catch (IOException unsafeDirectory) {
         this.preserveUnreadableFile = true;
         ClientAccess.logger.warn("Could not access map cache directory", unsafeDirectory);
         return;
      }
      this.preserveUnreadableFile = false;
      if (Files.notExists(filePath, LinkOption.NOFOLLOW_LINKS)) {
         return;
      }
      try {
         this.mapDataByName.putAll(MapCacheReader.read(filePath, (scale, colors) -> {
            MapItemSavedData data = MapItemSavedData.createForClient(scale, true, null);
            System.arraycopy(colors, 0, data.colors, 0, colors.length);
            return data;
         }));
      } catch (IOException | RuntimeException failure) {
         this.preserveUnreadableFile = true;
         ClientAccess.logger.warn("Could not load map cache {}; preserving the file", filePath, failure);
      }
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      long now = System.nanoTime();
      if (this.serverAddress != null
         && now - this.lastSaveNanos >= SAVE_INTERVAL_NANOS
         && !this.hasPendingSave(this.serverAddress)) {
         this.lastSaveNanos = now;
         byte[] snapshot = this.serializeMaps();
         if (snapshot != null) {
            this.queueSave(this.serverAddress, snapshot);
         }
      }
   }

   @EventHandler
   private void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      this.session = new Object();
      String address = event.getServerAddress();
      this.awaitPendingSave(address);
      this.lastSaveNanos = System.nanoTime();
      this.serverAddress = address;
      this.load(address);
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.session = new Object();
      if (this.serverAddress != null) {
         byte[] snapshot = this.serializeMaps();
         if (snapshot != null) {
            this.queueSave(this.serverAddress, snapshot);
         }
      }
      this.serverAddress = null;
      this.mapDataByName.clear();
      this.preserveUnreadableFile = false;
   }

   public MapItemSavedData getMapData(ItemStack stack, MapId mapId) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      if (level == null) return null;
      MapItemSavedData worldData = MapItem.getSavedData(mapId, level);
      if (mapId.id() != -1 && worldData != null) {
         return worldData;
      }

      String name = stack.getHoverName().getString();
      return stack.has(DataComponents.CUSTOM_NAME) ? this.mapDataByName.getOrDefault(name, null) : null;
   }

   public void save(String address) {
      this.awaitPendingSave(address);
      byte[] snapshot = this.serializeMaps();
      if (snapshot != null) {
         writeSnapshot(address, snapshot);
      }
   }

   private boolean hasPendingSave(String address) {
      CompletableFuture<Void> pending = this.pendingSavesByAddress.get(address);
      return pending != null && !pending.isDone();
   }

   private void awaitPendingSave(String address) {
      CompletableFuture<Void> pending = this.pendingSavesByAddress.get(address);
      if (pending == null) {
         return;
      }
      try {
         pending.join();
      } catch (CompletionException saveFailure) {
         ClientAccess.logger.warn("Previous map cache save for {} did not complete cleanly", address, saveFailure);
      }
   }

   private void queueSave(String address, byte[] snapshot) {
      CompletableFuture<Void> previous = this.pendingSavesByAddress.get(address);
      if (previous == null) {
         previous = CompletableFuture.completedFuture(null);
      }

      CompletableFuture<Void> queued = previous
         .handle((ignored, failure) -> null)
         .thenRunAsync(() -> writeSnapshot(address, snapshot), ClientAccess.executorService)
         .exceptionally(failure -> {
            ClientAccess.logger.warn("Could not schedule map cache save for {}", address, failure);
            return null;
         });

      this.pendingSavesByAddress.put(address, queued);
      queued.whenComplete((ignored, failure) ->
         this.pendingSavesByAddress.remove(address, queued)
      );
   }

   private byte[] serializeMaps() {
      if (this.preserveUnreadableFile || this.mapDataByName.isEmpty()) {
         return null;
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream output = new DataOutputStream(bytes)) {
         output.writeInt(this.mapDataByName.size());
         for (var entry : this.mapDataByName.entrySet()) {
            output.writeUTF(entry.getKey());
            output.writeByte(entry.getValue().scale);
            output.write(entry.getValue().colors);
         }
         return bytes.toByteArray();
      } catch (IOException failure) {
         ClientAccess.logger.warn("Could not serialize map cache", failure);
         return null;
      }
   }

   private static void writeSnapshot(String address, byte[] snapshot) {
      try {
         Path mapsDirectory = ConfigPathGuard.ensureDirectory(
            ConfigurationManager.ROOT_DIRECTORY,
            ConfigurationManager.MAPS_DIRECTORY
         );
         Path target = mapsDirectory.resolve(FontRenderer.sanitizeFileName(address) + ".data");
         FileUtils.writeBytesAtomically(target, CompressionUtils.deflate(snapshot, 2));
      } catch (IOException | RuntimeException failure) {
         ClientAccess.logger.warn("Could not save map cache for {}", address, failure);
      }
   }

   public MapItemSavedData getMapData(ItemStack stack, int mapId) {
      MapId lookupId = new MapId(mapId);
      return this.getMapData(stack, lookupId);
   }

   private void cacheNamedMap(ItemStack stack) {
      if (stack.getItem() == Items.FILLED_MAP && stack.has(DataComponents.CUSTOM_NAME)) {
         String name = stack.getHoverName().getString();
         if (this.mapDataByName.size() >= MapCacheReader.MAX_MAPS && !this.mapDataByName.containsKey(name)) {
            return;
         }
         MapId mapId = (MapId)stack.getOrDefault(DataComponents.MAP_ID, new MapId(-1));
         this.mapDataByName.compute(name, (ignoredName, previousData) -> {
            ClientLevel level = ClientAccess.schizoMinecraft.level;
            return MapItem.getSavedData(mapId, level);
         });
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundMapItemDataPacket) {
         TaskScheduler scheduler = ClientContext.taskScheduler;
         Object owner = this.session;
         Runnable updateMaps = () -> {
            if (owner != this.session) return;
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            if (player == null || ClientAccess.schizoMinecraft.level == null) return;
            Inventory inventory = player.getInventory();
            for (Object item : inventory.getNonEquipmentItems()) {
               ItemStack stack = (ItemStack)item;

               this.cacheNamedMap(stack);
            }
         };
         scheduler.scheduleRotationTask(updateMaps, 1);
      }
   }
}
