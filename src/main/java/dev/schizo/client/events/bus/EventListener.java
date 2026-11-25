/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.events.bus;

public interface EventListener {
   default dev.schizo.client.profiling.EventProfiler.CallbackHandle profileHandle() {
      return null;
   }

   default String diagnosticName() {
      return this.getClass().getSimpleName();
   }

   boolean isStatic();

   Class<?> getEventType();

   int getPriority();

   void call(Object event);
}
