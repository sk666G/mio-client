/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/** Keeps the module class lookup index consistent with successful registry mutations. */
final class ModuleClassIndex {
   private ModuleClassIndex() {
   }

   static <T> boolean cacheSuccessfulRegistration(Map<Class<?>, T> index, T value, boolean registered) {
      Objects.requireNonNull(index, "index");
      Objects.requireNonNull(value, "value");
      if (registered) {
         index.put(value.getClass(), value);
      }
      return registered;
   }

   static <T> boolean unregister(
      Collection<T> registeredEntries,
      Map<Class<?>, T> index,
      T value
   ) {
      Objects.requireNonNull(registeredEntries, "registeredEntries");
      Objects.requireNonNull(index, "index");
      Objects.requireNonNull(value, "value");
      if (!registeredEntries.remove(value)) {
         return false;
      }
      index.remove(value.getClass(), value);
      return true;
   }
}
