package dev.schizo.client.modules;

/** Identity/generation check for work queued outside the owned task scheduler. */
public final class DeferredModuleActionGuard {
   private DeferredModuleActionGuard() {
   }

   public static boolean isCurrent(
      boolean enabled,
      long expectedGeneration,
      long currentGeneration,
      Object expectedLevel,
      Object currentLevel,
      Object expectedPlayer,
      Object currentPlayer
   ) {
      return enabled
         && expectedGeneration == currentGeneration
         && expectedLevel != null
         && expectedPlayer != null
         && expectedLevel == currentLevel
         && expectedPlayer == currentPlayer;
   }
}
