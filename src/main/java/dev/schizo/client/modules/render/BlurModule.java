/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.ScreenChangeEvent;
import dev.schizo.client.events.HudRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.gui.screens.ClientScreen;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BlurModule extends Module {
   private static final Component CLICK_SCREEN_TITLE;
   private final Setting<Integer> radiusSetting;
   private final FloatAnimation openingAnimation;

   public int getRadius() {
      return (this.radiusSetting.getValue());
   }

   public BlurModule() {
      super("Blur", "Blurs the background in GUI's.", ModuleCategory.RENDER, new String[0]);
      String radiusKey = new String("Radius");
      this.radiusSetting = this.add(new NumberSetting<>(radiusKey, 6, 1, 16));
      this.openingAnimation = new FloatAnimation();
      this.setToggled(true);
   }

   static {

      String clickTitle = "CLICK";
      CLICK_SCREEN_TITLE = Component.literal(clickTitle);
   }


   private static boolean isBlurExcluded(Screen screen) {
      if (screen == null || screen instanceof ClientScreen || screen instanceof ChatScreen) {
         return true;
      }
      return screen.getTitle().equals(CLICK_SCREEN_TITLE);
   }

   @EventHandler
   private void onScreenChange(
      ScreenChangeEvent.Before event
   ) {
      if (event.isCancelled()) {
         return;
      }
      if (event.getNewScreen() == null) {
         FloatAnimation animation = this.openingAnimation;
         resetOpeningAnimation(animation);
         return;
      }
      if (event.getPreviousScreen() != null) {
         return;
      }
      this.openingAnimation.setTarget(1.0F, 75L);
   }

   private static void resetOpeningAnimation(FloatAnimation animation) {
      animation.setValue(0.0F);
   }

   @EventHandler(priority = -200)
   private void onHudRender(HudRenderEvent.Unconditional event) {
      PoseStack poseStack = event.getPoseStack();
      this.renderBackgroundBlur(poseStack);
   }

   public void renderBackgroundBlur(PoseStack poseStack) {
      Screen currentScreen = ClientAccess.schizoMinecraft.screen;
      if (isBlurExcluded(currentScreen)) {
         return;
      }
      (RenderUtils.getEntityBuffers()).endBatch();
      Runnable drawFullscreenMask = () -> {
         RenderUtils renderer = RenderUtils.INSTANCE;
         float width = (ClientAccess.schizoMinecraft.getWindow()).getGuiScaledWidth();
         float height = (ClientAccess.schizoMinecraft.getWindow()).getGuiScaledHeight();

         renderer.fillRect(
            poseStack, 0.0F, 0.0F, width, height, 9999, -1
         );
      };

      float radius = this.getRadius();
      FloatAnimation animationCapture = this.openingAnimation;

      MaskShaderRenderer.renderBlur(
         drawFullscreenMask, radius * animationCapture.getValue()
      );
   }

}
