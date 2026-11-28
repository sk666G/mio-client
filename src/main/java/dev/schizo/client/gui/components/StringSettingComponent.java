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
   private LegacyTextDraft draft;
   private String editError="";

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
            if(!draft.matchesLive(setting.getValue())) {
               editError="Value changed externally. Escape and reopen to edit the current value.";
               return;
            }
            try {
               if(draft.changed()) setting.setValue(draft.text());
               this.cancelInput();
            } catch(IllegalArgumentException rejected) {
               editError="Setting rejected this value.";
            }
         }
         case GLFW.GLFW_KEY_BACKSPACE -> {draft.backspace();editError="";}
         case GLFW.GLFW_KEY_DELETE -> {draft.delete();editError="";}
         case GLFW.GLFW_KEY_HOME -> draft.home();
         case GLFW.GLFW_KEY_END -> draft.end();
         case GLFW.GLFW_KEY_RIGHT -> draft.right();
         case GLFW.GLFW_KEY_LEFT -> draft.left();
      }
   }

   public String getText() {
      return this.editing
         ? this.draft.text()
         : this.setting.getValue();
   }

   public StringSettingComponent(Panel panel, CloseState closeState, Setting<String> setting) {
      super(panel, closeState, (StringSetting)setting);
   }

   public void beginEditing() {
      if (!this.editing) {
         this.getClickGui().setEditingSetting(this);
         this.editing = true;
         this.draft=new LegacyTextDraft(this.setting.getValue());
         this.editError="";
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
            String prefix = draft.text().substring(0,draft.caret());
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

            if (!draft.text().isEmpty()) {
               FontRenderer.INSTANCE
                  .drawWithShadow(
                     poseStack,
                     draft.text(),
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
      draft=null;
      editError="";
   }

   private void insertText(String text) {
      if(text==null || text.isEmpty()) return;
      draft.insert(text);
      editError="";
   }

   @Override public void textTyped(String text) {
      if(!isUnavailable() && editing) insertText(text);
   }

   @Override public void charTyped(char character) {
      textTyped(String.valueOf(character));
   }
}
