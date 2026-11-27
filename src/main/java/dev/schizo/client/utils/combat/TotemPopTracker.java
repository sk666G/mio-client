/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import dev.schizo.client.utils.entity.EntityUtils;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.PlayerTotemEvent;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.mixin.ClientboundEntityEventPacketAccessor;

import com.mojang.authlib.GameProfile;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.player.Player;

public final class TotemPopTracker implements ClientAccess {
   private final Map<String, Integer> popCounts = new ConcurrentHashMap<>();
   private final Map<String, Long> disconnectTimes = new ConcurrentHashMap<>();
   private final Map<UUID, String> profileNames = new ConcurrentHashMap<>();
   private volatile long sessionGeneration;

   public TotemPopTracker() {
      ClientAccess.eventBus.subscribe(this);
   }

   public int getPopCount(Player player) {
      GameProfile profile = player.getGameProfile();
      return this.getPopCount(profile.name());
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      Iterator players = ClientAccess.schizoMinecraft.level.players().iterator();

      while (players.hasNext()) {
         Player player = (Player)players.next();
         if (player != null) {
            if (!(EntityUtils.getHealthWithAbsorption(player) > 0.0F)) {
               Component playerName = player.getName();
               String name = playerName.getString();
               if (this.popCounts.containsKey(name)) {
                  int count = (Integer)this.popCounts.get(name);
                  this.popCounts.remove(name);
                  EventBus eventBus = ClientAccess.eventBus;
                  PlayerTotemEvent deathEvent = new PlayerTotemEvent(player, count, PlayerTotemEvent.Kind.DEATH);
                  eventBus.post(deathEvent);
               }
            }
         }
      }

      long nowMillis = System.currentTimeMillis();
      this.disconnectTimes.entrySet().removeIf(entry -> {
         if (entry.getValue() + 120000L >= nowMillis) {
            return false;
         }
         this.popCounts.remove(entry.getKey());
         return true;
      });
   }

   @EventHandler
   private void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      this.resetSession();
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.resetSession();
   }

   private void resetSession() {
      this.sessionGeneration++;
      this.popCounts.clear();
      this.disconnectTimes.clear();
      this.profileNames.clear();
   }

   public int getPopCount(String name) {
      return (Integer)this.popCounts.getOrDefault(name, Integer.valueOf(0));
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (event.getPacket() instanceof ClientboundEntityEventPacket entityEventPacket
         && entityEventPacket.getEventId() == 35) {
         int entityId = ((ClientboundEntityEventPacketAccessor)entityEventPacket).getId();
         long expectedGeneration = this.sessionGeneration;
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (this.sessionGeneration != expectedGeneration
               || ClientAccess.schizoMinecraft.level == null) {
               return;
            }

            if (ClientAccess.schizoMinecraft.level.getEntity(entityId) instanceof Player player) {
               String playerName = player.getGameProfile().name();
               int count = this.popCounts.merge(playerName, 1, Integer::sum);
               EventBus eventBus = ClientAccess.eventBus;
               eventBus.post(new PlayerTotemEvent(
                  player, count, PlayerTotemEvent.Kind.TOTEM_POP
               ));
            }
         });
      }

      if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket) {
         for (ClientboundPlayerInfoUpdatePacket.Entry joinedEntry : playerInfoUpdatePacket.newEntries()) {
            GameProfile profile = joinedEntry.profile();
            if (profile == null) {
               continue;
            }
            this.profileNames.put(profile.id(), profile.name());
            this.disconnectTimes.remove(profile.name());
         }
      }

      if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket playerInfoRemovePacket) {
         long disconnectedAt = System.currentTimeMillis();
         for (UUID removedId : playerInfoRemovePacket.profileIds()) {
            String removedName = this.profileNames.remove(removedId);
            if (removedName != null) {
               this.disconnectTimes.put(removedName, disconnectedAt);
            }
         }
      }
   }

}
