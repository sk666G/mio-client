/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.utils.InterpolatedFloat;
import dev.schizo.client.gui.CloseState;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.gui.CursorType;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.screens.ClientScreen;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.render.FontRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.text.NumberFormat;
import net.minecraft.util.Mth;
public class NumericSettingComponent<N extends Number> extends SettingComponent<N> {
   private static final NumberFormat NUMBER_FORMAT;
   private final InterpolatedFloat sliderFillAnimation = new InterpolatedFloat(
      6.0F,
      false
   );
   private final ElapsedTimer cursorBlinkTimer = new ElapsedTimer();
   private int sliderX;
   private int sliderY;
   private boolean dragging;
   private boolean cursorVisible = false;
   private float sliderFillWidth;
   private String inputText = "";
   private String inputError="";

   private String getBlinkingCursor() {
      if (this.cursorBlinkTimer
         .hasElapsed(500L)) {
         this.cursorVisible = !this.cursorVisible;
         this.cursorBlinkTimer.reset();
      }

      return this.cursorVisible
            && this.editing
         ? "_"
         : "";
   }

   @Override
   public void keyPressed(int keyCode) {
      if (this.isUnavailable() || !this.editing || !this.parentCloseState.isOpen()) {
         return;
      }

      switch (keyCode) {
         case 256:
            this.editing = false;
            this.inputError="";
            if(this.getClickGui().getEditingSetting()==this) this.getClickGui().setEditingSetting(null);
            ClientContext.getClickGui()
               .suppressEscapeClose();
            break;
         case 257:
         case 335:
            this.commitEditedValue();
            break;
         case 259:
            this.inputError="";
            if (!this.inputText.isEmpty()) {
               this.inputText = this.inputText
                  .substring(0, this.inputText.length() - 1);
            }
      }
   }

   public NumericSettingComponent(Panel panel, CloseState parentCloseState, NumberSetting<N> setting) {
      super(panel, parentCloseState, setting);
      this.numberSetting = setting;
   }

   static {
      NUMBER_FORMAT = NumberFormat.getNumberInstance();
      NUMBER_FORMAT.setMinimumFractionDigits(1);
      NUMBER_FORMAT.setMaximumFractionDigits(4);
      NUMBER_FORMAT.setGroupingUsed(false);
   }

   private final NumberSetting<N> numberSetting;

   private void commitEditedValue() {
      if(!this.inputText.isBlank()) {
         Double value=NumericControlInput.parse(this.inputText,setting.getMinimumValue().doubleValue(),
            setting.getMaximumValue().doubleValue(),setting.getValue() instanceof Integer);
         if(value==null) {
            this.inputError=setting.getValue() instanceof Integer?"Enter a whole number inside the allowed range.":"Enter a finite number inside the allowed range.";
            this.editing=true;
            return;
         }
         try {
            this.numberSetting.setNumber(value);
         } catch(IllegalArgumentException rejected) {
            this.inputError="Value rejected by the setting.";
            this.editing=true;
            return;
         }
      }
      this.editing=false;
      this.inputError="";
      if(this.getClickGui().getEditingSetting()==this) this.getClickGui().setEditingSetting(null);
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      if (this.isUnavailable()) {
         if (this.editing
            && this.getClickGui()
                  .getEditingSetting()
               == this) {
            this.getClickGui()
               .setEditingSetting(null);
         }

         this.editing = false;
         this.dragging=false;
         this.inputError="";
         return;
      }

      super.render(poseStack, mouseX, mouseY);
      if(this.hovered && !inputError.isEmpty() && this.getClickGui()!=null) this.getClickGui().setTooltipText(inputError);
      if (this.hovered
         && this.editing) {
         ClientScreen.cursor = CursorType.INPUT;
      }

      if (!this.panel.isDocked()) {
      GuiDrawUtils.fillRect(
         poseStack,
         this.sliderX
            + this.getHorizontalPadding(),
         this.sliderY
            + 0.5F,
         (int)(
            this.sliderX
               + Mth.clamp(
                  this.sliderFillAnimation
                     .updateAndGet(),
                  0.0F,
                  this.panel
                        .getWidth()
                     - (3)
               )
               + 2.0F
         ),
         this.sliderY
               + this.getHeight()
            - 0.5F,
         this.getUiModule().colorSetting.getValue()
      );
      }
      String displayText;
      if (this.editing) {
         displayText = this.inputText;
      } else {
         String valueText = this.setting.shouldDisplayBoundaryLabel()
            ? this.setting.getBoundaryLabel()
            : NUMBER_FORMAT.format(this.setting.getValue().doubleValue()) + this.numberSetting.getUnit();
         displayText = this.setting.getName() + ": " + valueText;
      }

      String measuredText = this.editing ? displayText + "_" : displayText;
      String drawnText = this.editing && !this.getBlinkingCursor().isEmpty() ? measuredText : displayText;

      if (this.panel.isDocked()) {
         float x = this.panel.getX(), y = this.panel.getY() + this.yOffset;
         float w = this.panel.getWidth(), h = this.getHeight();
         Color accent = this.getUiModule().colorSetting.getValue();
         float p = StudioControlGeometry.fraction(this.sliderFillAnimation.updateAndGet(), 0, Math.max(1, w - 18));
         float hover = StudioControlGeometry.clampProgress(this.getHoverAnimationValue());
         StudioControlStyle.softRect(poseStack, x + 5, y + 1, w - 10, h - 2,
            dev.schizo.client.render.ColorUtils.withAlpha(Color.WHITE, Math.round(9 + 14 * hover)));
         if (h >= 11) StudioControlStyle.slider(poseStack, x + 9, y + h - 4, w - 18, p, accent);
         FontRenderer.INSTANCE.drawWithoutShadow(poseStack, StudioControlStyle.fitText(drawnText, w - 18),
            x + 9, y + Math.max(0, this.getTextYOffset() - 2), this.getUiModule().textColorSetting.getValue());
         return;
      }

      this.renderWithTextScroll(
         poseStack,
         measuredText,
         () -> FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               drawnText,
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

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isUnavailable()) {
         this.editing = false;
         return;
      }

      super.mouseClicked(mouseX, mouseY, button);
      if (this.isMouseOver(mouseX, mouseY)
         && button == 0
         && this.parentCloseState
            .isOpen()) {
         if (this.editing) {
            this.commitEditedValue();
         } else {
            this.dragging = true;
         }
      }

      if (this.isMouseOver(mouseX, mouseY)
         && button == 1
         && this.parentCloseState
            .isOpen()) {
         this.editing = !this.editing;
         if (this.editing) {
            if (this.getClickGui()
                     .getEditingSetting()
                  != null
               && this.getClickGui()
                     .getEditingSetting()
                  != this) {
               this.getClickGui()
                  .getEditingSetting()
                  .editing = false;
            }

            this.getClickGui()
               .setEditingSetting(this);
            this.dragging=false;
            this.inputError="";
            this.inputText = (this.setting
                  .getValue())
               .toString();
         } else if (this.getClickGui()
               .getEditingSetting()
            == this) {
            this.commitEditedValue();
         }
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return super.isMouseOver(mouseX, mouseY)
         || this.dragging;
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (this.isUnavailable()) {
         this.dragging = false;
         this.sliderFillAnimation.setValue(0.0F);
         return;
      }

      this.sliderY = this.panel
            .getY()
         + this.yOffset;
      boolean docked = this.panel.isDocked();
      this.sliderX = this.panel.getX() + (docked ? 9 : 0);
      double sliderWidth = this.getPanel().getWidth() - (docked ? 18 : 2);
      double minimumValue = (this.setting
            .getMinimumValue())
         .doubleValue();
      double maximumValue = (this.setting
            .getMaximumValue())
         .doubleValue();
      this.sliderFillWidth = (float)Math.max(0, sliderWidth) * StudioControlGeometry.fraction(
         this.setting.getValue().doubleValue(), minimumValue, maximumValue);
      if (sliderWidth <= 0 || !Double.isFinite(maximumValue - minimumValue) || maximumValue <= minimumValue) {
         this.dragging = false;
         this.sliderFillAnimation.setValue(0.0F);
         return;
      }
      if(this.dragging) {
         double proposed=NumericControlInput.slider(mouseX,sliderX,sliderWidth,minimumValue,maximumValue,
            setting.getValue() instanceof Integer);
         if(Double.isFinite(proposed) && Double.compare(proposed,setting.getValue().doubleValue())!=0)
            this.numberSetting.setNumber(proposed);
      }

      this.sliderFillWidth = (float)sliderWidth * StudioControlGeometry.fraction(
         this.setting.getValue().doubleValue(), minimumValue, maximumValue);

      this.sliderFillAnimation
         .setTarget(
            this.sliderFillWidth
         );
   }

   @Override
   public void cancelInput() {
      super.cancelInput();
      this.dragging=false;
      this.inputError="";
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }
   }

   @Override
   public void charTyped(char character) {
      int characterCode = character;
      if (this.isUnavailable()) {
         return;
      }

      String allowedCharacters = "1234567890.,-";
      if (characterCode
         == 44) {
         characterCode = 46;
      }

      if (!this.editing
         || !this.parentCloseState.isOpen()
         || !FontRenderer.containsCharacter(allowedCharacters, characterCode)) {
         return;
      }

      if (characterCode
            == 46
         && FontRenderer.containsCharacter(
            this.inputText, characterCode
         )) {
         return;
      }

      if(this.inputText.length()>=48 || characterCode=='-' && !this.inputText.isEmpty()) return;
      this.inputError="";
      this.inputText += (char)characterCode;
   }

}
