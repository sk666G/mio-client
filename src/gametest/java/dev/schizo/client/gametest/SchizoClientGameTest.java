package dev.schizo.client.gametest;

import dev.schizo.client.ClientContext;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.gui.screens.CommandPaletteScreen;
import dev.schizo.client.gui.screens.HudEditorScreen;
import dev.schizo.client.gui.screens.PerformanceCenterScreen;
import dev.schizo.client.modules.client.UIModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
public final class SchizoClientGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      try (TestSingleplayerContext singleplayer = GameTestWorlds.create(context)) {
         singleplayer.getClientWorld().waitForChunksRender();
         context.waitTicks(5);

         context.runOnClient(minecraft -> {
            if (minecraft.level == null || minecraft.player == null) {
               throw new IllegalStateException("Singleplayer world did not initialize");
            }
            if (ClientContext.moduleRegistry == null
               || ClientContext.configurationManager == null
               || ClientContext.taskScheduler == null
               || ClientContext.frameTiming == null) {
               throw new IllegalStateException("Schizo client services are incomplete");
            }
         });

         long failuresBefore = EventFailureTracker.GLOBAL.totalFailures();

         context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(3);
         context.takeScreenshot("schizo-clickgui");

         context.setScreen(() -> null);
         context.waitForScreen(null);
         context.waitTicks(2);
         context.runOnClient(minecraft -> {
            UIModule ui = ClientContext.getModule(UIModule.class);
            if (ui == null) {
               throw new IllegalStateException("UI module is unavailable");
            }
            if (ui.isToggled()) {
               ui.disable();
            }
         });

         context.runOnClient(minecraft -> CommandPaletteScreen.openPalette());
         context.waitForScreen(CommandPaletteScreen.class);
         context.getInput().typeChars("health");
         context.waitTicks(2);
         context.takeScreenshot("schizo-command-palette");
         context.setScreen(() -> null);
         context.waitForScreen(null);

         context.setScreen(PerformanceCenterScreen::new);
         context.waitForScreen(PerformanceCenterScreen.class);
         context.waitTicks(3);
         context.takeScreenshot("schizo-performance-center");
         context.setScreen(() -> null);
         context.waitForScreen(null);

         context.setScreen(HudEditorScreen::new);
         context.waitForScreen(HudEditorScreen.class);
         context.waitTicks(3);
         context.takeScreenshot("schizo-hud-editor");
         context.setScreen(() -> null);
         context.waitForScreen(null);

         long failuresAfter = EventFailureTracker.GLOBAL.totalFailures();
         if (failuresAfter != failuresBefore) {
            throw new IllegalStateException(
               "Schizo event callback failures increased during GUI gametest: "
                  + failuresBefore + " -> " + failuresAfter
            );
         }
      }
   }
}
