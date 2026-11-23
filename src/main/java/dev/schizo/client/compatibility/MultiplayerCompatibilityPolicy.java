package dev.schizo.client.compatibility;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.modules.Module;
import net.minecraft.network.chat.Component;

/** Reversible, user-enabled guard for modules with unreviewed or server-facing behavior. */
public final class MultiplayerCompatibilityPolicy {
   private static volatile boolean conservativeEnabled;

   private MultiplayerCompatibilityPolicy() {
   }

   public static boolean isConservativeEnabled() {
      return conservativeEnabled;
   }

   public static void setConservativeEnabled(boolean enabled) {
      conservativeEnabled = enabled;
   }

   public static boolean canEnable(Module module) {
      if (!conservativeEnabled || ClientAccess.schizoMinecraft == null
         || ClientAccess.schizoMinecraft.level == null
         || ClientAccess.schizoMinecraft.hasSingleplayerServer()) {
         return true;
      }

      return isActivationAllowed(
         conservativeEnabled,
         true,
         module.getCompatibilityAssessment().classification()
      );
   }

   static boolean isActivationAllowed(
      boolean conservativeProfileEnabled, boolean multiplayer, CompatibilityClass classification
   ) {
      if (!conservativeProfileEnabled || !multiplayer) {
         return true;
      }
      return classification != CompatibilityClass.PACKET_AFFECTING
         && classification != CompatibilityClass.SERVER_SENSITIVE
         && classification != CompatibilityClass.UNVERIFIED;
   }

   public static void reportBlocked(Module module) {
      String explanation = module.getCompatibilityAssessment().summary();
      String message = "Conservative multiplayer profile blocked " + module.getName() + ": " + explanation;
      ClientAccess.logger.warn(message);
      if (ClientAccess.schizoMinecraft != null && ClientAccess.schizoMinecraft.gui != null) {
         ClientAccess.schizoMinecraft.gui.getChat().addMessage(Component.literal(message));
      }
   }
}
