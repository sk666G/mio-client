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
         .filter(candidate -> isValidWaypoint(candidate)
            && currentServer.equalsIgnoreCase(candidate.getServer()))
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
            if (!isValidWaypoint(waypoint)
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
         if (!isValidWaypoint(waypoint)) {
            continue;
         }
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
      if (!isValidWaypoint(waypoint)) {
         return false;
      }

      for (Waypoint existingWaypoint : this.entries) {
         if (hasSameIdentity(existingWaypoint, waypoint)) {
            return false;
         }
      }
      return this.getRegistry().add(waypoint);
   }

   /** Discovery descriptions are labels, so identical labels must not discard distinct locations. */
   public Waypoint registerDiscoveryWaypoint(Waypoint requested) {
      if (requested == null || requested.getName() == null || requested.getName().isBlank()
         || requested.getServer() == null || requested.getDimension() == null
         || !Double.isFinite(requested.getX()) || !Double.isFinite(requested.getY())
         || !Double.isFinite(requested.getZ())) return null;
      java.util.Set<String> names = new java.util.HashSet<>();
      for (Waypoint existing : this.entries) {
         if (existing == null || existing.getServer() == null
            || !requested.getServer().equalsIgnoreCase(existing.getServer())) continue;
         if (existing.getName() != null) names.add(existing.getName().toLowerCase(java.util.Locale.ROOT));
         if (existing.getDimension() != null && dev.schizo.client.utils.world.DimensionCoordinates.canonical(requested.getDimension())
               .equals(dev.schizo.client.utils.world.DimensionCoordinates.canonical(existing.getDimension()))
            && requested.getX() == existing.getX() && requested.getY() == existing.getY()
            && requested.getZ() == existing.getZ()) return existing;
      }
      String name = requested.getName();
      for (int suffix = 2; names.contains(name.toLowerCase(java.util.Locale.ROOT)); suffix++) {
         name = requested.getName() + " (" + suffix + ")";
      }
      Waypoint result = new Waypoint(name, requested.getX(), requested.getY(), requested.getZ(),
         requested.getDimension(), requested.getServer());
      return this.registerWaypoint(result) ? result : null;
   }

   public boolean unregisterWaypoint(Waypoint waypoint) {
      return waypoint != null
         && this.entries.removeIf(candidate -> candidate == waypoint || hasSameIdentity(candidate, waypoint));
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

   static boolean isValidWaypoint(Waypoint waypoint) {
      return waypoint != null
         && waypoint.getName() != null
         && waypoint.getServer() != null
         && waypoint.getDimension() != null
         && Double.isFinite(waypoint.getX())
         && Double.isFinite(waypoint.getY())
         && Double.isFinite(waypoint.getZ());
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
