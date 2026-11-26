/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui;

/** Process-local accessibility policy shared by UI animation primitives. */
public final class UiMotionPolicy {
   private static volatile boolean reducedMotion;

   private UiMotionPolicy() {
   }

   public static boolean isReducedMotion() {
      return reducedMotion;
   }

   public static void setReducedMotion(boolean reduced) {
      reducedMotion = reduced;
   }
}
