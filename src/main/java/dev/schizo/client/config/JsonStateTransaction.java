/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import com.google.gson.JsonElement;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Failure-atomic application of one JSON-backed live state object. */
public final class JsonStateTransaction {
   private JsonStateTransaction() {
   }

   public static void apply(
      Supplier<JsonElement> snapshotSupplier,
      Consumer<JsonElement> applier,
      JsonElement desiredState
   ) {
      if (snapshotSupplier == null || applier == null || desiredState == null) {
         throw new IllegalArgumentException("JSON state transaction inputs must not be null");
      }

      JsonElement previousState = snapshotSupplier.get();
      if (previousState == null) {
         throw new IllegalStateException("JSON configurable did not provide a rollback snapshot");
      }
      previousState = previousState.deepCopy();

      try {
         applier.accept(desiredState.deepCopy());
      } catch (RuntimeException applyFailure) {
         try {
            applier.accept(previousState.deepCopy());
         } catch (RuntimeException rollbackFailure) {
            applyFailure.addSuppressed(rollbackFailure);
         }
         throw applyFailure;
      }
   }
}
