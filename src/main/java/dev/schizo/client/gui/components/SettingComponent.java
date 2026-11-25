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
import dev.schizo.client.integrations.ModuleCompatibility;
import dev.schizo.client.integrations.MultiplayerCompatibilityWarnings;
import dev.schizo.client.gui.CloseState;
import dev.schizo.client.render.text.LanguageTable;
import dev.schizo.client.gui.components.EnumSettingComponent;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.gui.ScissorStack;
import dev.schizo.client.gui.components.ModuleComponent;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;

public abstract class SettingComponent<T> extends AbstractComponent {
   protected final FloatAnimation textScrollAnimation = new FloatAnimation();
   protected final CloseState parentCloseState;
   protected final Setting<T> setting;
   private boolean wasHoveredForTextScroll;
   private long textScrollHoverStartedNanos;
   public boolean editing;

   @Override
   public int getHeight() {
      return !this.setting
            .isVisible()
         ? 0
         : this.getBaseHeight();
   }

   public boolean isUnavailable() {
      boolean unavailable = !this.setting
            .isVisible()
         || this.parentCloseState.isClosed();
      if (unavailable) {
         this.setting.expanded = false;
      }

      return unavailable;
   }

   public Setting<T> getSetting() {
      return this.setting;
   }

   protected ClickGuiScreen getClickGui() {
      return ClientContext.getClickGui();
   }

   public CloseState getParentCloseState() {
      return this.parentCloseState;
   }

   public SettingComponent(Panel panel, Setting<T> setting) {
      this(panel, () -> false, setting);
   }

   public SettingComponent(Panel panel, CloseState parentCloseState, Setting<T> setting) {
      super(panel, 0);
      this.parentCloseState = parentCloseState;
      this.setting = setting;
   }

   private void updateTextScroll(float textWidth) {
      if (textWidth
         <= this.panel
               .getWidth()
            - (4)) {
         this.wasHoveredForTextScroll = false;
         this.textScrollAnimation
            .setValue(
               0.0F
            );
      } else {
         if (this.hovered) {
            if (!this.wasHoveredForTextScroll) {
               this.textScrollHoverStartedNanos = System.nanoTime();
            }

            if (System.nanoTime() - this.textScrollHoverStartedNanos > 750_000_000L) {
               this.textScrollAnimation
                  .setTarget(
                     textWidth
                        - (
                           this.panel
                                 .getWidth()
                              - (4)
                        ),
                     500L
                  );
            }
         } else {
            this.textScrollAnimation
               .setTarget(0.0F, 500L);
         }

         this.wasHoveredForTextScroll = this.hovered;
      }
   }

   protected void renderWithTextScroll(
      PoseStack poseStack, String text, Runnable renderer
   ) {
      poseStack.pushPose();
      try {
         this.updateTextScroll(FontRenderer.INSTANCE.getWidth(text));
         float scrollOffset = -this.textScrollAnimation.getValue();
         boolean scissorRequired = scrollOffset != 0.0F && !(this instanceof EnumSettingComponent<?>);
         if (scissorRequired) {
            int clipY = this.panel.getY() + this.yOffset;
            int clipHeight = this.getHeight();
            if (this.parentCloseState instanceof ModuleComponent parentModule) {
               clipY = this.panel.getY() + parentModule.getYOffset();
               clipHeight = parentModule.getHeight();
            }
            GuiDrawUtils.flush();
            ScissorStack.pushIntersection(
               this.panel.getX() + this.getHorizontalPadding(),
               clipY,
               this.panel.getWidth() - this.getHorizontalPadding(),
               clipHeight
            );
         }

         try {
            poseStack.translate(scrollOffset, 0.0F, 0.0F);
            renderer.run();
         } finally {
            if (scissorRequired) {
               try {
                  GuiDrawUtils.flush();
               } finally {
                  ScissorStack.pop();
               }
            }
         }
      } finally {
         poseStack.popPose();
      }
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      if (this.isMouseOver(mouseX, mouseY)
         && this.parentCloseState instanceof ModuleComponent parentModule) {
         String tooltip = null;
         if (this.getUiModule().descriptionsSetting.getValue()
            && this.getUiModule().settingsSetting.getValue()) {
            String moduleConfigName = parentModule.getModule().getConfigName();
            String settingConfigName = this.setting.getConfigName();
            LanguageTable translations = ClientContext.languageTable;
            tooltip = translations.getTranslation(
               "descriptions.settings.%s.%s".formatted(moduleConfigName, settingConfigName)
            );
         }

         if (MultiplayerCompatibilityWarnings.isMultiplayerSession()) {
            ModuleCompatibility.SettingAssessment assessment = ModuleCompatibility.assessSetting(
               parentModule.getModule().getClass(),
               this.setting.getConfigName()
            );
            String className = parentModule.getModule().getClass().getSimpleName();
            String reviewState = assessment.individuallyReviewed() ? "reviewed" : "UNVERIFIED";
            String compatibilityReview = "Multiplayer review [" + reviewState + "]: "
               + assessment.classification() + " setting '"
               + this.setting.getConfigName() + "'. Parent module: "
               + assessment.inheritedModuleClassification() + ". "
               + assessment.reason()
               + " See ;compat module " + className + ".";
            tooltip = tooltip == null || tooltip.isBlank()
               ? compatibilityReview
               : tooltip + "\n\n" + compatibilityReview;
         }

         if (tooltip != null && !tooltip.isBlank()) {
            ClientContext.getClickGui().setTooltipText(tooltip);
         }
      }

      super.render(poseStack, mouseX, mouseY);
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.isUnavailable()) {
         super.mouseClicked(mouseX, mouseY, button);
      }
   }

}
