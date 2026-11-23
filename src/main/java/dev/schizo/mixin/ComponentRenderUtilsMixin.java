/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.schizo.client.modules.misc.BetterChatModule;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.AtlasTextRenderer;
import dev.schizo.client.modules.client.FontsModule;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ComponentRenderUtils.class)
public class ComponentRenderUtilsMixin {
   private static final BetterChatModule betterChatModule = ClientContext.getModule(BetterChatModule.class);
   private static final FontsModule fontsModule = ClientContext.getModule(FontsModule.class);

   @ModifyExpressionValue(method = "wrapComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;getSplitter()Lnet/minecraft/client/StringSplitter;"))
   private static StringSplitter useChatFontSplitter(StringSplitter originalSplitter) {
      AtlasTextRenderer textRenderer = FontRenderer.INSTANCE
         .getRenderer();
      return textRenderer != null
            && fontsModule.isToggled()
            && fontsModule.chatSetting.getValue()
         ? textRenderer.getSplitter()
         : originalSplitter;
   }

   @ModifyVariable(method = "wrapComponents", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private static FormattedText highlightOwnName(FormattedText originalText) {
      return betterChatModule.highlightNames(originalText);
   }

}
