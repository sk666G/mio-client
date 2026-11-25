package dev.schizo.client.utils;

/** Published by client settings; notification workers may only read the flag. */
public final class CoordinatePrivacy {
   private static volatile boolean enabled;
   private CoordinatePrivacy() {}
   public static boolean isEnabled() { return enabled; }
   public static void setEnabled(boolean value) { enabled = value; }
}
