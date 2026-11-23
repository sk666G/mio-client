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
import dev.schizo.client.ClientContext;
import dev.schizo.client.commands.Command;
import dev.schizo.client.gui.components.ModuleComponent;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Global fuzzy-search launcher for modules, commands and settings. */
public final class CommandPaletteScreen extends ClientScreen {
   private static final int MAX_RESULTS = 40;
   private static final int VISIBLE_RESULTS = 8;
   private static final int RESULT_ROW_HEIGHT = 22;
   private static final int PANEL_WIDTH = 390;

   private enum EntryKind {
      MODULE,
      COMMAND,
      SETTING
   }

   private record PaletteEntry(
      EntryKind kind,
      String title,
      String subtitle,
      Module module,
      Command command
   ) {
   }

   private record PaletteResult(PaletteEntry entry, int score) {
   }

   private final List<PaletteEntry> index = new ArrayList<>();
   private final List<PaletteResult> results = new ArrayList<>();
   private EditBox searchBox;
   private int selectedIndex;
   private int firstVisibleIndex;
   private float panelLeft;
   private float panelTop;
   private float resultsTop;
   private float panelWidth;

   public static void openPalette() {
      if (ClientAccess.schizoMinecraft.screen == null) {
         new CommandPaletteScreen().open();
      }
   }

   private void rebuildIndex() {
      this.index.clear();

      for (Object moduleElement : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)moduleElement;
         this.index.add(new PaletteEntry(
            EntryKind.MODULE,
            module.getName(),
            module.isToggled() ? "Enabled module" : "Module",
            module,
            null
         ));

         for (Setting<?> setting : module.getSettings()) {
            if (setting.isGroupOnly()) {
               continue;
            }
            this.index.add(new PaletteEntry(
               EntryKind.SETTING,
               setting.getName(),
               module.getName() + " setting",
               module,
               null
            ));
         }
      }

      if (ClientContext.commandRegistry != null) {
         for (Object commandElement : ClientContext.commandRegistry.getRegistry()) {
            Command command = (Command)commandElement;
            String description = command.getDescription();
            this.index.add(new PaletteEntry(
               EntryKind.COMMAND,
               command.getName(),
               description.isBlank() ? "Command" : description,
               null,
               command
            ));
         }
      }
   }

   private void updateResults() {
      String query = this.searchBox == null
         ? ""
         : this.searchBox.getValue().strip().toLowerCase(Locale.ROOT);

      this.results.clear();
      for (PaletteEntry entry : this.index) {
         int score = this.matchScore(entry, query);
         if (score >= 0) {
            this.results.add(new PaletteResult(entry, score));
         }
      }

      this.results.sort(
         Comparator.comparingInt(PaletteResult::score)
            .thenComparing(result -> result.entry().kind().ordinal())
            .thenComparing(result -> result.entry().title(), String.CASE_INSENSITIVE_ORDER)
      );
      if (this.results.size() > MAX_RESULTS) {
         this.results.subList(MAX_RESULTS, this.results.size()).clear();
      }

      this.selectedIndex = 0;
      this.firstVisibleIndex = 0;
   }

   private int matchScore(PaletteEntry entry, String query) {
      if (query.isEmpty()) {
         return switch (entry.kind()) {
            case MODULE -> 0;
            case COMMAND -> 1;
            case SETTING -> 2;
         };
      }

      String title = entry.title().toLowerCase(Locale.ROOT);
      String subtitle = entry.subtitle().toLowerCase(Locale.ROOT);
      if (title.equals(query)) {
         return 0;
      }
      if (title.startsWith(query)) {
         return 1;
      }
      if (title.contains(query)) {
         return 2;
      }
      if (subtitle.contains(query)) {
         return 3;
      }
      if (isSubsequence(query, title)) {
         return 4;
      }
      return -1;
   }

   private static boolean isSubsequence(String query, String candidate) {
      int queryIndex = 0;
      for (int index = 0; index < candidate.length() && queryIndex < query.length(); index++) {
         if (candidate.charAt(index) == query.charAt(queryIndex)) {
            queryIndex++;
         }
      }
      return queryIndex == query.length();
   }

   private void keepSelectionVisible() {
      if (this.results.isEmpty()) {
         this.selectedIndex = 0;
         this.firstVisibleIndex = 0;
         return;
      }

      this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, this.results.size() - 1));
      if (this.selectedIndex < this.firstVisibleIndex) {
         this.firstVisibleIndex = this.selectedIndex;
      } else if (this.selectedIndex >= this.firstVisibleIndex + VISIBLE_RESULTS) {
         this.firstVisibleIndex = this.selectedIndex - VISIBLE_RESULTS + 1;
      }
      this.firstVisibleIndex = Math.max(
         0,
         Math.min(this.firstVisibleIndex, Math.max(0, this.results.size() - VISIBLE_RESULTS))
      );
   }

   private void performSelected() {
      if (this.results.isEmpty()) {
         return;
      }

      PaletteEntry entry = this.results.get(this.selectedIndex).entry();
      switch (entry.kind()) {
         case MODULE -> {
            entry.module().toggle();
            ClientAccess.schizoMinecraft.setScreen(null);
         }
         case COMMAND -> ClientAccess.schizoMinecraft.setScreen(
            new ChatScreen(CommandRegistry.getPrefix() + entry.command().getName() + " ", false)
         );
         case SETTING -> this.openModuleSettings(entry.module());
      }
   }

   private void openModuleSettings(Module module) {
      ClickGuiScreen clickGui = ClientContext.getClickGui();
      clickGui.open();
      EditBox clickGuiSearch = clickGui.getSearchBox();
      if (clickGuiSearch != null) {
         clickGuiSearch.setValue(module.getName());
      }

      for (Panel panel : clickGui.getPanels()) {
         for (Object child : panel.getChildren()) {
            if (child instanceof ModuleComponent component && component.getModule() == module) {
               if (component.isClosed()) {
                  component.toggleOpen();
               }
               component.startSearchHighlight();
               panel.updateLayout();
               return;
            }
         }
      }
   }

   @Override
   protected void init() {
      super.init();
      this.rebuildIndex();
      this.searchBox = new EditBox(
         ClientAccess.schizoMinecraft.font,
         16,
         16,
         320,
         18,
         Component.literal("Search modules, commands and settings")
      );
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue("");
      this.searchBox.setResponder(ignored -> this.updateResults());
      this.addRenderableWidget(this.searchBox);
      this.setInitialFocus(this.searchBox);
      this.updateResults();
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      switch (event.key()) {
         case GLFW.GLFW_KEY_ESCAPE -> {
            ClientAccess.schizoMinecraft.setScreen(null);
            return true;
         }
         case GLFW.GLFW_KEY_UP -> {
            if (!this.results.isEmpty()) {
               this.selectedIndex--;
               this.keepSelectionVisible();
            }
            return true;
         }
         case GLFW.GLFW_KEY_DOWN -> {
            if (!this.results.isEmpty()) {
               this.selectedIndex++;
               this.keepSelectionVisible();
            }
            return true;
         }
         case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
            this.performSelected();
            return true;
         }
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount
   ) {
      if (verticalAmount != 0.0D && !this.results.isEmpty()) {
         this.selectedIndex += verticalAmount < 0.0D ? 1 : -1;
         this.keepSelectionVisible();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0 || this.results.isEmpty()) {
         return;
      }

      float relativeY = (float)mouseY - this.resultsTop;
      int row = (int)(relativeY / RESULT_ROW_HEIGHT);
      if (relativeY >= 0.0F && row >= 0 && row < VISIBLE_RESULTS) {
         int resultIndex = this.firstVisibleIndex + row;
         if (resultIndex < this.results.size()) {
            this.selectedIndex = resultIndex;
            this.performSelected();
         }
      }
   }

   @Override
   public void renderBackgroundOverlay(PoseStack poseStack) {
      GuiDrawUtils.fillRect(
         poseStack,
         0.0F,
         0.0F,
         GuiScaleUtils.getScaledWidth(),
         GuiScaleUtils.getScaledHeight(),
         new Color(5, 8, 10, 170)
      );
   }

   @Override
   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
      int screenWidth = GuiScaleUtils.getScaledWidth();
      int screenHeight = GuiScaleUtils.getScaledHeight();
      this.panelWidth = Math.min(PANEL_WIDTH, screenWidth - 24.0F);
      this.panelLeft = (screenWidth - this.panelWidth) / 2.0F;
      this.panelTop = Math.max(18.0F, screenHeight * 0.22F);
      this.resultsTop = this.panelTop + 62.0F;

      int resultCount = Math.min(VISIBLE_RESULTS, Math.max(1, this.results.size()));
      float panelHeight = 62.0F + resultCount * RESULT_ROW_HEIGHT + 30.0F;
      Color surface = UIModule.INSTANCE.backgroundSetting.getValue();
      Color accent = UIModule.INSTANCE.colorSetting.getValue();

      GuiDrawUtils.fillRect(
         poseStack,
         this.panelLeft,
         this.panelTop,
         this.panelLeft + this.panelWidth,
         this.panelTop + panelHeight,
         surface
      );
      GuiDrawUtils.drawRectOutline(
         poseStack,
         this.panelLeft,
         this.panelTop,
         this.panelLeft + this.panelWidth,
         this.panelTop + panelHeight,
         ColorUtils.withAlpha(accent, 190)
      );

      FontRenderer font = FontRenderer.INSTANCE;
      font.drawWithShadow(
         poseStack,
         "SCHIZO / COMMAND",
         this.panelLeft + 14.0F,
         this.panelTop + 10.0F,
         accent
      );

      this.searchBox.setX((int)this.panelLeft + 12);
      this.searchBox.setY((int)this.panelTop + 32);
      font.drawWithShadow(
         poseStack,
         this.searchBox.getValue().isEmpty() ? "Search modules, commands, settings..." : this.searchBox.getValue(),
         this.panelLeft + 14.0F,
         this.panelTop + 34.0F,
         this.searchBox.getValue().isEmpty() ? Color.GRAY : Color.WHITE
      );
      GuiDrawUtils.fillRect(
         poseStack,
         this.panelLeft + 12.0F,
         this.panelTop + 53.0F,
         this.panelLeft + this.panelWidth - 12.0F,
         this.panelTop + 53.5F,
         ColorUtils.withAlpha(accent, 110)
      );

      this.keepSelectionVisible();
      for (int row = 0; row < VISIBLE_RESULTS; row++) {
         int resultIndex = this.firstVisibleIndex + row;
         if (resultIndex >= this.results.size()) {
            break;
         }

         PaletteEntry entry = this.results.get(resultIndex).entry();
         float rowTop = this.resultsTop + row * RESULT_ROW_HEIGHT;
         if (resultIndex == this.selectedIndex) {
            GuiDrawUtils.fillRect(
               poseStack,
               this.panelLeft + 8.0F,
               rowTop,
               this.panelLeft + this.panelWidth - 8.0F,
               rowTop + RESULT_ROW_HEIGHT,
               ColorUtils.withAlpha(accent, 48)
            );
         }

         String kindLabel = switch (entry.kind()) {
            case MODULE -> "MODULE";
            case COMMAND -> "COMMAND";
            case SETTING -> "SETTING";
         };
         font.drawWithShadow(
            poseStack,
            kindLabel,
            this.panelLeft + 14.0F,
            rowTop + 3.0F,
            ColorUtils.withAlpha(accent, 220)
         );
         font.drawWithShadow(
            poseStack,
            entry.title(),
            this.panelLeft + 88.0F,
            rowTop + 3.0F,
            Color.WHITE
         );
         font.drawWithShadow(
            poseStack,
            entry.subtitle(),
            this.panelLeft + 88.0F,
            rowTop + 12.0F,
            new Color(155, 165, 170)
         );
      }

      float footerY = this.resultsTop + resultCount * RESULT_ROW_HEIGHT + 7.0F;
      String footer = "↑ ↓ Select    Enter Open    Esc Close";
      font.drawWithShadow(
         poseStack,
         footer,
         this.panelLeft + 14.0F,
         footerY,
         Color.GRAY
      );
   }
}
