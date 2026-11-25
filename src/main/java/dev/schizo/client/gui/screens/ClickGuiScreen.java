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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

public class ClickGuiScreen extends ClientScreen implements ClientAccess {
   private static final int PANEL_LAYOUT_MARGIN = 10;
   private static final int PANEL_LAYOUT_GAP = 3;

   private static final int[] GAME_PANEL_UNLOCK_SEQUENCE = {
      GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP,
      GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
      GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
      GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
      GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A
   };

   private static final HUDModule HUD = (HUDModule)ClientContext.getModule(HUDModule.class);
   private SettingComponent<?> editingSetting = null;
   private int secretSequenceIndex = 0;
   private boolean gamePanelUnlocked;
   private boolean searchCursorVisible;
   private final FloatAnimation searchFocusAnimation = new FloatAnimation();
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
         this.reset();
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
                        moduleComponent.setOpen(true);
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
      int panelX = 10;

      for (ModuleCategory category : ModuleCategory.values()) {
         if (category != ModuleCategory.HUD) {
            ModuleCategoryPanel categoryPanel = new ModuleCategoryPanel(category);
            categoryPanel.setX(
               panelX
            );
            this.panels.add(categoryPanel);
            panelX += categoryPanel.getWidth()
               + 3;
         }
      }

      if (this.gamePanelUnlocked) {
         this.addGamePanel();
      }
   }

   @Override
   protected float resolveGuiScale(float preferredScale) {
      if (GuiScaleUtils.isAdvancedUiScaleLoaded() || this.panels.isEmpty()) {
         return preferredScale;
      }

      int requiredWidth = PANEL_LAYOUT_MARGIN * 2;
      for (int index = 0; index < this.panels.size(); index++) {
         requiredWidth += this.panels.get(index).getWidth();
         if (index + 1 < this.panels.size()) {
            requiredWidth += PANEL_LAYOUT_GAP;
         }
      }

      double baseMultiplier = GuiScaleUtils.calculateScaleMultiplier(1.0F);
      return ClickGuiLayout.fitScale(
         preferredScale,
         ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth(),
         baseMultiplier,
         requiredWidth
      );
   }

   @Override
   protected void reset() {
      super.reset();
      this.setFocused(null);
      this.searchBox.setFocused(false);
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
   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
      this.searchFocusAnimation
         .setTarget(
            this.searchBox.isFocused(), 300L
         );
      this.drawSearchDimmer(poseStack);
      int fontHeight = FontRenderer.INSTANCE
         .getHeight();
      if (this.openModuleComponents.size()
         >= (3)) {
         ((ModuleComponent)this.openModuleComponents.getFirst())
            .setOpen(
               false
            );
      }

      this.openModuleComponents.removeIf((java.util.function.Predicate<dev.schizo.client.gui.components.ModuleComponent>)ModuleComponent::isClosed);
      if (this.searchBox.isFocused()) {
         FontRenderer fontRenderer = FontRenderer.INSTANCE;
         String searchText = this.searchBox.getValue();
         String completionSuffix = this.searchCompletion;
         float searchTextWidth = fontRenderer.getWidth(searchText + completionSuffix + "_");
         float searchLeft = (GuiScaleUtils.getScaledWidth() - searchTextWidth) / 2.0F;
         float searchTop = (GuiScaleUtils.getScaledHeight() - fontHeight) / 2.0F;
         int caret = this.searchBox.getCursorPosition();
         int anchor = ((EditBoxSelectionAccessor)this.searchBox).schizo$getSelectionAnchor();
         ClickGuiSearchText.Selection selection = ClickGuiSearchText.selection(searchText.length(), caret, anchor);
         if (selection.start() != selection.end()) {
            float selectionLeft = searchLeft + fontRenderer.getWidth(searchText.substring(0, selection.start()));
            float selectionRight = searchLeft + fontRenderer.getWidth(searchText.substring(0, selection.end()));
            GuiDrawUtils.fillRect(
               poseStack, selectionLeft, searchTop - 1.0F, selectionRight, searchTop + fontHeight,
               ColorUtils.withAlpha(UIModule.INSTANCE.colorSetting.getValue(), 115)
            );
         }

         String searchLabel = searchText + ChatFormatting.GRAY + completionSuffix;
         fontRenderer.drawWithShadow(poseStack, searchLabel, searchLeft, searchTop, Color.WHITE);

         if (!this.getSearchCursor().isEmpty() && selection.start() == selection.end()) {
            float caretX = searchLeft + fontRenderer.getWidth(searchText.substring(0, selection.caret()));
            fontRenderer.drawWithShadow(poseStack, "_", caretX, searchTop, Color.WHITE);
         }
      } else {
         String searchHint = ClientContext.languageTable
            .getTranslationOrDefault("ui.Search", "Ctrl + F to activate search");
         float hintY = GuiScaleUtils.getScaledHeight()
            - fontHeight
            - 1;
         if (this.getParentScreen() instanceof TitleScreen) {
            hintY -= fontHeight + 1;
         }

         Color hintColor = ColorUtils.pulse(
            Color.GRAY, Color.WHITE, 2000.0D, 250.0D
         );
         int hudMargin = HUD.getSafeX();
         FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               searchHint,
               GuiScaleUtils.getScaledWidth()
                  - FontRenderer.INSTANCE
                     .getWidth(searchHint)
                  - hudMargin,
               hintY - 1.0F,
               hintColor
            );
      }
   }

   private void drawSearchDimmer(PoseStack poseStack) {
      float dimmerOpacity = this.searchFocusAnimation
            .getValue()
         * 0.2F;
      int dimmerColor = ARGB.color(dimmerOpacity, 0);
      GuiDrawUtils.fillRect(
         poseStack,
         0.0F,
         0.0F,
         ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth(),
         ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight(),
         dimmerColor
      );
   }

   @Override
   public void init() {
      super.init();
      this.searchCompletion = "";
      this.normalizedSearchQuery = "";
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
               if (!normalizedQuery.isEmpty()) {
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
