/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.managers.RenderTargetRegistry;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

public final class MaskShaderRenderer implements ClientAccess {
   private static final Supplier<RenderTarget> MASK_TARGET = RenderTargetRegistry.createLazyRenderTarget("zio_blur");
   private static final Supplier<RenderTarget> HORIZONTAL_TARGET = RenderTargetRegistry.createLazyRenderTarget("zio_blur_horizontal");
   private static final Supplier<RenderTarget> VERTICAL_TARGET = RenderTargetRegistry.createLazyRenderTarget("zio_blur_vertical");
   private static float blurWidth;
   private static float bloom;
   private static float opacity;
   private static float modulate;

   public static RenderTarget maskTarget() {
      return MASK_TARGET.get();
   }

   public static RenderTarget horizontalTarget() {
      return HORIZONTAL_TARGET.get();
   }

   public static RenderTarget verticalTarget() {
      return VERTICAL_TARGET.get();
   }

   public static float getBlurWidth() {
      return blurWidth;
   }

   public static float getBloom() {
      return bloom;
   }

   public static float getModulate() {
      return modulate;
   }

   public static void setParameters(
      float width, float bloomStrength, float maskOpacity, float modulation
   ) {
      blurWidth = width;
      bloom = bloomStrength;
      opacity = maskOpacity;
      modulate = modulation;
   }

   public static float getOpacity() {
      return opacity;
   }

   public static void renderBlur(Runnable drawMask, float width) {
      blurWidth = width;
      captureMask(drawMask);
      renderEffect("blur_mask");
   }

   public static void captureMask(Runnable drawMask) {
      captureMask(drawMask, Color.WHITE);
   }

   public static void captureMask(Runnable drawMask, Color clearColor) {
      RenderSystem.assertOnRenderThread();
      RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
      if (drawMask != null) {
         RenderUtils.clearColor(
            maskTarget(), clearColor
         );
         RenderUtils.clearDepth(
            maskTarget()
         );
         RenderTargetUtils.setMainRenderTarget(
            maskTarget()
         );
         try {
            drawMask.run();
         } finally {
            // A failed mask draw must not leave Minecraft rendering into maskTarget().
            RenderTargetUtils.setMainRenderTarget(mainTarget);
         }
      }
   }

   public static void renderEffect(String effectId) {
      ClientContext.shaderRegistry
         .render(effectId, () -> {});
   }

}
