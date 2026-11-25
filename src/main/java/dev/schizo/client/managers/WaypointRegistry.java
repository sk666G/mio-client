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
import java.util.TreeMap;
import java.util.TreeSet;
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
      if (!json.isJsonObject()) {
         return;
      }
      JsonObject configObject = json.getAsJsonObject();
      if (!configObject.has("waypoints")) {
         return;
      }
      List<Waypoint> loaded = decodeWaypoints(configObject.getAsJsonArray("waypoints"), ClientAccess.gson);
      this.entries.clear();
      this.entries.addAll(loaded);
   }

   static List<Waypoint> decodeWaypoints(JsonArray array, Gson gson) {
      List<Waypoint> loaded = new ArrayList<>();
      var namesByServer = new TreeMap<String, TreeSet<String>>(String.CASE_INSENSITIVE_ORDER);
      for (JsonElement element : array) {
         Waypoint waypoint = gson.fromJson(element, Waypoint.class);
         if (waypoint == null || waypoint.getName() == null || waypoint.getServer() == null
            || waypoint.getDimension() == null || !Double.isFinite(waypoint.getX())
            || !Double.isFinite(waypoint.getY()) || !Double.isFinite(waypoint.getZ())) {
            throw new IllegalArgumentException("Invalid waypoint in waypoints.json");
         }
         if (namesByServer.computeIfAbsent(waypoint.getServer(), ignored -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
               .add(waypoint.getName())) {
            loaded.add(waypoint);
         }
      }
      return loaded;
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
      if (waypoint == null || this.entries.stream().anyMatch(candidate -> sameIdentity(candidate, waypoint))) {
         return false;
      }
      return this.entries.add(waypoint);
   }

   public boolean unregisterWaypoint(Waypoint waypoint) {
      return waypoint != null && this.entries.removeIf(candidate -> sameIdentity(candidate, waypoint));
   }

   static boolean sameIdentity(Waypoint left, Waypoint right) {
      return left.getName().equalsIgnoreCase(right.getName()) && left.getServer().equalsIgnoreCase(right.getServer());
   }
}
