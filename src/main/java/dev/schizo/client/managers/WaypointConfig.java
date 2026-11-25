package dev.schizo.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.schizo.client.utils.world.Waypoint;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

final class WaypointConfig {
   private WaypointConfig() {}

   static List<Waypoint> decode(JsonArray array, Gson gson) {
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
}
