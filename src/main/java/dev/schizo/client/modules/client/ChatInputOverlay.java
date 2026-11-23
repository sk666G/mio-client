/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.HudRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.utils.InterpolatedFloat;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatInputOverlay implements ClientAccess {
   private final FloatAnimation commandHighlightAnimation = new FloatAnimation();
   private final HUDModule hudModule;

   private static boolean isCommandInput() {
      if (ClientAccess.schizoMinecraft.screen instanceof ChatScreen chatScreen) {
         EditBox input = chatScreen.input;
         String inputText = input.getValue();

         String prefix = CommandRegistry.getPrefix();
         return inputText.startsWith(prefix);
      } else {
         return false;
      }
   }

   ChatInputOverlay(HUDModule hudModule) {
      this.hudModule = hudModule;
   }

   @EventHandler
   private void onHudRender(HudRenderEvent.Unconditional event) {
      FloatAnimation animation = this.commandHighlightAnimation;

      boolean commandInput = isCommandInput();
      animation.setTarget(commandInput, 250L);
      animation = this.commandHighlightAnimation;

      float animationValue = animation.getValue();

      float highlightAlpha = animationValue * 1.5F;
      InterpolatedFloat heightAnimation = this.hudModule.chatInputHeight;
      float chatInputHeight = heightAnimation.getValue();
      Minecraft widthMinecraft = ClientAccess.schizoMinecraft;
      Window widthWindow = widthMinecraft.getWindow();
      float screenWidth = widthWindow.getGuiScaledWidth();
      Minecraft heightMinecraft = ClientAccess.schizoMinecraft;
      Window heightWindow = heightMinecraft.getWindow();
      float screenHeight = heightWindow.getGuiScaledHeight();
      Options options = ClientAccess.schizoMinecraft.options;

      int backgroundArgb = options.getBackgroundColor((-2147483648));
      Color backgroundColor = new Color(backgroundArgb, true);

      backgroundColor = ColorUtils.multiplyAlpha(
         backgroundColor, chatInputHeight / 12.0F
      );
      PoseStack backgroundPose = event.getPoseStack();

      float backgroundTop = screenHeight - 2.0F - chatInputHeight;

      float backgroundRight = screenWidth - 2.0F;

      GuiDrawUtils.fillRect(
         backgroundPose, 2.0F, backgroundTop, backgroundRight, screenHeight - 2.0F, backgroundColor
      );
      if (highlightAlpha > 0.0F) {
         Setting<Color> outlineColorSetting = UIModule.INSTANCE.colorSetting;
         Color outlineColor = ColorUtils.multiplyAlpha(
            outlineColorSetting.getValue(), highlightAlpha
         );
         PoseStack outlinePose = event.getPoseStack();

         float outlineTop = screenHeight - 3.0F - chatInputHeight;

         float outlineRight = screenWidth - 2.0F;

         GuiDrawUtils.drawRectOutline(
            outlinePose, 1.0F, outlineTop, outlineRight, screenHeight - 2.0F, outlineColor
         );
      }
   }

}
