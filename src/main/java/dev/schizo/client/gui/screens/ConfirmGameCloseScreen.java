/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientAccess;
import dev.schizo.mixin.MinecraftAccessor;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ConfirmGameCloseScreen extends ConfirmScreen {
   public ConfirmGameCloseScreen() {
      super(
         new CloseConfirmationHandler(),
         Component.literal("Confirm Game Close"),
         Component.literal("Are you sure that you want to close minecraft?")
      );
   }

   private static final class CloseConfirmationHandler implements BooleanConsumer, ClientAccess {
      @Override
      public void accept(boolean confirmed) {
         if (!((MinecraftAccessor)ClientAccess.schizoMinecraft).schizo$isDisconnecting()) {
            Minecraft client = ClientAccess.schizoMinecraft;
            client.setScreen(null);
         }

         if (confirmed) {
            Minecraft client = ClientAccess.schizoMinecraft;
            client.destroy();
         } else {
            Minecraft client = ClientAccess.schizoMinecraft;
            long windowHandle = (client.getWindow()).handle();
            GLFW.glfwSetWindowShouldClose(windowHandle, false);
         }
      }
   }
}
