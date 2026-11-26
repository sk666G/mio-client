/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.modules.client.HudColorMode;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.commands.PanicCommand;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.hud.WatermarkModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

   protected TitleScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void renderTitleWatermark(
      GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback, @Local(ordinal = 1) float opacity
   ) {
      if (!PanicCommand.unloaded) {
         WatermarkModule watermarkModule = MixinModuleRegistry.getModule(WatermarkModule.class);
         HUDModule hudModule = MixinModuleRegistry.getModule(HUDModule.class);
         if (watermarkModule != null && hudModule != null) {
            Minecraft client = ClientAccess.schizoMinecraft;
            int hudColor = hudModule.fadeSetting.getValue().getColor(hudModule, 0.0F).getRGB();
            String versionText = ChatFormatting.WHITE + watermarkModule.getVersionText();
            graphics.drawString(
               client.font,
               "Schizo " + versionText,
               2,
               2,
               ARGB.color(opacity, hudColor)
            );
         }
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      UIModule uiModule = MixinModuleRegistry.getModule(UIModule.class);
      if (uiModule != null
         && !uiModule.getKeybind().mouse()
         && uiModule.getKeybind().code() == keyEvent.key()) {
         uiModule.enable();
         ClientContext.getClickGui().open();
      }

      return super.keyPressed(keyEvent);
   }
}
