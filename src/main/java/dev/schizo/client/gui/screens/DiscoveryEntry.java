package dev.schizo.client.gui.screens;

import java.util.Locale;

/** Immutable UI snapshot. A stash has no observed altitude, so y remains null. */
public record DiscoveryEntry(String kind, String name, String server, String dimension,
                             double x, Double y, double z, long firstSeen, long lastSeen) {
   public boolean matches(String query, String currentServer, boolean localOnly) {
      if (localOnly && !this.server.equalsIgnoreCase(currentServer)) return false;
      String text = this.kind + " " + this.name + " " + this.server + " " + this.dimension;
      return text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
   }

   public String coordinates() {
      return String.format(Locale.ROOT, "%.1f, %s, %.1f", this.x, this.y == null ? "?" : this.y.toString(), this.z);
   }

   public static String canonicalDimension(String dimension) {
      return dev.schizo.client.utils.world.DimensionCoordinates.canonical(dimension);
   }
}
