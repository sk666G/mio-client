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
import dev.schizo.client.gui.components.HudModulePanel;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.render.ClientRenderPipelines;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.utils.collections.IntPair;
import dev.schizo.client.render.HudAnchorGroup;
import dev.schizo.client.render.HudHoverLabel;
import dev.schizo.client.utils.ClampedInterpolatedFloat;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiProjectionUtils;
import dev.schizo.client.gui.components.ModuleComponent;
import dev.schizo.client.render.HudWidgetRenderer;
import dev.schizo.client.render.HudWidgetDrag;
import dev.schizo.client.render.HudWidgetSnap;
import dev.schizo.client.render.HudHorizontalAlignment;
import dev.schizo.client.render.HudVerticalAlignment;
import dev.schizo.client.utils.KeyUtils;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.gui.components.StringSettingComponent;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.settings.options.HudAnchor;
import dev.schizo.client.config.HudLayoutStore;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.Module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends ClientScreen implements ClientAccess {

   private static HUDModule hud = (HUDModule)ClientContext.getModule(HUDModule.class);
   private static final List<HudAnchorGroup> anchorGroups = Collections.synchronizedList(
      new ArrayList<>()
   );
   public static final HudHoverLabel hoverLabel = new HudHoverLabel();
   private final HudMarginController marginController = new HudMarginController();
   private final ClampedInterpolatedFloat hoverLabelOpacity = new ClampedInterpolatedFloat(
      2.0F, true
   );
   private final HudModulePanel modulePanel = new HudModulePanel();
   private HudWidgetRenderer draggedWidget;
   private boolean centerPanelPending;
   private boolean directionsRefreshPending;
   private double dragOffsetX;
   private double dragOffsetY;
   private float mouseX;
   private float mouseY;
   private static final int LAYOUT_HISTORY_CAPACITY = 100;
   private final HudLayoutHistory<LayoutSnapshot> layoutHistory =
      new HudLayoutHistory<>(LAYOUT_HISTORY_CAPACITY);
   private LayoutSnapshot pendingLayoutBefore;

   private record WidgetLayoutState(
      HudWidgetRenderer widget,
      HudAnchor groupAnchor,
      float relativeX,
      float relativeY,
      HudHorizontalAlignment horizontalAlignment,
      HudVerticalAlignment verticalAlignment,
      float scale
   ) {
   }

   private record LayoutSnapshot(List<WidgetLayoutState> widgets) {
   }


   private LayoutSnapshot captureLayout() {
      List<WidgetLayoutState> widgetStates = new ArrayList<>();
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         for (Object widgetElement : group.getWidgets()) {
            HudWidgetRenderer widget = (HudWidgetRenderer)widgetElement;
            float scale = widget instanceof ResizableHudWidgetRenderer resizable
               ? resizable.getSizeScale()
               : 1.0F;
            widgetStates.add(new WidgetLayoutState(
               widget,
               group.getAnchor(),
               widget.getRelativeX(),
               widget.getRelativeY(),
               widget.getHorizontalAlignment(),
               widget.getVerticalAlignment(),
               scale
            ));
         }
      }
      return new LayoutSnapshot(List.copyOf(widgetStates));
   }

   private void restoreLayout(LayoutSnapshot snapshot) {
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         group.getWidgets().clear();
      }

      for (WidgetLayoutState state : snapshot.widgets()) {
         HudWidgetRenderer widget = state.widget();
         widget.setAnchor(state.groupAnchor());
         widget.setX(state.relativeX(), false);
         widget.setY(state.relativeY(), false);
         widget.setHorizontalAlignment(state.horizontalAlignment());
         widget.setVerticalAlignment(state.verticalAlignment());
         if (widget instanceof ResizableHudWidgetRenderer resizable) {
            resizable.setSizeScale(state.scale());
         }

         HudAnchorGroup group = this.getAnchorGroup(state.groupAnchor());
         if (group != null) {
            group.getWidgets().add(widget);
         }
         widget.updateDirections();
      }

      this.requestDirectionRefresh();
   }

   /** Exports only widget placement and scale; module settings remain in the normal client config. */
   public JsonObject exportLayoutPreset() {
      this.finishLayoutEdit();
      LayoutSnapshot snapshot = this.captureLayout();
      JsonObject layout = new JsonObject();
      layout.addProperty("version", HudLayoutStore.CURRENT_SCHEMA_VERSION);
      JsonArray widgets = new JsonArray();

      for (WidgetLayoutState state : snapshot.widgets()) {
         JsonObject widget = new JsonObject();
         widget.addProperty("module", state.widget().getWidgetModule().getConfigName());
         widget.addProperty("anchor", state.groupAnchor().name());
         widget.addProperty("x", state.relativeX());
         widget.addProperty("y", state.relativeY());
         widget.addProperty("horizontal", state.horizontalAlignment().name());
         widget.addProperty("vertical", state.verticalAlignment().name());
         widget.addProperty("scale", state.scale());
         widgets.add(widget);
      }

      layout.add("widgets", widgets);
      return layout;
   }

   /** Validates a saved layout completely before changing the live widget groups. */
   public void applyLayoutPreset(JsonObject layout) {
      if (layout == null || !layout.has("version") || !layout.get("version").isJsonPrimitive()
         || !layout.get("version").getAsJsonPrimitive().isNumber()
         || layout.get("version").getAsDouble() != HudLayoutStore.CURRENT_SCHEMA_VERSION
         || !layout.has("widgets") || !layout.get("widgets").isJsonArray()) {
         throw new IllegalArgumentException("Unsupported or malformed HUD layout");
      }

      Map<String, HudWidgetModule> modulesByName = new HashMap<>();
      for (Object moduleElement : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)moduleElement;
         if (module instanceof HudWidgetModule widgetModule) {
            modulesByName.putIfAbsent(widgetModule.getConfigName(), widgetModule);
         }
      }

      JsonArray widgetArray = layout.getAsJsonArray("widgets");
      if (widgetArray.size() > 512) {
         throw new IllegalArgumentException("HUD layout contains too many widgets");
      }

      List<WidgetLayoutState> restoredStates = new ArrayList<>(widgetArray.size());
      Set<String> restoredNames = new HashSet<>();
      for (JsonElement element : widgetArray) {
         if (!element.isJsonObject()) {
            throw new IllegalArgumentException("HUD layout contains an invalid widget entry");
         }
         JsonObject widgetJson = element.getAsJsonObject();
         String moduleName = readRequiredString(widgetJson, "module");
         if (!restoredNames.add(moduleName)) {
            throw new IllegalArgumentException("HUD layout contains a duplicate widget: " + moduleName);
         }
         HudWidgetModule widgetModule = modulesByName.get(moduleName);
         if (widgetModule == null || widgetModule.getRenderer() == null) {
            // A layout created by a newer client can contain widgets this client does not know.
            continue;
         }

         HudAnchor anchor = readEnum(widgetJson, "anchor", HudAnchor.class);
         HudHorizontalAlignment horizontal = readEnum(
            widgetJson, "horizontal", HudHorizontalAlignment.class
         );
         HudVerticalAlignment vertical = readEnum(widgetJson, "vertical", HudVerticalAlignment.class);
         float relativeX = readNormalizedCoordinate(widgetJson, "x");
         float relativeY = readNormalizedCoordinate(widgetJson, "y");
         float scale = readFloat(widgetJson, "scale");
         if (scale < 0.5F || scale > 2.0F) {
            throw new IllegalArgumentException("HUD widget scale is outside the supported range");
         }

         restoredStates.add(new WidgetLayoutState(
            widgetModule.getRenderer(), anchor, relativeX, relativeY, horizontal, vertical, scale
         ));
      }

      // Retain widgets introduced after a layout was saved instead of silently removing them.
      LayoutSnapshot current = this.captureLayout();
      for (WidgetLayoutState currentState : current.widgets()) {
         String moduleName = currentState.widget().getWidgetModule().getConfigName();
         if (restoredNames.add(moduleName)) {
            restoredStates.add(currentState);
         }
      }

      this.beginLayoutEdit();
      this.restoreLayout(new LayoutSnapshot(List.copyOf(restoredStates)));
      this.finishLayoutEdit();
   }

   private static String readRequiredString(JsonObject object, String key) {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
         throw new IllegalArgumentException("HUD layout is missing a valid " + key + " value");
      }
      String text = value.getAsString();
      if (text.isBlank() || text.length() > 128) {
         throw new IllegalArgumentException("HUD layout contains an invalid " + key + " value");
      }
      return text;
   }

   private static float readNormalizedCoordinate(JsonObject object, String key) {
      float value = readFloat(object, key);
      if (value < 0.0F || value > 1.0F) {
         throw new IllegalArgumentException("HUD layout coordinate is outside the supported range");
      }
      return value;
   }

   private static float readFloat(JsonObject object, String key) {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
         throw new IllegalArgumentException("HUD layout is missing a valid " + key + " value");
      }
      float number = value.getAsFloat();
      if (!Float.isFinite(number)) {
         throw new IllegalArgumentException("HUD layout contains a non-finite " + key + " value");
      }
      return number;
   }

   private static <T extends Enum<T>> T readEnum(JsonObject object, String key, Class<T> enumType) {
      String value = readRequiredString(object, key);
      try {
         return Enum.valueOf(enumType, value);
      } catch (IllegalArgumentException invalidValue) {
         throw new IllegalArgumentException("HUD layout contains an invalid " + key + " value", invalidValue);
      }
   }

   /** Starts a layout gesture. Multiple calls during the same gesture share one snapshot. */
   public void beginLayoutEdit() {
      if (this.pendingLayoutBefore == null) {
         this.pendingLayoutBefore = this.captureLayout();
      }
   }

   /** Commits one layout gesture, recording it only if something actually changed. */
   public void finishLayoutEdit() {
      LayoutSnapshot previous = this.pendingLayoutBefore;
      if (previous == null) {
         return;
      }
      this.pendingLayoutBefore = null;
      LayoutSnapshot current = this.captureLayout();
      if (!previous.equals(current)) {
         this.layoutHistory.record(previous);
      }
   }

   /** Cancels the in-progress gesture and restores its starting layout. */
   public void cancelLayoutEdit() {
      LayoutSnapshot previous = this.pendingLayoutBefore;
      this.pendingLayoutBefore = null;
      if (previous != null) {
         this.restoreLayout(previous);
      }
   }

   private boolean cancelPendingLayoutGesture() {
      if (this.pendingLayoutBefore == null) {
         return false;
      }

      if (this.draggedWidget != null) {
         this.draggedWidget.setSelected(false);
         this.draggedWidget = null;
      }

      ResizableHudWidgetRenderer.cancelActiveResize();
      this.cancelLayoutEdit();
      return true;
   }

   private void undoLayoutEdit() {
      if (this.cancelPendingLayoutGesture()) {
         return;
      }
      LayoutSnapshot previous = this.layoutHistory.undo(this.captureLayout());
      if (previous != null) {
         this.restoreLayout(previous);
      }
   }

   private void redoLayoutEdit() {
      if (this.cancelPendingLayoutGesture()) {
         return;
      }
      LayoutSnapshot next = this.layoutHistory.redo(this.captureLayout());
      if (next != null) {
         this.restoreLayout(next);
      }
   }

   private IntPair getArrowKeyOffset(int keyCode) {
      int step = KeyUtils.isShiftDown() ? 10 : 1;
      return switch (keyCode) {
         case 262 -> new IntPair(step, 0);
         case 263 -> new IntPair(-step, 0);
         case 264 -> new IntPair(0, step);
         case 265 -> new IntPair(0, -step);
         default -> null;
      };
   }

   public HudEditorScreen() {
      this.panels
         .add(this.modulePanel);

      for (HudAnchor anchor : HudAnchor.values()) {
         anchorGroups.add(new HudAnchorGroup(anchor));
      }
   }

   private void applyDragSnapping(HudWidgetRenderer widget, float[] size, float screenWidth, float screenHeight) {
      float snappedX = HudWidgetSnap.snap(widget.getX(), size[0], screenWidth, hud.getSafeX(), 5.0F);
      float snappedY = HudWidgetSnap.snap(widget.getY(), size[1], screenHeight, hud.getSafeY(), 5.0F);
      widget.setX(snappedX, true);
      widget.setY(snappedY, true);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.draggedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         HudWidgetRenderer widget = this.draggedWidget;
         float[] size = widget.getDimensions();
            float screenWidth = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth();
            float screenHeight = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight();
            widget.setX(HudWidgetDrag.clampCoordinate(event.x(), this.dragOffsetX, screenWidth, size[0]), true);
            widget.setY(HudWidgetDrag.clampCoordinate(event.y(), this.dragOffsetY, screenHeight, size[1]), true);
            this.applyDragSnapping(widget, size, screenWidth, screenHeight);
         widget.updateDirections();
         return true;
      }
      return super.mouseDragged(event, dragX, dragY);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.marginController.mouseReleased(event.button());
      boolean released = super.mouseReleased(event);
      if (this.draggedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         HudWidgetRenderer widget = this.draggedWidget;
         this.draggedWidget = null;
         HudAnchorGroup freeGroup = this.getAnchorGroup(HudAnchor.NONE);
         widget.setSelected(false);
         freeGroup.getWidgets().remove(widget);
         int screenWidth = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth();
         int screenHeight = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight();
         widget.updateSnapping(screenWidth, screenHeight, 5.0F);

         for (HudAnchorGroup group : anchorGroups) {
            for (Object widgetElement : group.getWidgets()) {
               HudWidgetRenderer otherWidget = (HudWidgetRenderer)widgetElement;
               if (otherWidget.getBounds().intersects(widget.getBounds())) {
                  group.getWidgets().addLast(widget);
                  widget.setAnchor(group.getAnchor());
                  this.finishLayoutEdit();
                  return true;
               }
            }
         }

         for (HudAnchor anchor : HudAnchor.values()) {
            if (anchor.getBounds().intersects(widget.getBounds().expand(5.0F))) {
               this.getAnchorGroup(anchor).getWidgets().addLast(widget);
               widget.setAnchor(anchor);
               this.finishLayoutEdit();
               return true;
            }
         }

         // A free widget that is not snapped to an anchor must remain in the free group.
         freeGroup.getWidgets().addLast(widget);
         widget.setAnchor(HudAnchor.NONE);
         this.finishLayoutEdit();
         return true;
      }

      return released;
   }

   private ModuleComponent findModuleRow(HudWidgetRenderer widget) {
      for (Object child : this.modulePanel.getChildren()) {
         if (child instanceof ModuleComponent row && row.getModule() == widget.getWidgetModule()) {
            return row;
         }
      }
      return null;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      HudAnchor dragAnchor = null;
      HudWidgetRenderer dragCandidate = null;

      for (HudAnchorGroup group : anchorGroups) {
         for (int widgetIndex = group.getWidgets()
                  .size()
               - 1;
            widgetIndex >= 0;
            widgetIndex--
         ) {
            HudWidgetRenderer widget = (HudWidgetRenderer)group.getWidgets()
               .get(widgetIndex);
            if (widget.isEnabled()
               && dragCandidate == null
               && widget.isMouseOver(event.x(), event.y())
               && !(
                  widget instanceof ResizableHudWidgetRenderer resizableWidget
                     && resizableWidget.isResizeHandleHovered(
                        event.x(), event.y()
                     )
               )) {
               if (event.button() == 0) {
                  this.beginLayoutEdit();
                  dragAnchor = group.getAnchor();
                  dragCandidate = widget;
                  this.dragOffsetX = event.x() - widget.getX();
                  this.dragOffsetY = event.y() - widget.getY();
                  this.draggedWidget = widget;
                  widget.setSelected(true);
                  widget.resetSnapping();
               } else if (event.button() == 1) {
                  ModuleComponent moduleRow = this.findModuleRow(widget);
                  if (moduleRow != null) {
                     moduleRow.toggleOpen();
                  }
               }
            }
         }
      }

      if (dragCandidate != null) {
         this.getAnchorGroup(
               dragAnchor
            )
            .getWidgets()
            .remove(
               dragCandidate
            );
         this.getAnchorGroup(HudAnchor.NONE)
            .getWidgets()
            .addLast(
               dragCandidate
            );
         dragCandidate
            .setAnchor(HudAnchor.NONE);
      } else {
         this.marginController
            .mouseClicked(
               event.x(), event.y(), event.button()
            );
      }

      return super.mouseClicked(event, doubleClick);
   }

   private void snapWidgetAxis(
      PoseStack poseStack, HudWidgetRenderer widget, float widgetSize, float screenSize, float margin, boolean horizontal
   ) {
      float position = horizontal ? widget.getX() : widget.getY();
      HudWidgetSnap.Target target = HudWidgetSnap.resolve(position, widgetSize, screenSize, margin, 5.0F);
      if (target == HudWidgetSnap.Target.NONE) {
         return;
      }

      float snappedPosition = HudWidgetSnap.position(target, widgetSize, screenSize, margin);
      switch (target) {
         case CENTER -> {
            this.renderSnapGuide(poseStack, snappedPosition - 0.5F, horizontal);
            this.renderSnapGuide(poseStack, snappedPosition + widgetSize, horizontal);
         }
         case LEADING -> this.renderSnapGuide(poseStack, snappedPosition - 0.5F, horizontal);
         case TRAILING -> this.renderSnapGuide(poseStack, snappedPosition + widgetSize, horizontal);
         case NONE -> {
         }
      }

      if (horizontal) {
         widget.setX(snappedPosition, true);
      } else {
         widget.setY(snappedPosition, true);
      }
   }

   private void snapWidget(
      GuiGraphics graphics, PoseStack poseStack, HudWidgetRenderer widget, float[] size
   ) {
      float screenWidth = graphics.guiWidth();
      float screenHeight = graphics.guiHeight();
      this.snapWidgetAxis(
         poseStack,
         widget,
         size[0],
         screenWidth,
         hud.getSafeX(),
         true
      );
      this.snapWidgetAxis(
         poseStack,
         widget,
         size[1],
         screenHeight,
         hud.getSafeY(),
         false
      );
   }

   private void renderSnapGuide(PoseStack poseStack, double position, boolean vertical) {
      int screenWidth = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth();
      int screenHeight = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight();
      float coordinate = (float)position;
      Color guideColor = ColorUtils.withAlpha(ColorUtils.getThemeColor(), 190);
      if (vertical) {
         GuiDrawUtils.fillRect(
            poseStack,
            coordinate,
            0.0F,
            coordinate
               + 0.5F,
            screenHeight,
            guideColor
         );
      } else {
         GuiDrawUtils.fillRect(
            poseStack,
            0.0F,
            coordinate,
            screenWidth,
            coordinate
               + 0.5F,
            guideColor
         );
      }
   }

   private void swapAnchorContents(HudAnchor firstAnchor, HudAnchor secondAnchor) {
      HudAnchorGroup firstGroup = this.getAnchorGroup(firstAnchor);
      HudAnchorGroup secondGroup = this.getAnchorGroup(secondAnchor);
      ArrayList firstWidgets = new ArrayList(
         firstGroup.getWidgets()
      );
      firstGroup.getWidgets()
         .clear();

      for (Object secondWidgetElement : secondGroup.getWidgets()) {
         HudWidgetRenderer secondWidget = (HudWidgetRenderer)secondWidgetElement;
         firstGroup.getWidgets()
            .add(secondWidget);
      }

      secondGroup.getWidgets()
         .clear();

      for (Object firstWidgetElement : firstWidgets) {
         HudWidgetRenderer firstWidget = (HudWidgetRenderer)firstWidgetElement;
         secondGroup.getWidgets().add(firstWidget);
      }

      for (Object widgetElement : firstGroup.getWidgets()) {
         ((HudWidgetRenderer)widgetElement).setAnchor(firstAnchor);
      }
      for (Object widgetElement : secondGroup.getWidgets()) {
         ((HudWidgetRenderer)widgetElement).setAnchor(secondAnchor);
      }
   }

   public void addWidget(HudAnchor anchor, HudWidgetRenderer widget) {
      this.getAnchorGroup(anchor)
         .getWidgets()
         .add(widget);
   }

   public void addWidget(HudWidgetRenderer widget) {
      this.addWidget(HudAnchor.NONE, widget);
   }

   public HudAnchorGroup getAnchorGroup(HudAnchor anchor) {
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         if (group.getAnchor()
            == anchor) {
            return group;
         }
      }

      return null;
   }

   public void renderWidgets(
      PoseStack poseStack, double mouseX, double mouseY
   ) {
      if (!hud.bloomSetting.getValue()) {
         this.renderWidgetContents(poseStack, mouseX, mouseY);
         return;
      }

      GuiDrawUtils.flush();
      MaskShaderRenderer.setParameters(
         hud.bloomRadiusSetting.getValue(), hud.bloomStrengthSetting.getValue(), 1.0F, 1.0F
      );
      RenderTarget mainTarget = ClientAccess.schizoMinecraft.getMainRenderTarget();
      try {
         MaskShaderRenderer.captureMask(() -> {
            this.renderWidgetContents(poseStack, mouseX, mouseY);
            GuiDrawUtils.flush();
         }, Color.black);
         MaskShaderRenderer.renderEffect("glowy");
         RenderUtils.blitToMain(MaskShaderRenderer.MASK_TARGET, ClientRenderPipelines.BLIT_PREMULTIPLIED_ALPHA);
      } finally {
         RenderTargetUtils.setMainRenderTarget(mainTarget);
      }
   }

   private void renderWidgetContents(
      PoseStack poseStack, double mouseX, double mouseY
   ) {
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         group.render(poseStack, mouseX, mouseY);
      }

      if (this.directionsRefreshPending) {
         for (HudAnchorGroup groupToRefresh : anchorGroups) {
            for (Object widgetElement : groupToRefresh.getWidgets()) {
               HudWidgetRenderer widget = (HudWidgetRenderer)widgetElement;
               widget.updateDirections();
            }
         }

         this.directionsRefreshPending = false;
      }
   }

   public void removeWidget(HudWidgetRenderer widget) {
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         if (group.getWidgets()
            .remove(widget)) {
            return;
         }
      }
   }

   @Override
   protected void init() {
      super.init();
      this.centerPanelPending = true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      boolean overPanel = false;

      for (Object panelElement : this.panels) {
         Panel panel = (Panel)panelElement;
         if (panel.isMouseOver(this.mouseX, this.mouseY)) {
            overPanel = true;
            break;
         }
      }

      if (!overPanel) {
         if (KeyUtils.isControlDown() && event.key() == GLFW.GLFW_KEY_Z) {
            if (KeyUtils.isShiftDown()) {
               this.redoLayoutEdit();
            } else {
               this.undoLayoutEdit();
            }
            return true;
         }
         if (KeyUtils.isControlDown() && event.key() == GLFW.GLFW_KEY_Y) {
            this.redoLayoutEdit();
            return true;
         }

         HudWidgetRenderer widget = this.findHoveredWidget();
         IntPair offset = this.getArrowKeyOffset(event.key());
         if (offset != null && widget != null && widget.isEnabled()) {
            this.beginLayoutEdit();
            float currentX = widget.getX();
            float currentY = widget.getY();
            if (widget.getAnchor() != HudAnchor.NONE) {
               this.removeWidget(widget);
               this.addWidget(HudAnchor.NONE, widget);
               widget.setAnchor(HudAnchor.NONE);
            }
            widget.resetSnapping();
            widget.setX(currentX + offset.first(), true);
            widget.setY(currentY + offset.second(), true);
            widget.updateDirections();
            this.finishLayoutEdit();
         } else {
            switch (event.key()) {
               case 73:
                  if (!(ClientContext.getClickGui().getEditingSetting() instanceof StringSettingComponent)) {
                     this.beginLayoutEdit();
                     this.swapAnchorContents(HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_RIGHT);
                     this.swapAnchorContents(HudAnchor.TOP_LEFT, HudAnchor.TOP_RIGHT);
                     this.requestDirectionRefresh();
                     this.finishLayoutEdit();
                  }
                  break;
               case 259:
               case 261:
                  if (widget != null && widget.isEnabled()) {
                     widget.getWidgetModule().setToggled(false);
                  }
            }
         }
      }

      return super.keyPressed(event);
   }

   @Override
   public void onClose() {
      super.onClose();
      if (this.draggedWidget != null) {
         this.draggedWidget.setSelected(false);
         this.draggedWidget = null;
      }
      ResizableHudWidgetRenderer.cancelActiveResize();
      this.cancelLayoutEdit();
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      PoseStack poseStack = GuiProjectionUtils.toPoseStack(graphics);
      RenderUtils.setRenderColor(
         1.0F,
         1.0F,
         1.0F,
         this.getTransitionOpacity()
      );
      hoverLabel.setVisible(false);
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      float screenCenterX = graphics.guiWidth()
         / 2.0F;
      float screenCenterY = graphics.guiHeight()
         / 2.0F;
      Color guideColor = ColorUtils.withAlpha(
         Color.white,
         (int)(
            RenderUtils.getRenderColor()[3]
               * 0.4F
         )
      );
      GuiDrawUtils.fillRect(
         poseStack,
         screenCenterX
            - 0.5F,
         0.0F,
         screenCenterX
            + 0.5F,
         screenCenterY
            * 2.0F,
         guideColor
      );
      GuiDrawUtils.fillRect(
         poseStack,
         0.0F,
         screenCenterY
            - 0.5F,
         screenCenterX
            - 0.5F,
         screenCenterY
            + 0.5F,
         guideColor
      );
      GuiDrawUtils.fillRect(
         poseStack,
         screenCenterX
            + 0.5F,
         screenCenterY - 0.5F,
         screenCenterX
            * 2.0F,
         screenCenterY
            + 0.5F,
         guideColor
      );
      this.marginController
         .render(poseStack, mouseX, mouseY);
      this.renderWidgets(
         poseStack,
         mouseX,
         mouseY
      );
      if (this.draggedWidget != null) {
         HudWidgetRenderer widget = this.draggedWidget;
         this.snapWidget(graphics, poseStack, widget, widget.getDimensions());
      }

      float labelOpacity = this.hoverLabelOpacity
         .updateAndGet();
      this.hoverLabelOpacity
         .setTarget(hoverLabel.isVisible());
      if (hoverLabel.isVisible()
         || labelOpacity > 0.1D) {
         FontRenderer.INSTANCE
            .drawWithShadow(
               poseStack,
               hoverLabel.getText(),
               hoverLabel.getX()
                  + 1e+01F
                     * (
                        1.0F
                           - labelOpacity
                     )
                     * hoverLabel.getHorizontalDirection(),
               hoverLabel.getY(),
               ColorUtils.withAlpha(
                  Color.white,
                  (int)(
                     labelOpacity
                        * 255.0F
                  )
               )
            );
      }

      super.render(graphics, mouseX, mouseY, partialTick);
      if (this.centerPanelPending) {
         this.centerModulePanel();
         this.centerPanelPending = false;
      }
   }

   @Override
   public void renderBackgroundOverlay(PoseStack poseStack) {
   }

   public void requestDirectionRefresh() {
      this.directionsRefreshPending = true;
   }

   private HudWidgetRenderer findHoveredWidget() {
      for (HudAnchorGroup group : anchorGroups) {
         for (Object widgetElement : group.getWidgets()) {
            HudWidgetRenderer widget = (HudWidgetRenderer)widgetElement;
            if (widget.isMouseOver(
               this.mouseX,
               this.mouseY
            )) {
               return widget;
            }
         }
      }

      return null;
   }

   private void centerModulePanel() {
      Panel panel = (Panel)this.panels
         .getFirst();
      int panelWidth = panel.getWidth();
      int panelHeight = panel.getHeight();
      int panelX = (int)(
         ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth()
               / 2.0F
            - panelWidth
               * 1.5F
      );
      int panelY = (int)(
         (ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight() - panelHeight)
            / 2.0F
      );
      panel.setX(
         panelX
      );
      panel.setY(
         panelY
      );
   }

   public List<HudAnchorGroup> getAnchorGroups() {
      return anchorGroups;
   }

}
