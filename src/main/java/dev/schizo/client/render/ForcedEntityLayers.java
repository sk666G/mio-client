/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

public interface ForcedEntityLayers {
   void schizo$setForcedLayers(boolean forced);

   boolean schizo$areLayersForced();
}
