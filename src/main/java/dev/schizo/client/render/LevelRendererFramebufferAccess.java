/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface LevelRendererFramebufferAccess {
   void schizo$popFramebuffer();

   void schizo$pushFramebuffer(RenderTarget target);
}
