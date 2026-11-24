/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientContext;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.HudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.client.HUDModule;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.Mth;

public class HudTextListRenderer extends HudWidgetRenderer {

   private static final HUDModule hudSettings = ClientContext.getModule(HUDModule.class);
   private final List<? extends HudTextRow> rows;
   private final float[] dimensionBuffer = new float[2];

   public HudTextListRenderer(HudWidgetModule widget, HudTextRow row) {
      this(widget, Collections.singletonList(row));
   }

   public HudTextListRenderer(HudWidgetModule widget, List<? extends HudTextRow> rows) {
      super(widget);
      this.rows = rows;
   }

   @Override
   public float[] calculateDimensions() {
      float height = 0.0F;
      float width = 0.0F;
      for (HudTextRow row : this.rows) {
         row.updateVisibility();
         if (row.getVisibility()
            != 0.0F) {
            float easedVisibility = row.getVisibility();
            easedVisibility = (float)Mth.clamp(
               1.0D - Math.pow(1.0F - easedVisibility, 4.0D), 0.0, 1.0D
            );
            float textWidth = FontRenderer.INSTANCE
               .getWidth(
                  row.getText()
                     .getString()
               );
            height += this.getHeight() * easedVisibility + 1;
            if (textWidth > width) {
               width = textWidth;
            }
         }
      }
      if (!renderingHud && width == 0.0F) {
         width = 50.0F;
         height = this.getHeight();
      }
      width *= hudSettings.getTextScale();
      height *= hudSettings.getTextScale();
      this.dimensionBuffer[0] = width;
      this.dimensionBuffer[1] = height;
      return this.dimensionBuffer;
   }

   @Override
   public void render(PoseStack poses, double mouseX, double mouseY) {
      super.render(poses, mouseX, mouseY);
      float scale = hudSettings.getTextScale();
      float rowY = this.getContentY(
         this.getHeight() * scale
      );
      for (HudTextRow row : this.rows) {
         float easedVisibility = row.getVisibility();
         if (easedVisibility != 0.0F) {
            poses.pushPose();
            try {
               easedVisibility = (float)Mth.clamp(
                  1.0D - Math.pow(1.0F - easedVisibility, 4.0D), 0.0, 1.0D
               );
               Color rowColor = this.getRowColor(rowY, row);
               float animatedWidth = row.widthAnimation
                     .updateAndGet()
                  * scale;
               float hiddenWidth = animatedWidth * (1.0F - easedVisibility);
               poses.translate(
                  this.getContentX(animatedWidth)
                     - hiddenWidth * this.getHorizontalDirection(),
                  rowY,
                  0.0F
               );
               poses.scale(scale, scale, 1.0F);
               row.renderRow(poses, 0.0F, 0.0F, rowColor, hudSettings.shouldDrawTextShadow());
               rowY += (this.getHeight() * easedVisibility + 1)
                  * this.getVerticalDirection()
                  * scale;
            } finally {
               poses.popPose();
            }
         }
      }
   }

   public Color getRowColor(float rowPosition, HudTextRow row) {
      return this.widget
         .getHudColor(rowPosition);
   }

   public List<? extends HudTextRow> getRows() {
      return this.rows;
   }

}
