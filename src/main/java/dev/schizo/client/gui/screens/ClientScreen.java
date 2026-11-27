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
import dev.schizo.client.gui.components.SettingComponent;
import dev.schizo.client.gui.panels.ModuleCategoryPanel;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.utils.InterpolatedFloat;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.gui.CursorType;
import dev.schizo.client.gui.UiMotionPolicy;
import dev.schizo.client.utils.world.Season;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiProjectionUtils;
import dev.schizo.client.gui.ScissorStack;
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.render.BlurModule;
import dev.schizo.client.SchizoClient;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class ClientScreen extends Screen implements ClientAccess {

   private static final Identifier AERO_TEXTURE = Identifier.fromNamespaceAndPath(
      "schizo", "textures/aero.png"
   );
   private static UIModule getUiModule() {
      return ClientContext.getModule(UIModule.class);
   }

   private static BlurModule getBlurModule() {
      return ClientContext.getModule(BlurModule.class);
   }
   private static final long TRANSITION_DURATION_NANOS = 180_000_000L;
   private static final int MAX_SEASONAL_PARTICLES = 12;
   private static final int PANEL_START_X = 10;
   private static final int PANEL_START_Y = 5;
   private static final int PANEL_GAP = 3;
   private static final float TOOLTIP_BACKGROUND_DEPTH = 500.0F;
   private static final float SCREEN_CONTENT_DEPTH = 1000.0F;

   public static CursorType cursor = CursorType.STANDARD;
   protected final ArrayList<Panel> panels = new ArrayList<>();
   protected final List<SnowflakeParticle> seasonalParticles = new ArrayList<>();
   private final boolean winterSeason;
   protected final InterpolatedFloat hoverMouseX = new InterpolatedFloat(
      2.5F,
      true
   );
   protected final InterpolatedFloat hoverMouseY = new InterpolatedFloat(
      2.5F,
      true
   );
   protected boolean panelDragging = false;
   protected float guiScale;
   protected boolean closing;
   protected String tooltipText = "";
   protected long lastHoverNanos = 0L;
   protected long transitionStartedNanos;
   private Screen parentScreen;
   protected boolean escapeCloseAllowed;
   private boolean skipOpeningTransition;

   public Screen getParentScreen() {
      return this.parentScreen;
   }

   public boolean isClosing() {
      return this.closing;
   }

   public void suppressEscapeClose() {
      this.escapeCloseAllowed = false;
   }

   public void markHovered() {
      this.lastHoverNanos = System.nanoTime();
   }

   public void resetSeasonalParticles() {
      this.seasonalParticles.clear();
      if (!this.winterSeason) {
         return;
      }

      for (int particleIndex = 0;
         particleIndex
            < MAX_SEASONAL_PARTICLES;
         particleIndex++
      ) {
         this.seasonalParticles.add(new SnowflakeParticle());
      }
   }

   public float getTransitionOpacity() {
      if (UiMotionPolicy.isReducedMotion()) {
         return this.closing ? 0.0F : 1.0F;
      }
      float opacity = (float)(System.nanoTime() - this.transitionStartedNanos)
         / TRANSITION_DURATION_NANOS;
      if (this.closing) {
         opacity = 1.0F
            - opacity;
      }

      return Mth.clamp(
         opacity,
         0.0F,
         1.0F
      );
   }

   public void setPanelDragging(boolean dragging) {
      this.panelDragging = dragging;
   }

   public boolean hasMenuParent() {
      return this.getParentScreen() instanceof TitleScreen
         || this.getParentScreen() instanceof DisconnectedScreen
         || this.getParentScreen() instanceof JoinMultiplayerScreen;
   }

   public boolean isPanelDragging() {
      return this.panelDragging;
   }

   public float getGuiScale() {
      return this.guiScale;
   }

   protected boolean isPanelVisible(Panel panel) {
      return true;
   }

   protected boolean canDragPanel(Panel panel) {
      return true;
   }

   protected float resolveGuiScale(float preferredScale) {
      return preferredScale;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   protected ClientScreen() {
      super(Component.literal(SchizoClient.NAME));
      this.winterSeason = Season.WINTER.containsMonth(LocalDate.now().getMonthValue());
      this.resetSeasonalParticles();
   }

   protected void reset() {
      this.lastHoverNanos = 0L;
   }

   public void onMouseClick(double mouseX, double mouseY, int button) {
   }

   public void setTooltipText(String text) {
      this.tooltipText = text;
   }

   @Override
   public void tick() {
      if (this.closing
         && (UiMotionPolicy.isReducedMotion()
            || System.nanoTime() - this.transitionStartedNanos >= TRANSITION_DURATION_NANOS)) {
         for (Panel panel : this.panels) {
            panel.setDragging(false);
         }

         if (this.parentScreen != null) {
            ClientAccess.schizoMinecraft.setScreen(
               this.parentScreen
            );
         } else {
            this.onClose();
         }

         this.setParentScreen(null);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      event = new MouseButtonEvent(
         event.x()
            / GuiScaleUtils.calculateScaleMultiplier(this.guiScale),
         event.y()
            / GuiScaleUtils.calculateScaleMultiplier(this.guiScale),
         event.buttonInfo()
      );

      for (int panelIndex = this.panels.size() - 1; panelIndex >= 0; panelIndex--) {
         Panel panel = this.panels
            .get(panelIndex);
         if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panel.setDragging(false);
         }
         panel.mouseReleased(
            event.x(), event.y(), event.button()
         );
      }

      this.onMouseRelease(
         event.x(), event.y(), event.button()
      );
      return super.mouseReleased(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      SettingComponent<?> editor = this.focusedSetting();
      if (editor != null) {
         String input = event.codepointAsString();
         for (int index = 0; index < input.length(); index++) {
            editor.charTyped(input.charAt(index));
         }
         return true;
      }
      String characters = event.codepointAsString();
      for (int panelIndex = this.panels.size() - 1; panelIndex >= 0; panelIndex--) {
         Panel panel = this.panels.get(panelIndex);
         if (!this.isPanelVisible(panel)) continue;
         for (int index = 0; index < characters.length(); index++) {
            panel.charTyped(characters.charAt(index));
         }
      }

      return super.charTyped(event);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.closing) {
         return false;
      }

      event = new MouseButtonEvent(
         event.x()
            / GuiScaleUtils.calculateScaleMultiplier(this.guiScale),
         event.y()
            / GuiScaleUtils.calculateScaleMultiplier(this.guiScale),
         event.buttonInfo()
      );
      boolean handledByScreen = super.mouseClicked(event, doubleClick);
      Panel draggedPanel = null;

      for (int panelIndex = this.panels.size() - 1; panelIndex >= 0; panelIndex--) {
         Panel panel = this.panels
            .get(panelIndex);
         if (!this.isPanelVisible(panel)
            || (!panel.isMouseOverHeader(event.x(), event.y()) && !panel.isMouseOver(event.x(), event.y()))) {
            continue;
         }
         if (this.canDragPanel(panel) && panel.isMouseOverHeader(event.x(), event.y())
            && event.button() == 0
            && !panel.isDragging()) {
            draggedPanel = panel;
            panel.setDragging(true);
            panel.dragOffsetX = (int)(
               event.x()
                  - panel.getX()
            );
            panel.dragOffsetY = (int)(
               event.y()
                  - panel.getY()
            );
         }

         panel.mouseClicked(
            event.x(), event.y(), event.button()
         );
         break;
      }

      if (draggedPanel != null) {
         this.panels.remove(draggedPanel);
         this.panels.add(draggedPanel);
      }

      this.reset();
      this.onMouseClick(
         event.x(), event.y(), event.button()
      );
      return handledByScreen;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      double scale = GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
      mouseX /= scale;
      mouseY /= scale;
      if (verticalAmount != 0.0) {
         for (int index = this.panels.size() - 1; index >= 0; index--) {
            Panel panel = this.panels.get(index);
            if (this.isPanelVisible(panel) && panel.isMouseOver(mouseX, mouseY)) {
               if (!panel.isDragging()) {
                  panel.mouseScrolled(mouseX, mouseY, verticalAmount);
               }
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
   }

   public void onMouseRelease(double mouseX, double mouseY, int button) {
   }

   public ArrayList<Panel> getPanels() {
      return this.panels;
   }

   public void setParentScreen(Screen screen) {
      this.parentScreen = screen;
   }

   @Override
   protected void init() {
      int screenWidth = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth();
      int screenHeight = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight();
      if (this.getParentScreen() != null && !(this.getParentScreen() instanceof ClientScreen)) {
         this.getParentScreen()
            .init(screenWidth, screenHeight);
      }

      if (!this.isWorldUnavailable()) {
         this.hoverMouseX
            .setValue(
               screenWidth
                  / 2.0F
            );
         this.hoverMouseY
            .setValue(
               screenHeight
                  / 2.0F
            );
      }

      this.panelDragging = false;
      this.getPanels()
         .forEach(panel -> panel.setDragging(false));
      this.guiScale = this.resolveGuiScale(
         UIModule.INSTANCE.scaleSetting.getValue()
      );
      this.closing = false;

      double scale = GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
      for (Panel panel : this.panels) {
         panel.init();
         panel.placeWithinViewport(panel.getX(), panel.getY(), screenWidth / scale, screenHeight / scale);
      }

      if (this.skipOpeningTransition) {
         this.transitionStartedNanos = System.nanoTime() - TRANSITION_DURATION_NANOS;
         this.skipOpeningTransition = false;
      } else {
         this.transitionStartedNanos = System.nanoTime();
      }
   }

   protected SettingComponent<?> focusedSetting() {
      ClickGuiScreen clickGui = ClientContext.getClickGui();
      SettingComponent<?> editor = clickGui.getEditingSetting();
      if (editor == null) {
         return null;
      }
      if (!this.panels.contains(editor.getPanel()) || !this.isPanelVisible(editor.getPanel())) {
         if (ClientAccess.schizoMinecraft.screen == this) {
            clickGui.setEditingSetting(null);
         }
         return null;
      }
      if (editor.editing && editor.getPanel().acceptsSettingInput(editor) && !editor.isUnavailable()) {
         return editor;
      }
      editor.editing = false;
      clickGui.setEditingSetting(null);
      return null;
   }

   @Override
   public void removed() {
      SettingComponent<?> editor = this.focusedSetting();
      if (editor != null) {
         editor.editing = false;
         ClientContext.getClickGui().setEditingSetting(null);
      }
      for (Panel panel : this.panels) {
         panel.setDragging(false);
      }
      super.removed();
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      this.escapeCloseAllowed = true;
      SettingComponent<?> editor = this.focusedSetting();
      if (editor != null) {
         editor.keyPressed(event.key());
         if (!editor.editing) {
            ClientContext.getClickGui().setEditingSetting(null);
         }
         return true;
      }

      for (int panelIndex = this.panels.size() - 1; panelIndex >= 0; panelIndex--) {
         Panel panel = this.panels.get(panelIndex);
         if (this.isPanelVisible(panel)) panel.keyPressed(event.key());
      }

      if (event.key() == GLFW.GLFW_KEY_ESCAPE && this.escapeCloseAllowed) {
         this.closeWithTransition();
      }

      return false;
   }

   @Override
   public void onClose() {
      super.onClose();
      CursorType.STANDARD.apply();
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      UIModule uiModule = getUiModule();
      if (uiModule == null) {
         return;
      }
      BlurModule blurModule = getBlurModule();
      float transitionOpacity = this.getTransitionOpacity();
      PoseStack poseStack = GuiProjectionUtils.toPoseStack(graphics);
      this.hoverMouseX
         .setTarget(mouseX);
      this.hoverMouseY
         .setTarget(mouseY);
      float hoverX = this.hoverMouseX
         .updateAndGet();
      float hoverY = this.hoverMouseY
         .updateAndGet();
      poseStack.translate(
         0.0F,
         0.0F,
         SCREEN_CONTENT_DEPTH
      );
      cursor.apply();
      cursor = CursorType.STANDARD;
      if (this.winterSeason && !UiMotionPolicy.isReducedMotion()) {
         this.seasonalParticles.removeIf(SnowflakeParticle::isExpired);
         if (this.seasonalParticles.size() < MAX_SEASONAL_PARTICLES) {
            this.seasonalParticles.add(new SnowflakeParticle());
         }
      }

      try {
         RenderUtils.setRenderColor(
            1.0F,
            1.0F,
            1.0F,
            transitionOpacity
         );
         float scale = (float)GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
         GuiScaleUtils.applyGuiScale(this.guiScale);
         poseStack.scale(
         scale,
         scale,
         1.0F
      );
      mouseX = (int)(mouseX / scale);
      mouseY = (int)(mouseY / scale);
      hoverX /= scale;
      hoverY /= scale;
      this.tooltipText = "";
      this.renderBackgroundOverlay(poseStack);
      if (uiModule.aeroSetting.getValue()) {
         this.renderAeroPanels(poseStack);
      }

      float blurStrength = blurModule == null ? 0.0F : blurModule.getRadius() * transitionOpacity;
      if (!this.isClosing()
         && blurModule != null
         && blurModule.isToggled()) {
         GuiDrawUtils.flush();
         MaskShaderRenderer.renderBlur(
            () -> {
               for (Panel blurPanel : this.getPanels()) {
                  if (!this.isPanelVisible(blurPanel)) continue;
                  float expansion = blurPanel.getExpansionValue();
                  float expandedContentHeight = (
                        blurPanel.getHeight()
                           - blurPanel.getHeaderHeight()
                     )
                     * expansion;
                  GuiDrawUtils.fillRect(
                     poseStack,
                     blurPanel.getX(),
                     blurPanel.getY(),
                     blurPanel.getX()
                        + blurPanel.getWidth(),
                     blurPanel.getY()
                           + blurPanel.getHeaderHeight()
                        + expandedContentHeight,
                     -1
                  );
               }

               GuiDrawUtils.flush();
            },
            blurStrength
         );
      }

      if (this.winterSeason) {
         for (SnowflakeParticle particle : this.seasonalParticles) {
            particle.render(poseStack);
         }
      }

      for (Panel panel : this.panels) {
         if (!this.isPanelVisible(panel)) continue;
         panel.updateDragPosition(mouseX, mouseY);
         panel.render(poseStack, mouseX, mouseY);
      }

      if (!this.tooltipText.isBlank()) {
         GuiDrawUtils.flush();
         String[] tooltipLines = this.tooltipText.split("\n");
         float tooltipWidth = 0.0F;

         for (String tooltipLine : tooltipLines) {
            float lineWidth = FontRenderer.INSTANCE
               .getWidth(tooltipLine);
            if (lineWidth > tooltipWidth) {
               tooltipWidth = lineWidth;
            }
         }

         float tooltipExtentX = tooltipWidth + 1.0F;
         float preferredTooltipLeft = hoverX + 9.0F;
         if (preferredTooltipLeft + tooltipExtentX > GuiScaleUtils.getScaledWidth()) {
            preferredTooltipLeft = hoverX - tooltipWidth - 2.0F;
         }
         float tooltipLeft = ScreenTooltipLayout.clampStart(
            preferredTooltipLeft, tooltipExtentX, GuiScaleUtils.getScaledWidth()
         );
         float tooltipRight = tooltipLeft + tooltipExtentX;

         float lineHeight = FontRenderer.INSTANCE.getHeight() + 1.0F;
         float tooltipHeight = lineHeight * tooltipLines.length;
         float tooltipTop = ScreenTooltipLayout.clampStart(
            hoverY - 1.0F, tooltipHeight, GuiScaleUtils.getScaledHeight()
         );
         float tooltipBottom = tooltipTop + tooltipHeight;
         float blurLeft = tooltipLeft;
         float blurRight = tooltipRight;
         int blurY = (int)tooltipTop;
         if (blurModule != null && blurModule.isToggled()) {
            MaskShaderRenderer.renderBlur(
               () -> RenderUtils.INSTANCE
                  .fillRect(
                     poseStack,
                     blurLeft,
                     blurY
                        - 1.0F,
                     blurRight,
                     tooltipBottom,
                     ColorUtils.packArgb(10, 10, 10, 140)
                  ),
               blurModule.getRadius()
            );
         }
         poseStack.pushPose();
         try {
            poseStack.translate(0.0F, 0.0F, TOOLTIP_BACKGROUND_DEPTH);
            RenderUtils.INSTANCE
               .fillRect(
                  poseStack,
                  tooltipLeft,
                  tooltipTop,
                  tooltipRight,
                  tooltipBottom,
                  new Color(10, 10, 10, 80)
               );
            int lineIndex = 0;

            for (String tooltipLine : tooltipLines) {
               FontRenderer.INSTANCE
                  .drawWithShadow(
                     poseStack,
                     tooltipLine,
                     tooltipLeft
                        + 1.0F,
                     tooltipTop + 1.0F + lineHeight * lineIndex,
                     Color.white
                  );
               lineIndex++;
            }

            this.lastHoverNanos = System.nanoTime();
         } finally {
            poseStack.popPose();
         }
      } else if (this.lastHoverNanos != 0L
         && System.nanoTime() - this.lastHoverNanos > 750_000_000L) {
         this.lastHoverNanos = 0L;
      }

      this.renderContents(graphics, poseStack, mouseX, mouseY, partialTick);
         GuiDrawUtils.flush();
      } finally {
         RenderUtils.setRenderColor(
            1.0F,
            1.0F,
            1.0F,
            1.0F
         );
         GuiScaleUtils.restoreGuiScale();
         poseStack.translate(
            0.0F,
            0.0F,
            -SCREEN_CONTENT_DEPTH
         );
      }
   }

   public void open() {
      if (ClientAccess.schizoMinecraft.screen instanceof ClientScreen previousScreen) {
         this.setParentScreen(
            previousScreen.parentScreen
         );
         previousScreen.setParentScreen(null);
         this.skipOpeningTransition = true;
      } else {
         this.setParentScreen(
            ClientAccess.schizoMinecraft.screen
         );
      }

      ClientAccess.schizoMinecraft.setScreen(this);
   }

   public void closeWithTransition() {
      if (!this.closing) {
         this.panelDragging = false;
         this.transitionStartedNanos = System.nanoTime();
         if (!(this.parentScreen instanceof ClickGuiScreen) && UIModule.INSTANCE.isToggled()) {
            UIModule.INSTANCE
               .toggle();
         }
      } else {
         this.onClose();
      }

      this.closing = true;
   }

   public void resetPanelPositions() {
      this.resetSeasonalParticles();
      this.panels
         .sort(
            Comparator.comparing(
               (Object panelEntry) -> panelEntry instanceof ModuleCategoryPanel categoryPanel
                  ? categoryPanel.category.ordinal()
                  : 100
            )
         );
      int panelX = PANEL_START_X;

      for (Panel panel : this.panels) {
         panel.setX(
            panelX
         );
         panel.setY(PANEL_START_Y);
         panelX += panel.getWidth()
            + PANEL_GAP;
      }
   }

   public void renderAeroPanels(PoseStack poseStack) {
      UIModule uiModule = getUiModule();
      if (uiModule == null) {
         return;
      }
      int textureAlpha = RenderUtils.getRenderColor()[3]
         / (2);
      int textureColor = ARGB.color(textureAlpha, -1);

      for (Panel panel : this.getPanels()) {
         if (!this.isPanelVisible(panel)) continue;
         int outlinePadding = uiModule.lineSetting.getValue() ? 2 : 0;
         int panelX = panel.getX();
         int panelY = panel.getY();
         float expansion = panel.getExpansionValue();
         float contentHeight = panel.getHeight()
            - panel.getHeaderHeight();
         float panelWidth = panel.getWidth();
         if (uiModule.lineSetting.getValue()) {
            panelX--;
            panelY--;
            panelWidth += 2.0F;
         } else {
            contentHeight -= outlinePadding;
         }

         contentHeight *= expansion;
         poseStack.pushPose();
         try {
            poseStack.translate(panelX, panelY, 0.0F);
            ScissorStack.push(
               panelX,
               panelY,
               (int)panelWidth,
               (int)Math.floor(
                  panel.getHeaderHeight()
                     + contentHeight
                     + outlinePadding
               )
            );
            try {
               GuiDrawUtils.drawTexture(
                  poseStack,
                  AERO_TEXTURE,
                  (int)(
                     -300.0F + panelX / 3.0F
                  ),
                  0,
                  0.0F,
                  0.0F,
                  600,
                  450,
                  600,
                  450,
                  textureColor
               );
               RenderUtils.getEntityBuffers().endBatch();
            } finally {
               ScissorStack.pop();
            }
         } finally {
            poseStack.popPose();
         }
      }
   }

   public void renderBackgroundOverlay(PoseStack poseStack) {
      GuiDrawUtils.fillRect(
         poseStack,
         0.0F,
         0.0F,
         GuiScaleUtils.getScaledWidth(),
         GuiScaleUtils.getScaledHeight(),
         UIModule.INSTANCE.tintSetting.getValue()
      );
      GuiDrawUtils.fillVerticalGradient(
         poseStack,
         0.0F,
         0.0F,
         GuiScaleUtils.getScaledWidth(),
         GuiScaleUtils.getScaledHeight(),
         ColorUtils.toArgb(
            UIModule.INSTANCE.gradientColorSetting.getValue(), 0
         ),
         (UIModule.INSTANCE.gradientColorSetting.getValue())
            .hashCode()
      );
      GuiDrawUtils.flush(false);
   }

}
