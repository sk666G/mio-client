package dev.schizo.client.compatibility;

/** Source-level behavior classification; it is not a guarantee of server acceptance. */
public enum CompatibilityClass {
   CLIENT_ONLY,
   VISUAL_ONLY,
   INPUT_MODIFYING,
   WORLD_INTERACTION,
   PACKET_AFFECTING,
   SERVER_SENSITIVE,
   UNVERIFIED
}
