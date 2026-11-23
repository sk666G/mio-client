/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import java.util.Collection;

public interface RegistryContainer {
   boolean register(Object entry);

   boolean unregister(Object entry);

   Collection getRegistry();
}
