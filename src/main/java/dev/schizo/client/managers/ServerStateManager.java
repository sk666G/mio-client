/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.world.WorldUtils;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.exploit.FastLatencyModule;
import dev.schizo.client.settings.options.DimensionType;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

public final class ServerStateManager implements ClientAccess {
   private static FastLatencyModule fastLatency;
   private static final Pattern LATENCY_PATTERN = Pattern.compile("\\d+ ping");
   private final ServerTickRateWindow tickRateWindow = new ServerTickRateWindow();
   private final ElapsedTimer setbackTimer;
   private final ElapsedTimer teleportAckTimer;
   private final ElapsedTimer packetFlowTimer;
   private final ElapsedTimer peakLatencyTimer;
   private volatile long lastPacketMillis;
   private volatile long lastEntityPacketMillis;
   private volatile long lastPacketNanos;
   private volatile long lastEntityPacketNanos;
   private volatile long lastTimeUpdateNanos;
   private volatile int teleportId;
   private volatile boolean serverSprinting;
   private volatile boolean serverSneaking;
   private volatile boolean inEnd;
   private volatile ServerData serverData;
   private volatile int footerLatencyMillis;
   private volatile int peakLatencyMillis;
   public boolean autoLogged;

   public boolean isServerSneaking() {
      return this.serverSneaking;
   }

   public int getTeleportId() {
      return this.teleportId;
   }

   public ElapsedTimer getTeleportAckTimer() {
      return this.teleportAckTimer;
   }

   public float getTickRate() {
      return this.tickRateWindow.average();
   }

   public long getLastPacketTime() {
      return this.getLastPacketTime(PacketActivity.ANY);
   }

   public boolean isServerSprinting() {
      return this.serverSprinting;
   }

   public int getPlayerLatencyMillis(Player player) {
      ClientPacketListener capturedConnection = ClientAccess.schizoMinecraft.player.connection;
      GameProfile profile = player.getGameProfile();
      UUID profileId = profile.id();
      PlayerInfo playerInfo = capturedConnection.getPlayerInfo(profileId);
      return playerInfo == null ? 0 : playerInfo.getLatency();
   }

   public float getLatestTickRate() {
      return this.tickRateWindow.latest();
   }

   public ElapsedTimer getSetbackTimer() {
      return this.setbackTimer;
   }

   public int getLatencyMillis() {
      if (ClientAccess.schizoMinecraft.player == null) {
         return 0;
      }
      FastLatencyModule fastLatencyModule = fastLatency;
      if (fastLatencyModule != null && fastLatencyModule.isToggled()) {
         return fastLatencyModule.getLatencyMillis();
      }
      LocalPlayer latencyPlayer = ClientAccess.schizoMinecraft.player;
      int playerLatency = this.getPlayerLatencyMillis(latencyPlayer);
      return playerLatency == 0 ? this.footerLatencyMillis : playerLatency;
   }

   public int getLatencyTicks() {
      return (int)Math.ceil(this.getLatencyMillis() / 50.0F);
   }

   public int getPeakLatencyMillis() {
      return this.peakLatencyMillis;
   }

   public ServerData getServerData() {
      return this.serverData;
   }

   public ServerStateManager() {
      this.setbackTimer = new ElapsedTimer();
      this.teleportAckTimer = new ElapsedTimer();
      this.packetFlowTimer = new ElapsedTimer();
      this.peakLatencyTimer = new ElapsedTimer();
      ClientAccess.eventBus.subscribe(this);
   }

   public boolean hasElapsedSinceSetback(long durationMillis) {
      ElapsedTimer timer = this.getSetbackTimer();
      return timer.hasElapsed(durationMillis);
   }

   public void setServerData(ServerData data) {
      this.serverData = data;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      long packetAgeNanos = this.lastPacketNanos == 0L
         ? Long.MAX_VALUE
         : System.nanoTime() - this.lastPacketNanos;
      if (packetAgeNanos > TimeUnit.MILLISECONDS.toNanos(500L)) {
         this.packetFlowTimer.reset();
      }

      int latencyMillis = this.getLatencyMillis();
      if (this.peakLatencyTimer.hasElapsed(10.0D, TimeUnit.SECONDS) || latencyMillis > this.peakLatencyMillis) {
         this.peakLatencyMillis = latencyMillis;
         this.peakLatencyTimer.reset();
      }

      DimensionType dimension = WorldUtils.getDimension();
      this.inEnd = dimension.isEnd();
   }

   public long getLastPacketTime(ServerStateManager.PacketActivity activity) {
      return switch (activity) {
         case ANY -> this.lastPacketMillis;
         case ENTITY -> this.lastEntityPacketMillis;
      };
   }

   public long getPacketIdleMillis(ServerStateManager.PacketActivity activity) {
      long packetNanos = switch (activity) {
         case ANY -> this.lastPacketNanos;
         case ENTITY -> this.lastEntityPacketNanos;
      };
      if (packetNanos == 0L) {
         return Long.MAX_VALUE;
      }
      long elapsedNanos = Math.max(0L, System.nanoTime() - packetNanos);
      return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
   }

   @EventHandler(priority = 200)
   private void onConnectionSendPacketEvent(
      PacketEvent.Send event
   ) {
      Input extractedInput;
      if (event.getPacket() instanceof ServerboundPlayerCommandPacket commandPacket) {
         if (commandPacket.getAction() == Action.START_SPRINTING) {
            if (this.serverSprinting) {
               event.cancel();
            }

            this.serverSprinting = true;
         }

         if (commandPacket.getAction() == Action.STOP_SPRINTING) {
            if (!this.serverSprinting) {
               event.cancel();
            }

            this.serverSprinting = false;
         }
      }

      if (event.getPacket() instanceof ServerboundPlayerInputPacket inputPacket) {
         extractedInput = inputPacket.input();
         this.serverSneaking = extractedInput.shift();
      }

      if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
         this.teleportAckTimer.reset();
      }
   }

   public boolean hasStablePacketFlowFor(long durationMillis) {
      return this.packetFlowTimer.hasElapsed(durationMillis);
   }

   private void resetSessionState() {
      this.tickRateWindow.clear();
      this.lastPacketMillis = 0L;
      this.lastEntityPacketMillis = 0L;
      this.lastPacketNanos = 0L;
      this.lastEntityPacketNanos = 0L;
      this.lastTimeUpdateNanos = 0L;
      this.teleportId = 0;
      this.serverSprinting = false;
      this.serverSneaking = false;
      this.inEnd = false;
      this.footerLatencyMillis = 0;
      this.peakLatencyMillis = 0;
      this.autoLogged = false;
      this.setbackTimer.clear();
      this.teleportAckTimer.clear();
      this.packetFlowTimer.clear();
      this.peakLatencyTimer.clear();
   }

   @EventHandler
   private void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      this.resetSessionState();
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.resetSessionState();
      this.serverData = null;
   }

   private void recordReceivedPacket(Packet packet) {
      long nowMillis = System.currentTimeMillis();
      long nowNanos = System.nanoTime();
      if (packet instanceof ClientboundMoveEntityPacket
         || packet instanceof ClientboundRotateHeadPacket
         || this.inEnd) {
         this.lastEntityPacketMillis = nowMillis;
         this.lastEntityPacketNanos = nowNanos;
      }

      this.lastPacketMillis = nowMillis;
      this.lastPacketNanos = nowNanos;
   }

   public float getTickRateScale() {
      return this.tickRateWindow.average() / 20.0F;
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      Packet receivedPacket = event.getPacket();
      this.recordReceivedPacket(receivedPacket);
      if (event.getPacket() instanceof ClientboundPlayerPositionPacket positionPacket) {
         this.teleportId = positionPacket.id();
         ElapsedTimer ackTimer = this.teleportAckTimer;
         if (ackTimer.hasElapsed(50L)) {
            this.setbackTimer.reset();
         }

         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            LocalPlayer sprintPlayer = ClientAccess.schizoMinecraft.player;
            if (sprintPlayer != null) {
               this.serverSprinting = sprintPlayer.isSprinting();
            }
         });
      }

      if (event.getPacket() instanceof ClientboundSetTimePacket) {
         long nowNanos = System.nanoTime();
         if (this.lastTimeUpdateNanos != 0L) {
            this.tickRateWindow.recordIntervalNanos(nowNanos - this.lastTimeUpdateNanos);
         }
         this.lastTimeUpdateNanos = nowNanos;
      }

      if (event.getPacket() instanceof ClientboundBundlePacket bundlePacket) {
         for (Packet<?> subpacket : bundlePacket.subPackets()) {
            EventBus eventBus = ClientAccess.eventBus;
            PacketEvent.Receive subpacketEvent = new PacketEvent.Receive(subpacket);
            eventBus.post(subpacketEvent);
         }
      }

      if (event.getPacket() instanceof ClientboundTabListPacket tabListPacket) {
         if (this.serverData == null) {
            return;
         }

         Component footerComponent = tabListPacket.footer();
         String footer = footerComponent.getString();
         Matcher latencyMatcher = LATENCY_PATTERN.matcher(footer);
         if (latencyMatcher.find()) {
            String latencyText = latencyMatcher.group();

            try {
               int textLength = latencyText.length();
               int suffixEnd = textLength - 5;
               latencyText = latencyText.substring(0, suffixEnd).trim();
               this.footerLatencyMillis = Integer.parseInt(latencyText);
            } catch (NumberFormatException | IndexOutOfBoundsException parseFailure) {
               this.footerLatencyMillis = 0;
            }
         }
      }
   }

   public static void bindModules(ModuleRegistry registry) {
      fastLatency = (FastLatencyModule)java.util.Objects.requireNonNull(
         registry.getModule(FastLatencyModule.class),
         "Required module is not registered: FastLatencyModule"
      );
   }

   public enum PacketActivity {
      ANY,
      ENTITY;
   }
}
