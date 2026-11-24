/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo context-profile failure state.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import java.util.Objects;

/** Remembers a context whose preset failed after rollback, preventing a per-tick retry loop. */
final class ContextProfileApplyFailure {
   private String failedContext;

   void markFailed(String contextKey) {
      this.failedContext = Objects.requireNonNull(contextKey, "contextKey");
   }

   boolean shouldSkip(String contextKey) {
      return this.failedContext != null && this.failedContext.equals(contextKey);
   }

   String getFailedContext() {
      return this.failedContext;
   }

   void clear() {
      this.failedContext = null;
   }
}
