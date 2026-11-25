package dev.schizo.client.utils;

/** Pure work-budget policy shared by incremental hot-path scanners. */
public final class IncrementalWorkBudget {
   private IncrementalWorkBudget() {
   }

   public static boolean canContinue(int processed, int maximumItems) {
      if (processed < 0 || maximumItems < 1) {
         throw new IllegalArgumentException("Invalid incremental work counters");
      }
      return processed < maximumItems;
   }

   public static boolean canContinue(
      int processed,
      long elapsedNanos,
      int maximumItems,
      long maximumNanos
   ) {
      if (!canContinue(processed, maximumItems)) {
         return false;
      }
      if (elapsedNanos < 0L || maximumNanos < 1L) {
         throw new IllegalArgumentException("Invalid incremental time budget");
      }
      // Match Schizo's existing scanner semantics: always allow the first item so
      // a slow clock sample cannot permanently starve an active scan.
      return processed == 0 || elapsedNanos < maximumNanos;
   }
}
