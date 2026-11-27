/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.utils.ClampedInterpolatedFloat;
import dev.schizo.client.gui.CloseState;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;

public class BooleanSettingComponent extends SettingComponent<Boolean> {
   private final ClampedInterpolatedFloat enabledAnimation = new ClampedInterpolatedFloat(
      () -> 2.0F
         * UIModule.INSTANCE.animSpeedSetting.getValue(),
      true
   );

   public BooleanSettingComponent(Panel panel, CloseState parentCloseState, Setting<Boolean> setting) {
      super(panel, parentCloseState, (BooleanSetting)setting);
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      if (this.isUnavailable()) {
         this.enabledAnimation.reset();
         return;
      }

      this.enabledAnimation
         .setTarget(this.setting.getValue());
      float enabledProgress = this.enabledAnimation
         .updateAndGet();
      super.render(poseStack, mouseX, mouseY);
      if (this.panel.isDocked() && this.panel.getWidth() >= 70 && this.getBaseHeight() >= 12) {
         renderStudioRow(poseStack, enabledProgress);
         return;
      }
      if ((this.setting.getValue() || enabledProgress > 0.01D) && !this.setting.groupOnly) {
         GuiDrawUtils.fillRect(
            poseStack,
            this.panel
                  .getX()
               + this.getHorizontalPadding(),
            this.panel
                     .getY()
                  + this.yOffset
               + 0.5F,
            this.panel
                  .getX()
               + Math.max(
                  this.panel
                           .getWidth()
                        * enabledProgress
                     - 1.0F,
                  2.0F
               ),
            this.panel
                     .getY()
                  + this.yOffset
                  + this.getHeight()
               - 0.5F,
            this.getUiModule().colorSetting.getValue()
         );
      }

      String settingName = this.setting.getName();
      if (!settingName.isEmpty()) {
         this.renderWithTextScroll(
            poseStack,
            settingName,
            () -> FontRenderer.INSTANCE
               .drawWithShadow(
                  poseStack,
                  settingName,
                  this.panel
                        .getX()
                     + (4),
                  this.panel
                        .getY()
                     + this.getTextYOffset()
                     - this.getHoverAnimationValue()
                     + this.yOffset,
                  this.getUiModule()
                     .textColorSetting
                     .getValue()
               )
         );
      }

      if (this.setting.visibilityDependency && !this.setting.groupOnly) {
         String expansionIndicator = this.setting.expanded
            ? "-"
            : "+";
         FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               expansionIndicator,
               this.panel
                        .getX()
                     + this.panel
                        .getWidth()
                     - (4)
                  - FontRenderer.INSTANCE
                     .getWidth(expansionIndicator),
               this.panel
                     .getY()
                  + this.getTextYOffset()
                  - this.getHoverAnimationValue()
                  + this.yOffset,
               this.getUiModule()
                  .textColorSetting
                  .getValue()
            );
      }

      if (this.setting.groupOnly) {
         String groupIndicator = "...";
         FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               groupIndicator,
               this.panel
                        .getX()
                     + this.panel
                        .getWidth()
                     - (4)
                  - FontRenderer.INSTANCE
                     .getWidth(groupIndicator),
               this.panel
                     .getY()
                  + this.getTextYOffset()
                  - this.getHoverAnimationValue()
                  + this.yOffset,
               this.getUiModule()
                  .textColorSetting
                  .getValue()
            );
      }
   }

   private void renderStudioRow(PoseStack pose, float progress) {
      float x = this.panel.getX(), y = this.panel.getY() + this.yOffset;
      float w = this.panel.getWidth(), h = this.getBaseHeight();
      Color accent = this.getUiModule().colorSetting.getValue();
      float hover = StudioControlGeometry.clampProgress(this.getHoverAnimationValue());
      StudioControlStyle.softRect(pose, x + 5, y + 1, w - 10, h - 2,
         dev.schizo.client.render.ColorUtils.withAlpha(Color.WHITE, Math.round(9 + 14 * hover)));
      if (!this.setting.groupOnly) {
         StudioControlStyle.toggle(pose, x + w - 43, y + (h - 10) / 2, progress, accent);
      }
      FontRenderer font = FontRenderer.INSTANCE;
      String label = StudioControlStyle.fitText(this.setting.getName(), w - 60);
      font.drawWithoutShadow(pose, label, x + 9, y + this.getTextYOffset(), this.getUiModule().textColorSetting.getValue());
      if (this.setting.visibilityDependency || this.setting.groupOnly) {
         String indicator = this.setting.groupOnly ? "..." : this.setting.expanded ? "-" : "+";
         font.drawWithoutShadow(pose, indicator, x + w - 9 - font.getWidth(indicator), y + this.getTextYOffset(), accent);
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isUnavailable()) {
         return;
      }

      if (this.setting.groupOnly && button == 0) {
         return;
      }

      super.mouseClicked(mouseX, mouseY, button);
      if (this.isMouseOver(mouseX, mouseY)) {
         if (button == 0) {
            this.setting
               .setValue(
                  !this.setting.getValue()
               );
         }

         if (button == 1 && this.setting.visibilityDependency) {
            this.setting.expanded = !this.setting.expanded;
         }
      }
   }

}
