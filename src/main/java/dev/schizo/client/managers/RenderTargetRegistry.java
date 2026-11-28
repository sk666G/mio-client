/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;

public final class RenderTargetRegistry implements ClientAccess {
   private static final ObjectArrayList<ScaledTarget> renderTargets = new ObjectArrayList<>();

   private RenderTargetRegistry() {
   }

   public static RenderTarget createRenderTarget(String name) {
      return createRenderTarget(name, 1);
   }

   public static RenderTarget createRenderTarget(String name, int scale) {
      Window window = ClientAccess.schizoMinecraft.getWindow();
      TextureTarget target = new TextureTarget(name, Math.max(1, window.getWidth() / scale), Math.max(1, window.getHeight() / scale), true);
      renderTargets.add(new ScaledTarget(target, scale));
      return target;
   }

   public static Supplier<RenderTarget> createLazyRenderTarget(String name) {
      return createLazyRenderTarget(name, 1);
   }

   public static Supplier<RenderTarget> createLazyRenderTarget(String name, int scale) {
      return new Supplier<>() {
         private volatile RenderTarget cachedTarget;

         @Override
         public RenderTarget get() {
            RenderTarget target = this.cachedTarget;
            if (target != null) {
               return target;
            }

            synchronized (this) {
               target = this.cachedTarget;
               if (target == null) {
                  // Assign only after successful creation so an early call can retry once the window exists.
                  target = createRenderTarget(name, scale);
                  this.cachedTarget = target;
               }
               return target;
            }
         }
      };
   }

   public static void resizeRenderTargets(int width, int height) {
      renderTargets.forEach(entry -> entry.target.resize(Math.max(1, width / entry.scale), Math.max(1, height / entry.scale)));
   }

   private record ScaledTarget(RenderTarget target, int scale) {}
}
