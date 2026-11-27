/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.utils.world.DimensionCoordinates;

import com.google.gson.annotations.SerializedName;

public class StashEntry implements NamedEntry {
   @SerializedName("id")
   private final String id;
   @SerializedName("x")
   private final int x;
   @SerializedName("z")
   private final int z;
   @SerializedName("dimension")
   private final String dimension;
   @SerializedName("server")
   private final String server;
   private long firstSeenMillis;
   private long lastSeenMillis;

   public StashEntry(String id, int x, int z, String dimension, String server) {
      this.firstSeenMillis = this.lastSeenMillis = System.currentTimeMillis();
      this.id = id;
      this.x = x;
      this.z = z;
      this.dimension = dimension;
      this.server = server;
   }

   public void observeAgain() {
      long now = System.currentTimeMillis();
      if (this.firstSeenMillis == 0) this.firstSeenMillis = now;
      this.lastSeenMillis = now;
   }

   public long firstSeenMillis() { return this.firstSeenMillis; }
   public long lastSeenMillis() { return this.lastSeenMillis; }

   public String formatCoordinates() {
      int xCoordinate = this.x;
      int zCoordinate = this.z;
      return xCoordinate + ", " + zCoordinate + ".";
   }

   public double getX() {
      return this.x;
   }

   public double getZ() {
      return this.z;
   }

   public String getDimension() {
      return this.dimension;
   }

   public String getServer() {
      return this.server;
   }

   public boolean hasSameLocation(StashEntry other) {
      return other != null
         && this.x == other.x
         && this.z == other.z
         && hasSameDimension(this.dimension, other.dimension)
         && java.util.Objects.equals(this.server, other.server);
   }

   static boolean hasSameDimension(String first, String second) {
      if (first == null || second == null) {
         return java.util.Objects.equals(first, second);
      }
      return DimensionCoordinates.canonical(first)
         .equals(DimensionCoordinates.canonical(second));
   }

   public boolean isPersistable() {
      return this.id != null && !this.id.isBlank()
         && this.dimension != null && !this.dimension.isBlank()
         && this.server != null && !this.server.isBlank();
   }

   @Override
   public String getName() {
      return this.id;
   }

}
