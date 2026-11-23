/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

public interface RenderState {
   default RenderState then(RenderState next) {
      return entering -> {
         this.apply(entering);
         next.apply(entering);
      };
   }

   static RenderState combine(RenderState... states) {
      return entering -> {
         for (RenderState state : states) {
            state.apply(entering);
         }
      };
   }

   void apply(boolean entering);
}
