package dev.schizo.client.modules.combat;

final class AntiPhaseSwapPolicy {
   private AntiPhaseSwapPolicy() {
   }

   static boolean shouldCaptureOriginal(boolean alreadyPending) {
      return !alreadyPending;
   }
}
