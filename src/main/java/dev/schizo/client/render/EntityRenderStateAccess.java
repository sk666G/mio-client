/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public interface EntityRenderStateAccess {
   Entity schizo$getEntity();

   void schizo$setEntity(Entity entity);

   static Entity getEntity(EntityRenderState renderState) {
      EntityRenderStateAccess access = (EntityRenderStateAccess)renderState;
      return access.schizo$getEntity();
   }

   static void setEntity(EntityRenderState renderState, Entity entity) {
      EntityRenderStateAccess access = (EntityRenderStateAccess)renderState;
      access.schizo$setEntity(entity);
   }
}
