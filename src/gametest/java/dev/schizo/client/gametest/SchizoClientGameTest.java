package dev.schizo.client.gametest;

import dev.schizo.client.ClientContext;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.gui.screens.ExpeditionScreen;
import dev.schizo.client.gui.screens.HudLayoutPreviewScreen;
import dev.schizo.client.gui.screens.CommandPaletteScreen;
import dev.schizo.client.gui.screens.HudEditorScreen;
import dev.schizo.client.gui.screens.HealthCenterScreen;
import dev.schizo.client.gui.screens.PerformanceCenterScreen;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.misc.StashEntry;
import dev.schizo.client.utils.inventory.TemporaryHotbarSelection;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
public final class SchizoClientGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      long failuresBefore = EventFailureTracker.GLOBAL.totalFailures();
      AtomicReference<ClientLevel> firstLevel = new AtomicReference<>();
      AtomicReference<LocalPlayer> firstPlayer = new AtomicReference<>();
      AtomicReference<com.google.gson.JsonElement> originalContextProfiles = new AtomicReference<>();

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
            firstLevel.set(minecraft.level);
            firstPlayer.set(minecraft.player);
         });

         context.runOnClient(minecraft -> {
            var profiles = ClientContext.contextProfileRegistry;
            if (profiles == null) {
               throw new IllegalStateException("Context profile registry is unavailable");
            }
            originalContextProfiles.set(profiles.toJson().deepCopy());
            profiles.clearRules();
            if (!profiles.setEnabled(true) || !profiles.isEnabled()) {
               throw new IllegalStateException("Context profiles did not enable in an initialized client");
            }
         });
         context.waitTicks(2);
         context.runOnClient(minecraft -> {
            var profiles = ClientContext.contextProfileRegistry;
            if ("No context profile has been applied yet".equals(profiles.getCurrentContextDescription())) {
               throw new IllegalStateException("Context profiles did not observe the active client context");
            }
            if (!profiles.previewCurrentRules().isEmpty()) {
               throw new IllegalStateException("Cleared context profile registry unexpectedly selected rules");
            }
            if (!profiles.setEnabled(false) || profiles.isEnabled()) {
               throw new IllegalStateException("Context profiles did not restore cleanly on disable");
            }
            com.google.gson.JsonElement original = originalContextProfiles.get();
            if (original == null) {
               throw new IllegalStateException("Context profile test did not capture the original configuration");
            }
            profiles.fromJson(original.deepCopy());
            if (!profiles.toJson().equals(original)) {
               throw new IllegalStateException("Context profile configuration was not restored after lifecycle test");
            }
         });
         context.waitTicks(1);

         context.runOnClient(minecraft -> verifyScissorOwnership());

         context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(3);
         context.takeScreenshot("schizo-clickgui");

         context.runOnClient(minecraft -> {
            ClickGuiScreen studio = (ClickGuiScreen)minecraft.screen;
            if (studio.getSelectedPanelName().isBlank() || studio.getPanels().size() < 7) {
               throw new IllegalStateException("Studio categories are unavailable");
            }
            var panel = studio.getPanels().getFirst();
            double scale = dev.schizo.client.render.GuiScaleUtils.calculateScaleMultiplier(studio.getGuiScale());
            // Second navigation row. The workspace bounds are already established by the preceding render.
            var categoryClick = new MouseButtonEvent((panel.getX() - 16) * scale,
               (panel.getY() + 36) * scale, new MouseButtonInfo(0, 0));
            studio.mouseClicked(categoryClick, false);
            studio.mouseReleased(categoryClick);
            if (!studio.getSelectedPanelName().equals(studio.getPanels().get(1).getName())) {
               throw new IllegalStateException("Studio sidebar did not select the second category");
            }
            var searchClick = new MouseButtonEvent((panel.getX() + 20) * scale,
               (panel.getY() - 20) * scale, new MouseButtonInfo(0, 0));
            studio.mouseClicked(searchClick, false);
            studio.mouseReleased(searchClick);
            if (!studio.getSearchBox().isFocused()) {
               throw new IllegalStateException("Studio search did not focus on click");
            }
            studio.getSearchBox().setValue("Crosshair");
            if (!"Render".equalsIgnoreCase(studio.getSelectedPanelName())) {
               throw new IllegalStateException("Studio search failed to select the matching category");
            }
            studio.getSearchBox().setValue("crossahir");
            if (!"Render".equalsIgnoreCase(studio.getSelectedPanelName())) {
               throw new IllegalStateException("Fuzzy search did not preserve the matching category");
            }
         });
         context.waitTicks(3);
         context.runOnClient(minecraft -> {
            ClickGuiScreen studio = (ClickGuiScreen)minecraft.screen;
            var panel = studio.getPanels().stream()
               .filter(p -> p.getName().equals(studio.getSelectedPanelName())).findFirst().orElseThrow();
            var component = panel.children.stream()
               .filter(c -> c instanceof dev.schizo.client.gui.components.ModuleComponent module
                  && module.getModule().getName().equalsIgnoreCase("Crosshair"))
               .map(c -> (dev.schizo.client.gui.components.ModuleComponent)c).findFirst().orElseThrow();
            double x = panel.getX() + 12;
            double y = panel.getY() + component.getYOffset() + 5;
            if (panel.findVisibleModuleAt(x, y) != component) {
               throw new IllegalStateException("Inspector target is not the visible searched module");
            }
            double scale = dev.schizo.client.render.GuiScaleUtils.calculateScaleMultiplier(studio.getGuiScale());
            var click = new MouseButtonEvent(x * scale, y * scale, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0));
            studio.mouseClicked(click, false);
            studio.mouseReleased(click);
            if (!component.isOpen()) throw new IllegalStateException("Module settings did not open");
         });
         context.waitTicks(10);
         context.takeScreenshot("schizo-studio-controls");
         context.runOnClient(minecraft -> {
            ClickGuiScreen studio = (ClickGuiScreen)minecraft.screen;
            var panel = studio.getPanels().stream()
               .filter(p -> p.getName().equals(studio.getSelectedPanelName())).findFirst().orElseThrow();
            var component = panel.children.stream()
               .filter(c -> c instanceof dev.schizo.client.gui.components.ModuleComponent module
                  && module.getModule().getName().equalsIgnoreCase("Crosshair"))
               .map(c -> (dev.schizo.client.gui.components.ModuleComponent)c).findFirst().orElseThrow();
            double x = panel.getX() + 12;
            double y = panel.getY() + component.getYOffset() + 5;
            double scale = dev.schizo.client.render.GuiScaleUtils.calculateScaleMultiplier(studio.getGuiScale());
            var click = new MouseButtonEvent(x * scale, y * scale, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, 0));
            studio.mouseClicked(click, false);
            if (!(minecraft.screen instanceof dev.schizo.client.gui.screens.ModuleInspectorScreen)) {
               throw new IllegalStateException("Middle click failed to open inspector");
            }
         });
         context.waitForScreen(dev.schizo.client.gui.screens.ModuleInspectorScreen.class);
         context.waitTicks(2);
         context.runOnClient(minecraft -> {
            net.minecraft.client.gui.components.EditBox filter = null;
            for (var child : minecraft.screen.children()) {
               if (child instanceof net.minecraft.client.gui.components.EditBox box
                  && box.getMessage().getString().equals("Filter settings")) {
                  filter = box;
                  break;
               }
            }
            if (filter == null) throw new IllegalStateException("Inspector filter missing");
            var event = new MouseButtonEvent(filter.getX() + 4, filter.getY() + 4, new MouseButtonInfo(0, 0));
            minecraft.screen.mouseClicked(event, false);
            minecraft.screen.mouseReleased(event);
            if (!filter.isFocused()) throw new IllegalStateException("Inspector filter did not receive focus");
            filter.setValue("no-such-setting-schizo-qa");
            boolean foundDisabledApply = false;
            for (var child : minecraft.screen.children()) {
               if (child instanceof Button button && button.getMessage().getString().equals("Apply value")) {
                  foundDisabledApply = !button.active;
               }
            }
            if (!foundDisabledApply) throw new IllegalStateException("Empty setting filter must disable apply");
            filter.setValue("");
         });
         context.waitTicks(8);
         context.takeScreenshot("schizo-studio-inspector");
         context.runOnClient(minecraft -> clickButton(minecraft.screen, "Back"));
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(3);
         context.runOnClient(minecraft -> ((ClickGuiScreen)minecraft.screen).getSearchBox().setValue(""));
         context.waitTicks(2);
         // Exercise the actual dock buttons and return path, rather than creating destinations directly.
         Class<?>[] destinations = {HudEditorScreen.class, PerformanceCenterScreen.class,
            ExpeditionScreen.class, HealthCenterScreen.class, dev.schizo.client.gui.screens.ThemeStudioScreen.class,
            dev.schizo.client.gui.screens.QuickAccessScreen.class};
         for (int index = 0; index < destinations.length; index++) {
            final int tool = index;
            context.runOnClient(minecraft -> {
               ClickGuiScreen studio = (ClickGuiScreen)minecraft.screen;
               double scale = dev.schizo.client.render.GuiScaleUtils.calculateScaleMultiplier(studio.getGuiScale());
               var event = new MouseButtonEvent(studio.studioToolX(tool) * scale, studio.studioToolY() * scale,
                  new MouseButtonInfo(0, 0));
               studio.mouseClicked(event, false);
               if (!destinations[tool].isInstance(minecraft.screen)) {
                  throw new IllegalStateException("Studio tool failed to open: " + tool);
               }
            });
            context.waitTicks(3);
            if (tool == 3) context.takeScreenshot("schizo-health-center");
            if (tool == 4) {
               context.runOnClient(minecraft -> {
                  var ui = dev.schizo.client.modules.client.UIModule.INSTANCE;
                  var before = ui.snapshotPalette();
                  var choice = ui.paletteSetting.getValue();
                  clickButton(minecraft.screen, "Midnight");
                  clickButton(minecraft.screen, "Test switch");
                  if (!before.equals(ui.snapshotPalette()) || choice != ui.paletteSetting.getValue()) {
                     throw new IllegalStateException("Theme preview mutated live settings");
                  }
                  clickButton(minecraft.screen, "Discard preview");
                  if (!before.equals(ui.snapshotPalette()) || choice != ui.paletteSetting.getValue()) {
                     throw new IllegalStateException("Discarding preview mutated live settings");
                  }
                  clickButton(minecraft.screen, "Edit colors");
                  net.minecraft.client.gui.components.EditBox accent = null;
                  for (var child : minecraft.screen.children()) {
                     if (child instanceof net.minecraft.client.gui.components.EditBox box
                        && box.getMessage().getString().equals("Accent")) accent = box;
                  }
                  if (accent == null) throw new IllegalStateException("Custom accent input missing");
                  accent.setValue("#123");
                  boolean invalidDisabled = false;
                  for (var child : minecraft.screen.children()) {
                     if (child instanceof Button button && button.getMessage().getString().equals("Preview colors"))
                        invalidDisabled = !button.active;
                  }
                  if (!invalidDisabled) throw new IllegalStateException("Invalid hex must disable preview");
                  accent.setValue("#FF9900FF");
                  clickButton(minecraft.screen, "Preview colors");
                  if (!before.equals(ui.snapshotPalette()) || choice != ui.paletteSetting.getValue())
                     throw new IllegalStateException("Custom preview mutated live settings");
                  clickButton(minecraft.screen, "Discard preview");
                  if (!before.equals(ui.snapshotPalette()) || choice != ui.paletteSetting.getValue())
                     throw new IllegalStateException("Custom discard mutated live settings");
               });
               context.waitTicks(2);
            }
            context.runOnClient(minecraft -> {
               if (minecraft.screen instanceof dev.schizo.client.gui.screens.ClientScreen child) {
                  child.closeWithTransition();
               } else {
                  minecraft.screen.onClose();
               }
            });
            context.waitForScreen(ClickGuiScreen.class);
            context.waitTicks(3);
         }

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
         context.runOnClient(minecraft -> {
            for (var child : minecraft.screen.children()) {
               if (child instanceof net.minecraft.client.gui.components.EditBox box) {
                  box.setValue("crosshair length");
                  break;
               }
            }
         });
         context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
         context.waitForScreen(dev.schizo.client.gui.screens.ModuleInspectorScreen.class);
         context.runOnClient(minecraft -> {
            var inspector = (dev.schizo.client.gui.screens.ModuleInspectorScreen)minecraft.screen;
            var target = ClientContext.getModule(dev.schizo.client.modules.render.CrosshairModule.class).getSettings().stream()
               .filter(setting -> setting.getName().equalsIgnoreCase("Length")).findFirst().orElseThrow();
            if (inspector.selectedSetting() != target)
               throw new IllegalStateException("Search did not reveal the exact registered setting");
            if (!dev.schizo.client.gui.screens.RecentSettingsScreen.wasVisited(
               ClientContext.getModule(dev.schizo.client.modules.render.CrosshairModule.class), target))
               throw new IllegalStateException("Explicit setting navigation was not recorded");
            inspector.onClose();
         });
         context.waitForScreen(CommandPaletteScreen.class);
         context.runOnClient(minecraft -> {
            boolean retained = false;
            for (var child : minecraft.screen.children()) {
               if (child instanceof net.minecraft.client.gui.components.EditBox box)
                  retained |= box.getValue().equals("crosshair length");
            }
            if (!retained) throw new IllegalStateException("Command palette lost query on return");
         });
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
            if ("Profiles".equals(tab)) {
               context.runOnClient(minecraft -> clickButton(minecraft.screen, "Edit rules"));
               context.waitForScreen(dev.schizo.client.gui.screens.ContextProfileEditorScreen.class);
               context.waitTicks(2);
               context.runOnClient(minecraft -> {
                  var before = ClientContext.contextProfileRegistry.getRules();
                  clickButton(minecraft.screen, "New rule");
                  clickButton(minecraft.screen, "Preview save");
                  if (!(minecraft.screen instanceof dev.schizo.client.gui.screens.ContextProfileEditorScreen)
                     || !before.equals(ClientContext.contextProfileRegistry.getRules())) {
                     throw new IllegalStateException("Invalid editor draft changed the rule list");
                  }
                  clickButton(minecraft.screen, "Back");
               });
               context.waitForScreen(ExpeditionScreen.class);
               context.waitTicks(2);
            }
         }
         context.setScreen(() -> null);
         context.waitForScreen(null);

         var layoutBefore = new AtomicReference<com.google.gson.JsonObject>();
         context.runOnClient(minecraft -> {
            var layout = ClientContext.getHudEditor().exportLayoutPreset();
            layoutBefore.set(layout.deepCopy());
            var widgets = layout.getAsJsonArray("widgets");
            if (widgets.isEmpty()) {
               throw new IllegalStateException("No widgets available for preview regression");
            }

            com.google.gson.JsonObject widget = null;
            for (Object candidate : ClientContext.moduleRegistry.getRegistry()) {
               if (candidate instanceof dev.schizo.client.modules.HudWidgetModule hud
                  && hud.getRenderer() instanceof dev.schizo.client.render.ResizableHudWidgetRenderer) {
                  for (var entry : widgets) {
                     var value = entry.getAsJsonObject();
                     if (value.get("module").getAsString().equals(hud.getConfigName())) {
                        widget = value;
                        break;
                     }
                  }
               }
               if (widget != null) {
                  break;
               }
            }
            if (widget == null) {
               throw new IllegalStateException("No resizable widget available for preview regression");
            }

            widget.addProperty("scale", widget.get("scale").getAsFloat() < 1.5F ? 1.5F : 0.75F);
            minecraft.setScreen(new HudLayoutPreviewScreen(layout));
            if (layoutBefore.get().equals(ClientContext.getHudEditor().exportLayoutPreset())) {
               throw new IllegalStateException("A changed layout preview was not applied");
            }
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
            minecraft.setScreen(new HudLayoutPreviewScreen(new com.google.gson.JsonObject()));
            if (!layoutBefore.get().equals(ClientContext.getHudEditor().exportLayoutPreset())) {
               throw new IllegalStateException("Malformed preview changed the original layout");
            }
            for (var child : minecraft.screen.children()) {
               if (child instanceof Button button
                  && button.getMessage().getString().equals("Keep layout")
                  && button.active) {
                  throw new IllegalStateException("Malformed preview can be committed");
               }
            }
         });
         context.setScreen(() -> null);
         context.waitForScreen(null);
      }

      context.waitTicks(3);

      try (TestSingleplayerContext secondSingleplayer = GameTestWorlds.create(context)) {
         secondSingleplayer.getClientWorld().waitForChunksRender();
         context.waitTicks(5);

         context.runOnClient(minecraft -> {
            if (minecraft.level == null || minecraft.player == null) {
               throw new IllegalStateException("Second singleplayer world did not initialize");
            }
            if (minecraft.level == firstLevel.get()) {
               throw new IllegalStateException("Client level instance was not replaced across world recreation");
            }
            if (minecraft.player == firstPlayer.get()) {
               throw new IllegalStateException("Local player instance was not replaced across world recreation");
            }
         });

         context.setScreen(HudEditorScreen::new);
         context.waitForScreen(HudEditorScreen.class);
         context.waitTicks(3);
         context.takeScreenshot("schizo-hud-editor-reconnect");
         context.setScreen(() -> null);
         context.waitForScreen(null);
      }

      long failuresAfter = EventFailureTracker.GLOBAL.totalFailures();
      if (failuresAfter != failuresBefore) {
         throw new IllegalStateException(
            "Schizo event callback failures increased during GUI/world lifecycle gametest: "
               + failuresBefore + " -> " + failuresAfter
         );
      }
   }

   private static void clickButton(Screen screen, String label) {
      for (var child : screen.children()) {
         if (child instanceof Button button && button.getMessage().getString().equals(label)) {
            var event = new MouseButtonEvent(
               button.getX() + 2,
               button.getY() + 2,
               new MouseButtonInfo(0, 0)
            );
            if (!screen.mouseClicked(event, false)) {
               throw new IllegalStateException("Button ignored click: " + label);
            }
            screen.mouseReleased(event);
            return;
         }
      }
      throw new IllegalStateException("Missing button: " + label);
   }

   private static void verifyScissorOwnership() {
      var state = com.mojang.blaze3d.systems.RenderSystem.getScissorStateForRenderTypeDraws();
      boolean enabled = state.enabled();
      int x = state.x(), y = state.y(), width = state.width(), height = state.height();
      try {
         state.enable(7, 9, 31, 37);
         dev.schizo.client.gui.ScissorStack.push(0, 0, 16, 16);
         try {
            int innerX = state.x(), innerY = state.y(), innerW = state.width(), innerH = state.height();
            dev.schizo.client.gui.ScissorStack.pushIntersection(4, 4, 4, 4);
            try {
               if (!state.enabled() || state.width() > innerW || state.height() > innerH) {
                  throw new IllegalStateException("Nested scissor escaped parent");
               }
            } finally {
               dev.schizo.client.gui.ScissorStack.pop();
            }
            if (!state.enabled() || state.x() != innerX || state.y() != innerY
               || state.width() != innerW || state.height() != innerH) {
               throw new IllegalStateException("Nested scissor did not restore its predecessor");
            }
         } finally {
            dev.schizo.client.gui.ScissorStack.pop();
         }
         if (!state.enabled() || state.x() != 7 || state.y() != 9
            || state.width() != 31 || state.height() != 37) {
            throw new IllegalStateException("Schizo scissor did not restore external clipping");
         }
      } finally {
         if (enabled) state.enable(x, y, width, height);
         else state.disable();
      }
   }

}
