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
import dev.schizo.client.gui.ScissorStack;
import java.util.Objects;
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
   private String originalValue;
   private String editError="";
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
               this.insertText(clipboard);
            }
         }
         case GLFW.GLFW_KEY_ESCAPE -> {
            ClientContext.getClickGui().suppressEscapeClose();
            this.cancelInput();
         }
         case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
            if(!Objects.equals(originalValue,setting.getValue())) {
               editError="Value changed externally. Escape and reopen to edit the current value.";
               return;
            }
            try {
               if(!Objects.equals(originalValue,editingText)) setting.setValue(editingText);
               this.cancelInput();
            } catch(IllegalArgumentException rejected) {
               editError="Setting rejected this value.";
            }
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
         this.originalValue=this.setting.getValue();
         this.editingText = this.originalValue;
         this.editError="";
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
         if(!editError.isEmpty()) getClickGui().setTooltipText(editError);
         GuiDrawUtils.flush();
         ScissorStack.pushIntersection(panel.getX()+3,panel.getY()+yOffset,Math.max(0,panel.getWidth()-6),getHeight());
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
            try { GuiDrawUtils.flush(); }
            finally {
               poseStack.popPose();
               ScissorStack.pop();
            }
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
         if(this.editing) this.cancelInput();
         else this.beginEditing();
      }
   }

   @Override public void cancelInput() {
      super.cancelInput();
      editingText=null;
      originalValue=null;
      cursorIndex=0;
      editError="";
   }

   private void insertText(String text) {
      if(text==null || text.isEmpty()) return;
      editingText=new StringBuilder(editingText).insert(cursorIndex,text).toString();
      cursorIndex+=text.length();
      editError="";
   }

   @Override public void textTyped(String text) {
      if(!isUnavailable() && editing) insertText(text);
   }

   @Override public void charTyped(char character) {
      textTyped(String.valueOf(character));
   }
}
