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
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.panels.GamePanel;
import dev.schizo.client.gui.panels.ModuleCategoryPanel;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.gui.components.SettingComponent;
import dev.schizo.client.config.ModulePresetCodec;
import dev.schizo.client.render.TextUtils;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.gui.components.ModuleComponent;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.mixin.EditBoxSelectionAccessor;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ClickGuiScreen extends ClientScreen implements ClientAccess {
   private static final Color STUDIO_FRAME = new Color(17, 18, 23, 250);
   private static final Color STUDIO_NAV = new Color(12, 13, 17, 250);
   private static final Color STUDIO_EDGE = new Color(55, 56, 64, 200);
   private static final Color STUDIO_MUTED = new Color(159, 162, 173);
   private final FloatAnimation selectionAnimation = new FloatAnimation();
   private StudioLayout studioLayout;
   private Panel selectedPanel;

   private static final int[] GAME_PANEL_UNLOCK_SEQUENCE = {
      GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP,
      GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
      GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
      GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
      GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A
   };

   private SettingComponent<?> editingSetting = null;
   private int secretSequenceIndex = 0;
   private boolean gamePanelUnlocked;
   private boolean searchCursorVisible;
   private final ElapsedTimer searchCursorTimer = new ElapsedTimer();
   public EditBox searchBox;
   private String searchCompletion;
   private String normalizedSearchQuery = "";
   public List openModuleComponents = new ArrayList();

   private void handleSecretSequence(int keyCode) {
      if (!this.gamePanelUnlocked
         && GAME_PANEL_UNLOCK_SEQUENCE[this.secretSequenceIndex]
            == keyCode) {
         this.secretSequenceIndex++;
         if (this.secretSequenceIndex
            == GAME_PANEL_UNLOCK_SEQUENCE.length) {
            try {
               Path markerPath = ConfigurationManager.GAME_PANEL_UNLOCK_MARKER_PATH;
               markerPath.toFile().createNewFile();
               java.nio.file.attribute.DosFileAttributeView dos = Files.getFileAttributeView(
                  markerPath, java.nio.file.attribute.DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS
               );
               if (dos != null) {
                  try {
                     dos.setHidden(true);
                  } catch (UnsupportedOperationException unavailable) {

                  }
               }
            } catch (IOException unlockFailure) {
               ClientAccess.logger.warn("Could not persist game panel unlock state", unlockFailure);
            }

            this.gamePanelUnlocked = true;
            this.secretSequenceIndex = 0;
            this.addGamePanel();
            this.resetPanelPositions();
         }
      } else {
         this.secretSequenceIndex = 0;
      }
   }

   private static boolean isControlDown() {
      long window = ClientAccess.schizoMinecraft.getWindow().handle();
      return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
         || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
   }

   private void handleFocusedSearchKey(KeyEvent event) {
      int keyCode = event.key();
      if (keyCode == GLFW.GLFW_KEY_ESCAPE
         || (keyCode == GLFW.GLFW_KEY_F && isControlDown())) {
         this.clearSearch();
         return;
      }

      String searchText = this.searchBox.getValue();
      switch (keyCode) {
         case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
            String completedSearch = TextUtils.normalizeKeyboardLayout(searchText + this.searchCompletion);
            if (!completedSearch.isEmpty()) {
               boolean foundModule = false;
               for (Panel panel : this.panels) {
                  for (Object childEntry : panel.getChildren()) {
                     if (childEntry instanceof ModuleComponent moduleComponent
                        && moduleComponent.getModule().getName().equalsIgnoreCase(completedSearch)) {
                        this.selectPanel(panel);
                        moduleComponent.setOpen(true);
                        if (!this.openModuleComponents.contains(moduleComponent)) this.openModuleComponents.add(moduleComponent);
                        moduleComponent.startSearchHighlight();
                        foundModule = true;
                        break;
                     }
                  }
                  if (foundModule) {
                     break;
                  }
               }
            }
            this.reset();
         }
         case GLFW.GLFW_KEY_TAB -> {
            if (!this.searchCompletion.isEmpty()) {
               this.searchBox.setValue(TextUtils.normalizeKeyboardLayout(searchText + this.searchCompletion));
               this.searchBox.moveCursorToEnd(false);
            }
         }
         default -> this.searchBox.keyPressed(event);
      }
   }

   private String getSearchCursor() {
      if (this.searchCursorTimer
         .hasElapsed(500L)) {
         this.searchCursorVisible = !this.searchCursorVisible;
         this.searchCursorTimer
            .reset();
      }

      return this.searchCursorVisible ? "_" : "";
   }

   private void handlePresetPaste(int keyCode) {
      if (isControlDown() && keyCode == GLFW.GLFW_KEY_V) {
         ModulePresetCodec presetCodec = ClientContext.configurationManager
            .getModulePresetCodec();

         try {
            ModulePresetCodec.DecodedPreset decodedPreset = presetCodec.decode(
               ClientAccess.schizoMinecraft.keyboardHandler.getClipboard()
            );
            ModuleImportConfirmationScreen presetScreen = new ModuleImportConfirmationScreen(
               ClientContext.getClickGui(),
               decodedPreset.module(),
               decodedPreset.json()
            );
            presetScreen.open();
         } catch (Exception pasteException) {
            ClientAccess.logger.warn("Could not import module preset from clipboard", pasteException);
         }
      }
   }

   public String getNormalizedSearchQuery() {
      return this.normalizedSearchQuery;
   }

   public EditBox getSearchBox() {
      return this.searchBox;
   }

   public SettingComponent<?> getEditingSetting() {
      return this.editingSetting;
   }

   public ClickGuiScreen() {
      this.gamePanelUnlocked = ConfigurationManager.GAME_PANEL_UNLOCK_MARKER_PATH
         .toFile()
         .exists();
      for (ModuleCategory category : ModuleCategory.values()) {
         if (category != ModuleCategory.HUD) this.panels.add(new ModuleCategoryPanel(category));
      }
      if (this.gamePanelUnlocked) this.addGamePanel();
      this.selectedPanel = this.panels.isEmpty() ? null : this.panels.getFirst();
   }

   @Override
   protected float resolveGuiScale(float preferredScale) {
      if (GuiScaleUtils.isAdvancedUiScaleLoaded()) return preferredScale;
      var window = ClientAccess.schizoMinecraft.getWindow();
      double base = GuiScaleUtils.calculateScaleMultiplier(1.0F);
      float scale = ClickGuiLayout.fitScale(preferredScale, window.getGuiScaledWidth(), base, 320);
      return ClickGuiLayout.fitScale(scale, window.getGuiScaledHeight(), base, 250);
   }

   @Override
   protected boolean isPanelVisible(Panel panel) {
      return panel == this.selectedPanel;
   }

   @Override
   protected boolean canDragPanel(Panel panel) {
      return false;
   }

   @Override
   public void resetPanelPositions() {
      this.studioLayout = null;
   }

   /** Current visible category; also useful for initialized-client QA. */
   public String getSelectedPanelName() {
      return this.selectedPanel == null ? "" : this.selectedPanel.getName();
   }

   private void selectPanel(Panel panel) {
      if (!this.panels.contains(panel)) return;
      if (this.selectedPanel != panel) {
         this.setEditingSetting(null);
         this.selectedPanel = panel;
         panel.setExpanded(true);
         panel.updateLayout();
      }
   }

   private void ensureStudioLayout() {
      StudioLayout next = StudioLayout.fit(GuiScaleUtils.getScaledWidth(), GuiScaleUtils.getScaledHeight(), this.panels.size());
      if (!this.panels.contains(this.selectedPanel)) {
         this.selectedPanel = this.panels.isEmpty() ? null : this.panels.getFirst();
         this.studioLayout = null;
      }
      if (!next.equals(this.studioLayout)) {
         this.studioLayout = next;
         for (Panel panel : this.panels) {
            panel.setDockedGeometry(next.contentX(), next.contentY(), next.contentWidth(), next.contentHeight());
         }
         if (this.selectedPanel != null) {
            this.selectedPanel.setExpanded(true);
            this.selectedPanel.updateLayout();
            this.selectionAnimation.setValue(next.navY(this.panels.indexOf(this.selectedPanel)));
         }
      }
   }

   @Override
   protected void reset() {
      super.reset();
      this.setFocused(null);
      if (this.searchBox != null) this.searchBox.setFocused(false);
   }

   private void clearSearch() {
      this.reset();
      this.searchBox.setValue("");
      this.searchBox.setSuggestion("");
   }

   @Override
   public void onFilesDrop(List paths) {
      PresetImportScreen.openForDroppedFiles(this, paths);
   }

   public void setEditingSetting(SettingComponent<?> component) {
      if (this.editingSetting != null && this.editingSetting != component) {
         this.editingSetting.editing = false;
      }
      this.editingSetting = component;
   }

   @Override
   public void renderBackgroundOverlay(PoseStack poseStack) {
      this.ensureStudioLayout();
      StudioLayout l = this.studioLayout;
      Color accent = UIModule.INSTANCE.colorSetting.getValue();
      GuiDrawUtils.fillRect(poseStack, 0, 0, GuiScaleUtils.getScaledWidth(), GuiScaleUtils.getScaledHeight(), new Color(5, 6, 9, 215));
      GuiDrawUtils.fillRect(poseStack, l.x(), l.y(), l.right(), l.bottom(), STUDIO_FRAME);
      GuiDrawUtils.fillRect(poseStack, l.x(), l.y(), l.x() + l.navWidth(), l.bottom(), STUDIO_NAV);
      GuiDrawUtils.drawRectOutline(poseStack, l.x(), l.y(), l.right(), l.bottom(), STUDIO_EDGE);
      GuiDrawUtils.fillRect(poseStack, l.x(), l.y(), l.x() + l.navWidth(), l.y() + 2, accent);
      GuiDrawUtils.fillRect(poseStack, l.x() + l.navWidth(), l.y(), l.x() + l.navWidth() + 1, l.bottom(), STUDIO_EDGE);
      GuiDrawUtils.fillRect(poseStack, l.contentX(), l.contentY(), l.contentX() + l.contentWidth(), l.contentY() + l.contentHeight(), UIModule.INSTANCE.backgroundSetting.getValue());
      int index = this.panels.indexOf(this.selectedPanel);
      if (index >= 0) {
         this.selectionAnimation.setTarget((float)l.navY(index), 180L);
         float position = this.selectionAnimation.getValue(progress -> {
            float inverse = 1.0F - (Float)progress;
            return 1.0F - inverse * inverse * inverse;
         });
         GuiDrawUtils.fillRect(poseStack, l.x() + 6, position, l.x() + l.navWidth() - 6, position + l.navRowHeight() - 2, ColorUtils.withAlpha(accent, 32));
         GuiDrawUtils.fillRect(poseStack, l.x() + 6, position + 2, l.x() + 8, position + l.navRowHeight() - 4, accent);
      }
      GuiDrawUtils.flush();
      FontRenderer f = FontRenderer.INSTANCE;
      f.drawWithoutShadow(poseStack, "SCHIZO", l.x() + 12, l.y() + 11, Color.WHITE);
      f.drawWithoutShadow(poseStack, "STUDIO / 2.0", l.x() + 12, l.y() + 25, accent);
      for (int i = 0; i < this.panels.size(); i++) {
         Panel entry = this.panels.get(i);
         f.drawWithoutShadow(poseStack, entry.getName(), l.x() + 14,
            l.navY(i) + (l.navRowHeight() - f.getHeight()) / 2.0F - 1,
            entry == this.selectedPanel ? Color.WHITE : STUDIO_MUTED);
      }
      f.drawWithoutShadow(poseStack, "ESC  Close", l.x() + 12, l.bottom() - 14, STUDIO_MUTED);
      GuiDrawUtils.flush();
   }

   @Override
   public void renderContents(GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
      StudioLayout l = this.studioLayout;
      if (l == null || this.searchBox == null) return;
      FontRenderer font = FontRenderer.INSTANCE;
      Color accent = UIModule.INSTANCE.colorSetting.getValue();
      GuiDrawUtils.fillRect(poseStack, l.contentX(), l.searchY(), l.right() - 8, l.searchY() + 22, new Color(26, 27, 34, 255));
      GuiDrawUtils.drawRectOutline(poseStack, l.contentX(), l.searchY(), l.right() - 9, l.searchY() + 21,
         this.searchBox.isFocused() ? ColorUtils.withAlpha(accent, 150) : STUDIO_EDGE);
      GuiDrawUtils.flush();
      String query = this.searchBox.getValue();
      float x = l.contentX() + 8;
      float y = l.searchY() + (22 - font.getHeight()) / 2.0F;
      float available = Math.max(1, l.contentWidth() - 32);
      ClickGuiSearchText.Selection selection = ClickGuiSearchText.selection(query.length(), this.searchBox.getCursorPosition(),
         ((EditBoxSelectionAccessor)this.searchBox).schizo$getSelectionAnchor());
      int start = 0;
      int caret = selection.caret();
      while (start < caret && font.getWidth(query.substring(start, caret)) > available - 5) start++;
      int end = query.length();
      while (end > start && font.getWidth(query.substring(start, end)) > available - 5) end--;
      if (query.isEmpty() && !this.searchBox.isFocused()) {
         String hint = "Search modules & settings  [Ctrl+F]";
         if (font.getWidth(hint) > available) hint = "Search  [Ctrl+F]";
         font.drawWithoutShadow(poseStack, hint, x, y, STUDIO_MUTED);
      } else {
         if (this.searchBox.isFocused() && selection.start() != selection.end()) {
            int left = Math.max(start, Math.min(end, selection.start()));
            int right = Math.max(start, Math.min(end, selection.end()));
            GuiDrawUtils.fillRect(poseStack, x + font.getWidth(query.substring(start, left)), y - 1,
               x + font.getWidth(query.substring(start, right)), y + font.getHeight() + 1, ColorUtils.withAlpha(accent, 100));
            GuiDrawUtils.flush();
         }
         String visibleQuery = query.substring(start, end);
         font.drawWithoutShadow(poseStack, visibleQuery, x, y, Color.WHITE);
         if (this.searchBox.isFocused() && end == query.length() && caret == query.length()
            && !this.searchCompletion.isEmpty()
            && font.getWidth(visibleQuery + this.searchCompletion) <= available) {
            font.drawWithoutShadow(poseStack, this.searchCompletion, x + font.getWidth(visibleQuery), y, STUDIO_MUTED);
         }
         if (this.searchBox.isFocused() && !this.getSearchCursor().isEmpty() && selection.start() == selection.end()) {
            float caretX = x + font.getWidth(query.substring(start, Math.min(end, caret)));
            GuiDrawUtils.fillRect(poseStack, caretX, y, caretX + 1, y + font.getHeight(), accent);
         }
      }
      if (!query.isEmpty()) {
         font.drawWithoutShadow(poseStack, "X", l.right() - 20, y, STUDIO_MUTED);
      }
      if (!query.isEmpty() && this.selectedPanel instanceof ModuleCategoryPanel category && !category.hasSearchMatches()) {
         String empty = "No matching modules in this category.";
         if (font.getWidth(empty) > available) empty = "No matches.";
         font.drawWithoutShadow(poseStack, empty, l.contentX() + 9, l.contentY() + 31, STUDIO_MUTED);
      }
      String[] toolLabels = {"HUD", "Performance", "Profiles", "Health"};
      String[] shortLabels = {"HUD", "Perf", "Profiles", "Health"};
      for (int i = 0; i < toolLabels.length; i++) {
         int tx = l.toolX(i);
         boolean hover = l.toolIndex(mouseX, mouseY) == i;
         GuiDrawUtils.fillRect(poseStack, tx, l.toolsY(), tx + l.toolWidth(), l.toolsY() + 21,
            hover ? ColorUtils.withAlpha(accent, 52) : new Color(33, 34, 42, 230));
         GuiDrawUtils.fillRect(poseStack, tx, l.toolsY() + 20, tx + l.toolWidth(), l.toolsY() + 21,
            hover ? accent : STUDIO_EDGE);
         GuiDrawUtils.flush();
         String label = font.getWidth(toolLabels[i]) + 6 <= l.toolWidth() ? toolLabels[i] : shortLabels[i];
         font.drawWithoutShadow(poseStack, label, tx + (l.toolWidth() - font.getWidth(label)) / 2,
            l.toolsY() + (21 - font.getHeight()) / 2, Color.WHITE);
      }
      String footer = "Left click: toggle   /   Right click: settings";
      if (font.getWidth(footer) > l.contentWidth()) footer = "L: toggle   R: settings";
      font.drawWithoutShadow(poseStack, footer, l.contentX() + 2, l.bottom() - 15, STUDIO_MUTED);
      this.openModuleComponents.removeIf((java.util.function.Predicate<ModuleComponent>)component -> !component.isOpen());
      // Hidden categories do not advance row animations; keep the open-limit local to the active category.
      List<ModuleComponent> active = new ArrayList<>();
      for (Object entry : this.openModuleComponents) {
         ModuleComponent component = (ModuleComponent)entry;
         if (component.getPanel() == this.selectedPanel) active.add(component);
      }
      if (active.size() > 2) {
         for (int index = 0; index < active.size() - 2; index++) active.get(index).setOpen(false);
      }
   }

   @Override
   public void onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0 || this.studioLayout == null) return;
      StudioLayout l = this.studioLayout;
      int tool = l.toolIndex(mouseX, mouseY);
      if (tool >= 0) {
         this.setEditingSetting(null);
         this.reset();
         switch (tool) {
            case 0 -> this.openStudioChild(ClientContext.getHudEditor());
            case 1 -> this.openStudioChild(new PerformanceCenterScreen());
            case 2 -> ClientAccess.schizoMinecraft.setScreen(new ExpeditionScreen(this, 2));
            case 3 -> ClientAccess.schizoMinecraft.setScreen(new HealthCenterScreen(this));
            default -> throw new IllegalStateException("Unknown Studio destination");
         }
         return;
      }
      if (mouseX >= l.x() && mouseX < l.x() + l.navWidth()) {
         int index = l.navIndex(mouseY);
         if (index >= 0 && index < this.panels.size()) this.selectPanel(this.panels.get(index));
      } else if (mouseX >= l.contentX() && mouseX < l.right() - 8 && mouseY >= l.searchY() && mouseY < l.searchY() + 22) {
         this.setEditingSetting(null);
         if (mouseX >= l.right() - 28 && !this.searchBox.getValue().isEmpty()) {
            this.clearSearch();
         } else {
            this.setInitialFocus(this.searchBox);
         }
      }
   }

   /** Workspace coordinates for an actual tool-button pointer interaction. */
   public double studioToolX(int index) {
      if (this.studioLayout == null || index < 0 || index >= 4) throw new IllegalStateException("Studio layout unavailable");
      return this.studioLayout.toolX(index) + this.studioLayout.toolWidth() / 2.0D;
   }

   public double studioToolY() {
      if (this.studioLayout == null) throw new IllegalStateException("Studio layout unavailable");
      return this.studioLayout.toolsY() + 10.0D;
   }

   private void openStudioChild(ClientScreen child) {
      child.setParentScreen(this);
      ClientAccess.schizoMinecraft.setScreen(child);
   }

   @Override
   public void init() {
      super.init();
      this.searchCompletion = "";
      this.normalizedSearchQuery = "";
      this.studioLayout = null;
      this.searchBox = new EditBox(
         ClientAccess.schizoMinecraft.font,
         7,
         -100,
         100,
         9,
         Component.literal("Search")
      );
      this.searchBox.setVisible(true);
      this.searchBox.setEditable(true);
      this.searchBox
         .setMaxLength(64);
      this.searchBox
         .setResponder(
            searchText -> {
               String normalizedQuery = TextUtils.normalizeKeyboardLayout(searchText);
               this.normalizedSearchQuery = normalizedQuery;
               for (Panel panel : this.panels) panel.resetScroll();
               if (!normalizedQuery.isEmpty()) {
                  Panel exactMatch = null;
                  for (Panel candidate : this.panels) {
                     for (Object entry : candidate.getChildren()) {
                        if (entry instanceof ModuleComponent component
                           && component.getModule().getName().equalsIgnoreCase(normalizedQuery)) {
                           exactMatch = candidate;
                           break;
                        }
                     }
                     if (exactMatch != null) break;
                  }
                  if (exactMatch != null) {
                     this.selectPanel(exactMatch);
                  } else if (!(this.selectedPanel instanceof ModuleCategoryPanel selected) || !selected.hasSearchMatches()) {
                     for (Panel candidate : this.panels) {
                        if (candidate instanceof ModuleCategoryPanel category && category.hasSearchMatches()) {
                           this.selectPanel(candidate);
                           break;
                        }
                     }
                  }
                  for (Object moduleEntry : (List)ClientContext.moduleRegistry.getRegistry()) {
                     Module module = (Module)moduleEntry;
                     if (module instanceof HUDModule || module instanceof HudWidgetModule) {
                        continue;
                     }
                     String suffix = ClickGuiSearchText.completionSuffix(module.getName(), normalizedQuery);
                     if (suffix != null) {
                        this.searchCompletion = suffix;
                        return;
                     }
                  }
               }

               this.searchCompletion = "";
            }
         );
      this.addRenderableWidget(this.searchBox);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.closing) {
         return true;
      }
      if (this.searchBox.isFocused()) {
         this.handleFocusedSearchKey(event);
         return true;
      }
      if (!this.searchBox.getValue().isEmpty() && event.key() == GLFW.GLFW_KEY_ESCAPE && this.editingSetting == null) {
         this.clearSearch();
         return true;
      }

      if (super.keyPressed(event)) {
         return true;
      }
      Keybind toggleKeybind = UIModule.INSTANCE.getKeybind();
      if (toggleKeybind.code()
            == event.key()
         && !toggleKeybind.mouse()
         && this.escapeCloseAllowed
         && !this.searchBox.isFocused()) {
         this.closeWithTransition();
         return true;
      } else {
         if (!this.closing && this.editingSetting == null
            && event.key() == GLFW.GLFW_KEY_F && isControlDown()) {
            this.setInitialFocus(this.searchBox);
            return true;
         }
         this.handleSecretSequence(event.key());
         this.handlePresetPaste(event.key());
         return true;
      }
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.closing) {
         return true;
      }
      if (this.searchBox.isFocused()) {
         this.searchBox.charTyped(event);
         return true;
      }
      return super.charTyped(event);
   }

   public void addGamePanel() {
      GamePanel gamePanel = new GamePanel();
      this.panels.add(gamePanel);
      this.resetPanelPositions();
   }

   @Override
   public void closeWithTransition() {
      super.closeWithTransition();
      ClientContext.configurationManager.saveAsyncSnapshot();
   }

   public boolean isGamePanelUnlocked() {
      return this.gamePanelUnlocked;
   }

}
