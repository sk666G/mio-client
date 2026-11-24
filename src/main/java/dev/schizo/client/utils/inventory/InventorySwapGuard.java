/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import java.util.Objects;

/** Runs an inventory action between a successful temporary swap and its restoration. */
final class InventorySwapGuard {
   private InventorySwapGuard() {
   }

   static void run(Runnable swapIn, Runnable action, Runnable swapBack) {
      Objects.requireNonNull(swapIn, "swapIn");
      Objects.requireNonNull(action, "action");
      Objects.requireNonNull(swapBack, "swapBack");

      // A failed swap-in may not have changed inventory state, so only restore
      // after the swap-in operation returns successfully.
      swapIn.run();
      try {
         action.run();
      } catch (RuntimeException | Error actionFailure) {
         try {
            swapBack.run();
         } catch (RuntimeException | Error restoreFailure) {
            if (restoreFailure != actionFailure) {
               actionFailure.addSuppressed(restoreFailure);
            }
         }
         throw actionFailure;
      }

      swapBack.run();
   }
}
