/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.gui.components.AbstractComponent;
import dev.schizo.client.gui.panels.Panel;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.function.Supplier;

public class ButtonComponent extends AbstractComponent {
   private final Supplier<String> labelSupplier;
   private boolean hoverOffsetEnabled;
   private final ButtonLabelLines labelLines=new ButtonLabelLines();

   public ButtonComponent(Panel parentPanel, Supplier<String> labelSupplier) {
      super(parentPanel, 0);
      this.labelSupplier = labelSupplier;
   }

   public ButtonComponent(Panel parentPanel, String label) {
      super(parentPanel, 0);
      this.labelSupplier = () -> label;
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      var lines = labelLines.resolve(this.getLabel());

      float hoverOffset=this.hoverOffsetEnabled && this.isMouseOver(mouseX,mouseY)?1F:0F;
      int lineHeight=FontRenderer.INSTANCE.getHeight();
      for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
         FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               lines.get(lineIndex),
               this.panel
                     .getX()
                  + (3),
               this.panel
                        .getY()
                     + this.yOffset
                  + this.getTextYOffset()
                  + lineIndex * lineHeight
                  + 1.0F
                  - hoverOffset,
               Color.white
            );
      }

      super.render(poseStack, mouseX, mouseY);
   }

   @Override
   public int getHeight() {
      return super.getHeight()
         + FontRenderer.INSTANCE
               .getHeight()
            * (labelLines.resolve(this.getLabel()).size() - 1)
         + 1;
   }

   protected void renderButtonBackground(PoseStack poseStack) {
      Color buttonColor = this.getUiModule().buttonSetting.getValue();
      buttonColor = ColorUtils.withAlpha(
         buttonColor,
         buttonColor.getAlpha()
            / 255.0F
            * 0.1F
      );
      if (this.getUiModule().elementGradientSetting.getValue()
         )
       {
         GuiDrawUtils.fillVerticalGradient(
            poseStack,
            this.panel
                  .getX()
               + 1,
            this.panel
                     .getY()
                  + this.yOffset
               + 0.5F,
            this.panel
                  .getWidth()
               - 1,
            this.getHeight()
               - 0.5F,
            buttonColor.hashCode(),
            ColorUtils.toArgb(buttonColor, 0)
         );
      } else {
         RenderUtils.INSTANCE
            .fillRect(
               poseStack,
               this.panel
                     .getX()
                  + 1,
               this.panel
                        .getY()
                     + this.yOffset
                  + 0.5F,
               this.panel
                     .getX()
                  + this.panel
                     .getWidth()
                  - 1,
               this.panel
                        .getY()
                     + this.yOffset
                     + this.getHeight()
                  - 0.5F,
               this.getUiModule().buttonSetting.getValue()
            );
      }
   }

   public String getLabel() {
      return this.labelSupplier.get();
   }

   public void setHoverOffsetEnabled(boolean enabled) {
      this.hoverOffsetEnabled = enabled;
   }

}
