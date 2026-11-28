package dev.schizo.client.modules.combat;

final class PusherPowerPolicy {
   private PusherPowerPolicy() {
   }

   static boolean hasPowerSource(boolean redstoneBlockAvailable, boolean redstoneTorchAvailable) {
      return redstoneBlockAvailable || redstoneTorchAvailable;
   }
}
