/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.gui.GuiElement;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.settings.options.HudAnchor;
import dev.schizo.client.profiling.EventProfiler;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public class HudAnchorGroup implements ClientAccess, GuiElement {

   private final List widgets = new ArrayList();
   private final HudAnchor anchor;

   private static HUDModule hud() {
      HUDModule hud = HUDModule.INSTANCE;
      if (hud == null) {
         throw new IllegalStateException("HUD module is not initialized");
      }
      return hud;
   }

   public List getWidgets() {
      return this.widgets;
   }

   public HudAnchorGroup(HudAnchor anchor) {
      this.anchor = anchor;
   }

   private void prepareWidgetRender(float enabledProgress) {
      if (!HudWidgetRenderer.renderingHud
         && enabledProgress
            != 1.0F) {
         GuiDrawUtils.flush();
      }
   }

   private float getBottomMargin(boolean rightAligned) {
      float sideOffset = rightAligned
         ? hud().getBottomHudOffset()
         : hud().getChatInputOffset();
      return Mth.clamp(
         hud().getSafeY()
            + sideOffset,
         0.0F,
         16.0F
      );
   }

   private float calculateY(
      float height, HudHorizontalAlignment horizontalAlignment, HudVerticalAlignment verticalAlignment
   ) {
      return switch (verticalAlignment) {
         case TOP -> hud().getSafeY()
            + (horizontalAlignment == HudHorizontalAlignment.RIGHT ? hud().getEffectIconOffset() : 0.0F);
         case BOTTOM -> ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight()
            - height - this.getBottomMargin(horizontalAlignment == HudHorizontalAlignment.RIGHT);
         case CENTER -> (ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight() - height) / 2.0F;
         default -> throw new IllegalStateException("Unexpected value: " + verticalAlignment);
      };
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      poseStack.pushPose();
      try {
         if (this.anchor != HudAnchor.NONE) {
            this.applyHudTranslation(poseStack);
         }

         for (int widgetIndex = 0; widgetIndex < this.widgets.size(); widgetIndex++) {
            HudWidgetRenderer widget = (HudWidgetRenderer)this.widgets.get(widgetIndex);
            widget.updateEnabledAnimation();
            float enabledProgress = widget.getEnabledProgress();
            boolean visible = widget.isEnabled() || enabledProgress > 0.0F;
            float[] size;
            if (!visible) {
               size = widget.useHiddenDimensions();
            } else {
               size = widget.calculateDimensions();
               widget.setDimensions(size);
            }
            if (this.anchor != HudAnchor.NONE) {
               this.layoutAnchoredWidget(widget, size, widgetIndex);
            } else {
               this.layoutFreeWidget(widget, size);
            }

            this.prepareWidgetRender(enabledProgress);
            if (visible) {
               RenderUtils.setRenderColor(1.0F, 1.0F, 1.0F, enabledProgress);
               try {
                  EventProfiler profiler = EventProfiler.GLOBAL;
                  if (HudWidgetRenderer.renderingHud && profiler.isEnabled()) {
                     long startedAt = profiler.nanoTime();
                     try {
                        widget.render(poseStack, mouseX, mouseY);
                     } finally {
                        profiler.recordHudRender(widget.getProfilerHandle(), profiler.nanoTime() - startedAt);
                     }
                  } else {
                     widget.render(poseStack, mouseX, mouseY);
                  }
               } finally {
                  try {
                     this.prepareWidgetRender(enabledProgress);
                     if (!HudWidgetRenderer.renderingHud) {
                        RenderUtils.getEntityBuffers().endBatch(ClientRenderTypes.guiTriangles());
                     }
                  } finally {
                     RenderUtils.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  }
               }
            }

            if (widgetIndex < this.widgets.size() - 1
               && this.anchor != HudAnchor.NONE
               && this.anchor.getVerticalAlignment() != HudVerticalAlignment.BOTTOM) {
               ((HudWidgetRenderer)this.widgets.get(widgetIndex + 1))
                  .setY(widget.getY() + size[1], true);
            }
         }
      } finally {
         poseStack.popPose();
      }
   }

   private void layoutFreeWidget(HudWidgetRenderer widget, float[] size) {
      HudHorizontalAlignment horizontalAlignment = widget.getHorizontalAlignment();
      HudVerticalAlignment verticalAlignment = widget.getVerticalAlignment();
      if (horizontalAlignment != HudHorizontalAlignment.FREE) {
         widget.setX(
            this.calculateX(size[0], horizontalAlignment), true
         );
      }

      if (verticalAlignment != HudVerticalAlignment.FREE) {
         widget.setY(
            this.calculateY(size[1], horizontalAlignment, verticalAlignment), true
         );
      }
   }

   private void layoutAnchoredWidget(
      HudWidgetRenderer widget, float[] size, int widgetIndex
   ) {
      widget.setX(
         this.calculateX(
            size[0],
            this.anchor
               .getHorizontalAlignment()
         ),
         true
      );
      if (widgetIndex == 0) {
         widget.setY(
            this.calculateY(
               size[1],
               this.anchor
                  .getHorizontalAlignment(),
               this.anchor
                  .getVerticalAlignment()
            ),
            true
         );
      } else if (this.anchor == HudAnchor.BOTTOM_LEFT
         || this.anchor == HudAnchor.BOTTOM_RIGHT) {
         widget.setY(
            ((HudWidgetRenderer)this.widgets.get(widgetIndex - 1))
                  .getY()
               - size[1],
            true
         );
      }
   }

   private float calculateX(float width, HudHorizontalAlignment horizontalAlignment) {
      return switch (horizontalAlignment) {
         case LEFT -> hud().getSafeX();
         case RIGHT -> ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth() - width - hud().getSafeX();
         case CENTER -> ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth() / 2.0F - width / 2.0F;
         default -> throw new IllegalStateException("Unexpected value: " + horizontalAlignment);
      };
   }

   private void applyHudTranslation(PoseStack poseStack) {
      poseStack.translate(
         hud().getParallaxOffset()
      );
   }

   public HudAnchor getAnchor() {
      return this.anchor;
   }

}
