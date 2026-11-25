/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.diagnostics;

/** Value-only snapshot used by local health/status surfaces. */
public record ClientHealthSnapshot(
   boolean safeModeActive,
   boolean safeModeArmed,
   boolean safeModeInitializationFailed,
   boolean safeModeUnsafeMarker,
   int moduleConfigIssues,
   long eventFailures,
   int scheduledQueued,
   int scheduledOutstanding,
   boolean profilerEnabled
) {
   public boolean hasWarnings() {
      return this.safeModeInitializationFailed
         || this.safeModeUnsafeMarker
         || this.moduleConfigIssues > 0
         || this.eventFailures > 0
         || this.scheduledOutstanding > 0;
   }
}
