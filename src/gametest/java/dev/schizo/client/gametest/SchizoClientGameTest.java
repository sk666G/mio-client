package dev.schizo.client.gametest;

import dev.schizo.client.ClientContext;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.gui.screens.ExpeditionScreen;
import dev.schizo.client.gui.screens.HudLayoutPreviewScreen;
import dev.schizo.client.utils.inventory.TemporaryHotbarSelection;
import dev.schizo.client.modules.misc.StashEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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

         context.runOnClient(minecraft -> {
            var inventory = minecraft.player.getInventory();
            int original = inventory.getSelectedSlot();
            var selection = new TemporaryHotbarSelection();
            try {
               int temporary = (original + 1) % 9;
               selection.select(temporary);
               inventory.setSelectedSlot((original + 2) % 9);
               inventory.setSelectedSlot(temporary);
               selection.restore(true);
               if (inventory.getSelectedSlot() != temporary) {
                  throw new IllegalStateException("A stale selection owner overrode a manual slot change");
               }
            } finally {
               selection.clear();
               inventory.setSelectedSlot(original);
            }
            var stashes = ClientContext.stashRegistry;
            if (!stashes.registerEntry(new StashEntry("QA storage", 100, 200, "OVERWORLD", "singleplayer"))
               || !stashes.registerEntry(new StashEntry("QA storage", 300, 400, "OVERWORLD", "singleplayer"))) {
               throw new IllegalStateException("Distinct stashes with identical descriptions were rejected");
            }
         });
         context.setScreen(ExpeditionScreen::new);
         context.waitForScreen(ExpeditionScreen.class);
         context.waitTicks(2);
         context.takeScreenshot("schizo-expedition-discoveries");
         for (String tab : new String[]{"Travel", "Profiles", "Compatibility"}) {
            context.runOnClient(minecraft -> clickButton(minecraft.screen, tab));
            context.waitTicks(2);
            context.takeScreenshot("schizo-expedition-" + tab.toLowerCase(java.util.Locale.ROOT));
         }
         context.setScreen(() -> null);
         context.waitForScreen(null);

         var layoutBefore = new java.util.concurrent.atomic.AtomicReference<com.google.gson.JsonObject>();
         context.runOnClient(minecraft -> {
            var layout = ClientContext.getHudEditor().exportLayoutPreset();
            layoutBefore.set(layout.deepCopy());
            minecraft.setScreen(new HudLayoutPreviewScreen(layout));
         });
         context.waitForScreen(HudLayoutPreviewScreen.class);
         context.waitTicks(2);
         context.takeScreenshot("schizo-hud-layout-preview");
         context.setScreen(() -> null);
         context.waitForScreen(null);
         context.runOnClient(minecraft -> {
            if (!layoutBefore.get().equals(ClientContext.getHudEditor().exportLayoutPreset())) {
               throw new IllegalStateException("Cancelling a layout preview changed the original layout");
            }
         });

         long failuresAfter = EventFailureTracker.GLOBAL.totalFailures();
         if (failuresAfter != failuresBefore) {
            throw new IllegalStateException(
               "Schizo event callback failures increased during GUI gametest: "
                  + failuresBefore + " -> " + failuresAfter
            );
         }
      }
   }
   private static void clickButton(Screen screen, String label) {
      for (var child : screen.children()) {
         if (child instanceof Button button && button.getMessage().getString().equals(label)) {
            var event = new MouseButtonEvent(button.getX() + 2, button.getY() + 2, new MouseButtonInfo(0, 0));
            if (!screen.mouseClicked(event, false)) throw new IllegalStateException("Button ignored click: " + label);
            screen.mouseReleased(event);
            return;
         }
      }
      throw new IllegalStateException("Missing button: " + label);
   }

}
