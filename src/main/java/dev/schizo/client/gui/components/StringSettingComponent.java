/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.CursorType;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;

import dev.schizo.client.gui.CloseState;
import dev.schizo.client.gui.components.SettingComponent;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.screens.ClientScreen;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

public class StringSettingComponent extends SettingComponent<String> {
   private final ElapsedTimer cursorBlinkTimer = new ElapsedTimer();
   private boolean cursorVisible = false;
   private String editingText;
   private int cursorIndex = 0;

   public String getCursorMarker() {
      if (this.cursorBlinkTimer
         .hasElapsed(500L)) {
         this.cursorVisible = !this.cursorVisible;
         this.cursorBlinkTimer
            .reset();
      }

      return this.cursorVisible ? "_" : "";
   }

   @Override
   public void keyPressed(int key) {
      if (this.isUnavailable() || !this.editing) {
         return;
      }
      switch (key) {
         case GLFW.GLFW_KEY_V -> {
            long window = ClientAccess.schizoMinecraft.getWindow().handle();
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
               || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
               String clipboard = ClientAccess.schizoMinecraft.keyboardHandler.getClipboard();
               this.editingText = new StringBuilder(this.editingText).insert(this.cursorIndex, clipboard).toString();
               this.cursorIndex += clipboard.length();
            }
         }
         case GLFW.GLFW_KEY_ESCAPE -> {
            ClientContext.getClickGui().suppressEscapeClose();
            this.editing = false;
         }
         case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
            this.editing = false;
            this.setting.setValue(this.editingText);
         }
         case GLFW.GLFW_KEY_BACKSPACE -> {
            if (this.cursorIndex > 0) {
               int previous = this.editingText.offsetByCodePoints(this.cursorIndex, -1);
               this.editingText = new StringBuilder(this.editingText).delete(previous, this.cursorIndex).toString();
               this.cursorIndex = previous;
            }
         }
         case GLFW.GLFW_KEY_DELETE -> {
            if (this.cursorIndex < this.editingText.length()) {
               int next = this.editingText.offsetByCodePoints(this.cursorIndex, 1);
               this.editingText = new StringBuilder(this.editingText).delete(this.cursorIndex, next).toString();
            }
         }
         case GLFW.GLFW_KEY_HOME -> this.cursorIndex = 0;
         case GLFW.GLFW_KEY_END -> this.cursorIndex = this.editingText.length();
         case GLFW.GLFW_KEY_RIGHT -> {
            if (this.cursorIndex < this.editingText.length()) {
               this.cursorIndex = this.editingText.offsetByCodePoints(this.cursorIndex, 1);
            }
         }
         case GLFW.GLFW_KEY_LEFT -> {
            if (this.cursorIndex > 0) {
               this.cursorIndex = this.editingText.offsetByCodePoints(this.cursorIndex, -1);
            }
         }
      }
   }

   public String getText() {
      return this.editing
         ? this.editingText
         : this.setting.getValue();
   }

   public StringSettingComponent(Panel panel, CloseState closeState, Setting<String> setting) {
      super(panel, closeState, (StringSetting)setting);
   }

   public void beginEditing() {
      if (!this.editing) {
         this.getClickGui().setEditingSetting(this);
         this.editing = true;
         this.editingText = this.setting.getValue();
         this.cursorIndex = this.editingText.length();
      }
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
         return;
      }

      super.render(poseStack, mouseX, mouseY);
      if (this.hovered) {
         ClientScreen.cursor = CursorType.INPUT;
      }

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
            + this.panel
               .getWidth()
            - 1,
         this.panel
                  .getY()
               + this.yOffset
               + this.getHeight()
            - 0.5F,
         this.getUiModule().colorSetting.getValue()
      );

      if (this.editing) {
         poseStack.pushPose();
         try {
            String cursorMarker = this.getCursorMarker();
            String prefix = this.editingText.substring(0, this.cursorIndex);
            float caretWidth = FontRenderer.INSTANCE
               .getWidth(
                  prefix + "_"
               );
            if (caretWidth
               > this.getPanel()
                     .getWidth()
                  - this.getHorizontalPadding()
                     * (
                        2
                     )) {
               poseStack.translate(
                  this.getPanel()
                           .getWidth()
                        - this.getHorizontalPadding()
                           * (
                              2
                           )
                     - caretWidth,
                  0.0F,
                  0.0F
               );
            }

            if (!this.editingText.isEmpty()) {
               FontRenderer.INSTANCE
                  .drawWithShadow(
                     poseStack,
                     this.editingText,
                     this.panel
                           .getX()
                        + (
                           4
                        ),
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

            if (!cursorMarker.isBlank()) {
               FontRenderer.INSTANCE
                  .drawWithShadow(
                     poseStack,
                     cursorMarker,
                     this.panel
                              .getX()
                           + (
                              4
                           )
                        + FontRenderer.INSTANCE
                           .getWidth(
                              prefix
                           ),
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
         } finally {
            poseStack.popPose();
         }
      } else {
         String labelText = this.getDisplayText();
         this.renderWithTextScroll(
            poseStack,
            labelText,
            () -> FontRenderer.INSTANCE
               .drawWithShadow(
                  poseStack,
                  labelText,
                  this.panel
                        .getX()
                     + (
                        4
                     ),
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
   }

   private String getDisplayText() {
      return this.setting.getName() + ": " + this.setting.getValue();
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isUnavailable()) {
         this.editing = false;
         return;
      }

      super.mouseClicked(mouseX, mouseY, button);
      if (this.isMouseOver(mouseX, mouseY) && button == 0) {
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
         } else if (this.getClickGui()
               .getEditingSetting()
            == this) {
            this.getClickGui()
               .setEditingSetting(null);
         }

         this.editingText = this.setting
            .getValue();
         this.cursorIndex = this.editingText.length();
      }
   }

   @Override
   public void charTyped(char character) {
      if (!this.isUnavailable() && this.editing) {
         this.editingText = new StringBuilder(
               this.editingText
            )
            .insert(this.cursorIndex, character)
            .toString();
         this.cursorIndex++;
      }
   }

}
