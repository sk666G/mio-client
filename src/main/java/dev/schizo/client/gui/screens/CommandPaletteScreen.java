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
import dev.schizo.client.managers.ModulePreferencesRegistry;
import dev.schizo.client.config.ModuleQuickAccessRanking;
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

   private enum EntryKind {
      MODULE,
      COMMAND,
      SETTING,
      KEYBIND
   }

   private record PaletteEntry(
      EntryKind kind,
      String title,
      String subtitle,
      String searchContext,
      Module module,
      Setting<?> setting,
      Command command
   ) {
   }

   private record PaletteResult(PaletteEntry entry, int score, ModuleQuickAccessRanking.Rank quickAccessRank) {
   }

   private final List<PaletteEntry> index = new ArrayList<>();
   private final List<PaletteResult> results = new ArrayList<>();
   private EditBox searchBox;
   private String normalizedQuery = "";
   private int selectedIndex;
   private int firstVisibleIndex;
   private float panelLeft;
   private float panelTop;
   private float resultsTop;
   private float panelWidth;
   private int visibleResults = CommandPaletteLayout.MAX_VISIBLE_RESULTS;

   public static void openPalette() {
      if (ClientAccess.schizoMinecraft.screen == null) {
         new CommandPaletteScreen().open();
      }
   }

   private void rebuildIndex() {
      this.index.clear();
      ModulePreferencesRegistry preferences = ClientContext.modulePreferencesRegistry;

      for (Object moduleElement : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)moduleElement;
         int favoriteIndex = preferences == null ? -1 : preferences.getFavoriteIndex(module.getConfigName());
         int recentIndex = preferences == null ? -1 : preferences.getRecentIndex(module.getConfigName());
         String moduleSubtitle = favoriteIndex >= 0
            ? "★ Favorite module"
            : recentIndex >= 0
               ? "Recently enabled · #" + (recentIndex + 1)
               : module.isToggled() ? "Enabled module" : "Module";
         this.index.add(new PaletteEntry(
            EntryKind.MODULE,
            module.getName(),
            moduleSubtitle,
            moduleSubtitle + " " + String.join(" ", module.getAliases()) + " " + module.getDescription(),
            module,
            null,
            null
         ));

         for (Setting<?> setting : module.getSettings()) {
            if (setting.isGroupOnly() || setting.isExcludedFromConfig()) {
               continue;
            }
            this.index.add(new PaletteEntry(
               EntryKind.SETTING,
               setting.getName(),
               module.getName() + " setting",
               module.getName() + " setting " + setting.getConfigName() + " " + String.join(" ", module.getAliases()),
               module,
               setting,
               null
            ));
         }

         if (!module.getKeybind().isUnbound()) {
            this.index.add(new PaletteEntry(
               EntryKind.KEYBIND,
               module.getKeybind().getDisplayName() + " - " + module.getName(),
               "Keybind for " + module.getName(),
               "Keybind for " + module.getName(),
               module,
               null,
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
               description,
               null,
               null,
               command
            ));
         }
      }
   }

   private void updateResults() {
      String query = this.searchBox == null
         ? ""
         : PaletteMatcher.normalizeQuery(this.searchBox.getValue());

      this.normalizedQuery = query;
      this.results.clear();
      ModulePreferencesRegistry preferences = ClientContext.modulePreferencesRegistry;
      var quickAccessRanks = preferences == null
         ? ModuleQuickAccessRanking.emptySnapshot()
         : ModuleQuickAccessRanking.snapshot(preferences.getFavorites(), preferences.getRecentlyUsed());
      for (PaletteEntry entry : this.index) {
         int score = PaletteMatcher.scoreNormalized(entry.title(), entry.searchContext(), query);
         if (score >= 0) {
            ModuleQuickAccessRanking.Rank rank = switch (entry.kind()) {
               case MODULE -> quickAccessRanks.rank(entry.module().getConfigName());
               case COMMAND -> new ModuleQuickAccessRanking.Rank(3, Integer.MAX_VALUE);
               case SETTING -> new ModuleQuickAccessRanking.Rank(4, Integer.MAX_VALUE);
               case KEYBIND -> new ModuleQuickAccessRanking.Rank(5, Integer.MAX_VALUE);
            };
            this.results.add(new PaletteResult(entry, score, rank));
         }
      }

      this.results.sort(
         Comparator.comparingInt(PaletteResult::score)
            .thenComparing(PaletteResult::quickAccessRank)
            .thenComparing(result -> result.entry().kind().ordinal())
            .thenComparing(result -> result.entry().title(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(result -> result.entry().subtitle(), String.CASE_INSENSITIVE_ORDER)
      );
      if (this.results.size() > MAX_RESULTS) {
         this.results.subList(MAX_RESULTS, this.results.size()).clear();
      }

      this.selectedIndex = 0;
      this.firstVisibleIndex = 0;
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
      } else if (this.selectedIndex >= this.firstVisibleIndex + this.visibleResults) {
         this.firstVisibleIndex = this.selectedIndex - this.visibleResults + 1;
      }
      this.firstVisibleIndex = Math.max(
         0,
         Math.min(this.firstVisibleIndex, Math.max(0, this.results.size() - this.visibleResults))
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
         case SETTING -> ClientAccess.schizoMinecraft.setScreen(
            new ModuleInspectorScreen(this, entry.module(), entry.setting())
         );
         case KEYBIND -> this.openModuleSettings(entry.module());
      }
   }

   private void openModuleSettings(Module module) {
      ClickGuiScreen clickGui = ClientContext.getClickGui();
      clickGui.open();
      clickGui.revealModule(module);
   }

   @Override
   protected void init() {
      String previousQuery = this.searchBox == null ? "" : this.searchBox.getValue();
      super.init();
      this.rebuildIndex();
      int screenWidth = GuiScaleUtils.getScaledWidth();
      int screenHeight = GuiScaleUtils.getScaledHeight();
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(screenWidth, screenHeight);
      int width = Math.round(layout.panelWidth());
      int left = Math.round(layout.panelLeft());
      int top = Math.round(layout.panelTop());
      this.searchBox = new EditBox(
         ClientAccess.schizoMinecraft.font,
         left + 12,
         top + 32,
         Math.max(1, width - 24),
         18,
         Component.literal("Search modules, commands and settings")
      );
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue(previousQuery);
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
      int row = (int)(relativeY / CommandPaletteLayout.RESULT_ROW_HEIGHT);
      boolean insidePanel = mouseX >= this.panelLeft
         && mouseX <= this.panelLeft + this.panelWidth;
      if (insidePanel && relativeY >= 0.0F && row >= 0 && row < this.visibleResults) {
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
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(screenWidth, screenHeight);
      this.panelWidth = layout.panelWidth();
      this.panelLeft = layout.panelLeft();
      this.panelTop = layout.panelTop();
      this.visibleResults = layout.visibleRows();
      this.resultsTop = this.panelTop + CommandPaletteLayout.HEADER_HEIGHT;

      int resultCount = Math.min(this.visibleResults, Math.max(1, this.results.size()));
      float panelHeight = CommandPaletteLayout.HEADER_HEIGHT
         + resultCount * CommandPaletteLayout.RESULT_ROW_HEIGHT
         + CommandPaletteLayout.FOOTER_HEIGHT;
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

      String searchValue = this.searchBox.getValue();
      String searchDisplay = searchValue.isEmpty()
         ? "Search modules, commands, settings..."
         : searchValue + (this.searchBox.isFocused() ? "|" : "");
      font.drawWithShadow(
         poseStack,
         SearchFeedback.fit(searchDisplay, this.panelWidth - 28, font::getWidth),
         this.panelLeft + 14.0F,
         this.panelTop + 34.0F,
         searchValue.isEmpty() ? Color.GRAY : Color.WHITE
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
      if (this.results.isEmpty()) {
         font.drawWithShadow(
            poseStack,
            "No matching actions",
            this.panelLeft + 14.0F,
            this.resultsTop + 4.0F,
            Color.GRAY
         );
      }
      for (int row = 0; row < this.visibleResults; row++) {
         int resultIndex = this.firstVisibleIndex + row;
         if (resultIndex >= this.results.size()) {
            break;
         }

         PaletteResult result = this.results.get(resultIndex);
         PaletteEntry entry = result.entry();
         float rowTop = this.resultsTop + row * CommandPaletteLayout.RESULT_ROW_HEIGHT;
         if (resultIndex == this.selectedIndex) {
            GuiDrawUtils.fillRect(
               poseStack,
               this.panelLeft + 8.0F,
               rowTop,
               this.panelLeft + this.panelWidth - 8.0F,
               rowTop + CommandPaletteLayout.RESULT_ROW_HEIGHT,
               ColorUtils.withAlpha(accent, 48)
            );
         }

         String kindLabel = switch (entry.kind()) {
            case MODULE -> "MODULE";
            case COMMAND -> "COMMAND";
            case SETTING -> "SETTING";
            case KEYBIND -> "KEYBIND";
         };
         font.drawWithShadow(
            poseStack,
            SearchFeedback.fit(kindLabel, Math.min(68, this.panelWidth - 28), font::getWidth),
            this.panelLeft + 14.0F,
            rowTop + 3.0F,
            ColorUtils.withAlpha(accent, 220)
         );
         float textX = this.panelLeft + 88.0F;
         float textWidth = Math.max(0, this.panelWidth - 102.0F);
         String title = SearchFeedback.fit(entry.title(), textWidth, font::getWidth);
         String hint = SearchFeedback.hint(result.score(), this.normalizedQuery);
         String subtitle = SearchFeedback.fit(hint.isEmpty() ? entry.subtitle() : hint + " | " + entry.subtitle(), textWidth, font::getWidth);
         font.drawWithShadow(poseStack, title, textX, rowTop + 3.0F, Color.WHITE);
         font.drawWithShadow(poseStack, subtitle, textX, rowTop + 12.0F, new Color(155, 165, 170));
         SearchFeedback.Span span = SearchFeedback.literalSpan(entry.title(), this.normalizedQuery);
         // Never underline an ellipsis or a match that extends beyond the visible original prefix.
         int visibleEnd = title.equals(entry.title()) ? title.length() : Math.max(0, title.length() - 3);
         if (span != null && span.end() <= visibleEnd) {
            float start = textX + font.getWidth(entry.title().substring(0, span.start()));
            float end = textX + font.getWidth(entry.title().substring(0, span.end()));
            if (end > start) GuiDrawUtils.fillRect(poseStack, start, rowTop + 11,
               Math.min(textX + textWidth, end), rowTop + 12, ColorUtils.withAlpha(accent, 220));
         }
      }

      float footerY = this.resultsTop + resultCount * CommandPaletteLayout.RESULT_ROW_HEIGHT + 7.0F;
      String footer = SearchFeedback.fit("↑ ↓ Select    Enter Open    Esc Close", this.panelWidth - 28, font::getWidth);
      font.drawWithShadow(
         poseStack,
         footer,
         this.panelLeft + 14.0F,
         footerY,
         Color.GRAY
      );
   }
}
