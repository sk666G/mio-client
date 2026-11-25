/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.world.Waypoint;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.settings.JsonConfigurable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

public final class WaypointRegistry extends AbstractRegistry<Waypoint> implements ClientAccess, JsonConfigurable {
   public WaypointRegistry() {
      super(new ArrayList<>());
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   @Override
   public boolean register(Object entry) {
      Waypoint waypoint = (Waypoint)entry;
      return this.registerWaypoint(waypoint);
   }

   @Override
   public boolean unregister(Object entry) {
      Waypoint waypoint = (Waypoint)entry;
      return this.unregisterWaypoint(waypoint);
   }

   public List<Waypoint> getVisible() {
      if (this.isWorldUnavailable()) {
         return Collections.emptyList();
      }

      ClientPacketListener serverCheckConnection = ClientAccess.schizoMinecraft.player.connection;
      String serverAddress;
      if (serverCheckConnection.getServerData() == null) {
         serverAddress = "singleplayer";
      } else {
         ClientPacketListener addressConnection = ClientAccess.schizoMinecraft.player.connection;
         serverAddress = addressConnection.getServerData().ip;
      }

      String currentServer = serverAddress;
      return this.entries.stream()
         .filter(candidate -> {
            if (candidate.getServer() != null) {
               String waypointServer = candidate.getServer();
               if (currentServer.equalsIgnoreCase(waypointServer)) {
                  return true;
               }
            }
            return false;
         })
         .sorted(Comparator.comparing((Waypoint waypoint) -> {
            LocalPlayer playerSnapshot = ClientAccess.schizoMinecraft.player;
            double waypointX = waypoint.getX();
            double waypointY = waypoint.getY();
            double waypointZ = waypoint.getZ();
            return playerSnapshot.distanceToSqr(waypointX, waypointY, waypointZ);
         }))
         .toList();
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject configObject)
         || !(configObject.get("waypoints") instanceof JsonArray waypointArray)) {
         throw new IllegalArgumentException("Waypoint configuration must contain a waypoints array");
      }

      List<Waypoint> loaded = new ArrayList<>();
      for (JsonElement element : waypointArray) {
         try {
            Waypoint waypoint = ClientAccess.gson.fromJson(element, Waypoint.class);
            if (waypoint == null
               || waypoint.getName() == null
               || waypoint.getServer() == null
               || loaded.stream().anyMatch(existing -> hasSameIdentity(existing, waypoint))) {
               continue;
            }
            loaded.add(waypoint);
         } catch (RuntimeException invalidWaypoint) {
            ClientAccess.logger.warn("Skipping invalid waypoint entry", invalidWaypoint);
         }
      }

      this.entries.clear();
      this.entries.addAll(loaded);
   }

   @Override
   public JsonElement toJson() {
      JsonArray waypointArray = new JsonArray();
      JsonObject configObject = new JsonObject();
      Iterator<Waypoint> iterator = this.entries.iterator();
      while (iterator.hasNext()) {
         Waypoint waypoint = iterator.next();
         Gson gson = ClientAccess.gson;
         JsonElement waypointJson = gson.toJsonTree(waypoint);
         waypointArray.add(waypointJson);
      }
      configObject.add("waypoints", waypointArray);
      return configObject;
   }

   @Override
   public String getConfigName() {
      return "waypoints.json";
   }

   public boolean registerWaypoint(Waypoint waypoint) {
      if (waypoint == null) {
         return false;
      }

      for (Waypoint existingWaypoint : this.entries) {
         if (hasSameIdentity(existingWaypoint, waypoint)) {
            return false;
         }
      }
      return this.getRegistry().add(waypoint);
   }

   public boolean unregisterWaypoint(Waypoint waypoint) {
      return waypoint != null && this.entries.removeIf(candidate -> hasSameIdentity(candidate, waypoint));
   }

   public boolean renameWaypoint(Waypoint waypoint, String targetName) {
      if (waypoint == null || targetName == null || targetName.isBlank() || !this.entries.contains(waypoint)) {
         return false;
      }

      Waypoint replacement = new Waypoint(
         targetName,
         waypoint.getPosition(),
         waypoint.getDimension(),
         waypoint.getServer()
      );
      replacement.setToggled(waypoint.isToggled());

      for (Waypoint existing : this.entries) {
         if (existing != waypoint && hasSameIdentity(existing, replacement)) {
            return false;
         }
      }

      if (!this.entries.remove(waypoint)) {
         return false;
      }
      if (this.registerWaypoint(replacement)) {
         return true;
      }

      this.entries.add(waypoint);
      return false;
   }

   static boolean hasSameIdentity(Waypoint first, Waypoint second) {
      if (first == null || second == null) {
         return false;
      }

      String firstName = first.getName();
      String secondName = second.getName();
      String firstServer = first.getServer();
      String secondServer = second.getServer();
      return firstName != null
         && secondName != null
         && firstServer != null
         && secondServer != null
         && firstName.equalsIgnoreCase(secondName)
         && firstServer.equalsIgnoreCase(secondServer);
   }
}
