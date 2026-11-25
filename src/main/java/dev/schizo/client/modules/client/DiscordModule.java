/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.integrations.IntegrationHealth;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.events.MinecraftTickEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.StatusDisplayType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;

public class DiscordModule extends Module {
   private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
   private static final String ICON_URL = "https://cdn.discordapp.com/app-icons/1055222411819241572/22387af3b091639d90c2335d52cf11b7.png";
   private static final long CONNECTION_TIMEOUT_MS = 12_000L;
   private static final ScheduledExecutorService CONNECTION_TIMEOUTS = Executors.newSingleThreadScheduledExecutor(task -> {
      Thread thread = new Thread(task, "Schizo-Discord-ConnectionTimeout");
      thread.setDaemon(true);
      return thread;
   });
   private final Setting<Boolean> serverSetting;
   private volatile CompletableFuture<IPCClient> connectionFuture;
   private volatile IPCClient activeClient;
   private long startTimestamp;
   private final ElapsedTimer updateTimer;

   public DiscordModule() {
      super("Discord", "Displays Schizo as your Discord activity.", ModuleCategory.CLIENT, new String[0]);
      this.serverSetting = this.add(new BooleanSetting("Server", false));
      this.startTimestamp = System.currentTimeMillis();
      this.updateTimer = new ElapsedTimer();

      this.setDrawn(false);
   }

   @Override
   public void onEnable() {
      this.closeClient();
      IPCClient client = new IPCClient(1055222411819241572L);
      this.activeClient = client;

      CompletableFuture<IPCClient> pending = CompletableFuture.supplyAsync(() -> {
         try {
            client.connect(new DiscordBuild[0]);
            return client;
         } catch (Exception failure) {
            client.cancelConnect();
            throw new java.util.concurrent.CompletionException(failure);
         }
      }, ClientAccess.executorService);
      this.connectionFuture = pending;
      pending.whenComplete((connectedClient, failure) -> {
         if (failure != null) {
            LOGGER.warn("Discord connection failed: {}", failure.getClass().getSimpleName());
            Minecraft minecraft = ClientAccess.schizoMinecraft;
            if (minecraft != null) {
               minecraft.execute(() -> {
                  if (this.connectionFuture == pending && this.isToggled()) this.toggle();
               });
            }
         }
      });

      CONNECTION_TIMEOUTS.schedule(() -> {
         if (this.connectionFuture != pending || pending.isDone() || !this.isToggled()) return;
         LOGGER.warn("Discord IPC connection timed out after {} ms; aborting the local transport", CONNECTION_TIMEOUT_MS);
         client.cancelConnect();
         Minecraft minecraft = ClientAccess.schizoMinecraft;
         if (minecraft != null) {
            minecraft.execute(() -> {
               if (this.connectionFuture == pending && this.isToggled()) this.toggle();
            });
         }
      }, CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
   }

   @Override
   public void onDisable() {
      this.closeClient();
   }

   @EventHandler
   private void onTick(MinecraftTickEvent event) {
      try {
         CompletableFuture<IPCClient> pendingConnection = this.connectionFuture;
         if (pendingConnection == null || !pendingConnection.isDone()) {
            return;
         }
         ElapsedTimer timerSnapshot = this.updateTimer;
         if (!timerSnapshot.hasElapsed(2500L)) {
            return;
         }
         timerSnapshot = this.updateTimer;
         timerSnapshot.reset();
         IPCClient ipcClient = this.connectionFuture.getNow(null);
         if (ipcClient == null) {
            return;
         }
         RichPresence.Builder presenceBuilder = new RichPresence.Builder();
         presenceBuilder.setLargeImage(ICON_URL, "Schizo v" + SchizoClient.VERSION, ICON_URL);

         long timestamp = this.startTimestamp;
         presenceBuilder.setStartTimestamp(timestamp);
         ActivityType activityType = ActivityType.Playing;
         presenceBuilder.setActivityType(activityType);
         StatusDisplayType displayType = StatusDisplayType.Name;
         presenceBuilder.setStatusDisplayType(displayType);
         Setting<Boolean> serverVisibilitySetting = this.serverSetting;
         Boolean showServer = serverVisibilitySetting.getValue();
         if (showServer) {
            String serverDetails = "Not playing";
            if (!this.isWorldUnavailable()) {
               ClientPacketListener connection = ClientAccess.schizoMinecraft.player.connection;
               ServerData serverData = connection.getServerData();
               Minecraft minecraft = ClientAccess.schizoMinecraft;
               String serverLocation;
               if (!minecraft.isLocalServer() && serverData != null) {
                  String serverAddress = serverData.ip;
                  serverLocation = "on " + serverAddress;
               } else {
                  serverLocation = "singleplayer";
               }
               serverDetails = "Playing " + serverLocation;
            }
            presenceBuilder.setDetails(serverDetails);
         }
         RichPresence presence = presenceBuilder.build();
         ipcClient.sendRichPresence(presence);
      } catch (Exception updateFailure) {
         LOGGER.warn("Could not update Discord activity", updateFailure);
      }
   }

   public IntegrationHealth getConnectionHealth() {
      if (!this.isToggled()) {
         return IntegrationHealth.DISABLED;
      }
      CompletableFuture<IPCClient> pending = this.connectionFuture;
      if (pending == null) {
         return IntegrationHealth.UNAVAILABLE;
      }
      if (!pending.isDone()) {
         return IntegrationHealth.CONNECTING;
      }
      if (pending.isCompletedExceptionally() || pending.isCancelled()) {
         return IntegrationHealth.FAILED;
      }
      return pending.getNow(null) == null ? IntegrationHealth.UNAVAILABLE : IntegrationHealth.CONNECTED;
   }

   /** Returns the local IPC lifecycle state without waiting for the connection future. */
   public String getConnectionStatus() {
      return this.getConnectionHealth().label();
   }

   public void closeClient() {
      CompletableFuture<IPCClient> pending = this.connectionFuture;
      IPCClient client = this.activeClient;
      this.connectionFuture = null;
      this.activeClient = null;
      if (client == null) return;

      if (pending == null || !pending.isDone() || pending.isCompletedExceptionally() || pending.isCancelled()) {
         client.cancelConnect();
         return;
      }

      try {
         client.sendRichPresence(null);
      } catch (Exception failure) {
         LOGGER.debug("Could not clear Discord activity", failure);
      } finally {
         try {
            client.close();
         } catch (Exception failure) {
            LOGGER.debug("Could not close Discord connection", failure);
            client.cancelConnect();
         }
      }
   }

}
