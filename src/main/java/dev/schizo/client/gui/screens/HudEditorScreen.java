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
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.render.ClientRenderPipelines;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.utils.collections.IntPair;
import dev.schizo.client.utils.math.FloatBounds;
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
import dev.schizo.client.render.HudEditorGrid;
import dev.schizo.client.render.HudGroupAlignment;
import dev.schizo.client.render.HudGroupMove;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends ClientScreen implements ClientAccess {

   private final HUDModule hud;
   private final List<HudAnchorGroup> anchorGroups = new ArrayList<>();
   public static final HudHoverLabel hoverLabel = new HudHoverLabel();
   private final HudMarginController marginController = new HudMarginController();
   private final ClampedInterpolatedFloat hoverLabelOpacity = new ClampedInterpolatedFloat(
      2.0F, true
   );
   private final HudModulePanel modulePanel = new HudModulePanel();
   private HudWidgetRenderer draggedWidget;
   private GroupDrag groupDrag;
   private boolean groupDragMoved;

   private record GroupDrag(List<HudWidgetRenderer> targets, List<HudGroupMove.Bounds> start,
                            double pointerX, double pointerY, int gridStep) {}
   private ResizableHudWidgetRenderer resizedWidget;
   private boolean centerPanelPending;
   private boolean directionsRefreshPending;
   private final List<HudWidgetRenderer> selection = new ArrayList<>();
   private boolean toolbarVisible = true;
   private HudToolbar.Action pressedToolbarAction;
   private int gridStep;
   private int dragGridStep;
   private double dragOffsetX;
   private double dragOffsetY;
   private float mouseX;
   private float mouseY;
   private static final int LAYOUT_HISTORY_CAPACITY = 100;
   private final HudLayoutHistory<LayoutSnapshot> layoutHistory =
      new HudLayoutHistory<>(LAYOUT_HISTORY_CAPACITY);

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
      this.applyLayoutPreset(layout, true);
   }

   private void applyLayoutPreset(JsonObject layout, boolean recordHistory) {
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

      if (recordHistory) this.beginLayoutEdit();
      try {
         this.restoreLayout(new LayoutSnapshot(List.copyOf(restoredStates)));
      } catch (RuntimeException failure) {
         try { this.restoreLayout(current); }
         catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
         if (recordHistory) this.layoutHistory.cancelEdit();
         throw failure;
      }
      if (recordHistory) this.finishLayoutEdit();
   }

   public LayoutPreview beginLayoutPreview(JsonObject layout) {
      this.cancelPendingLayoutGesture();
      LayoutSnapshot before = this.captureLayout();
      this.applyLayoutPreset(layout, false);
      return new LayoutPreview(new HudLayoutPreview<>(before, this.captureLayout(), HudEditorScreen::sameEditableLayout));
   }

   private static boolean sameEditableLayout(LayoutSnapshot first, LayoutSnapshot second) {
      if (first.widgets().size() != second.widgets().size()) return false;
      for (int index = 0; index < first.widgets().size(); index++) {
         WidgetLayoutState a = first.widgets().get(index), b = second.widgets().get(index);
         if (a.widget() != b.widget() || a.groupAnchor() != b.groupAnchor()
            || a.horizontalAlignment() != b.horizontalAlignment() || a.verticalAlignment() != b.verticalAlignment()
            || Float.compare(a.scale(), b.scale()) != 0) return false;
         // Anchors/alignment recompute these coordinates during rendering as widget sizes change.
         if (a.groupAnchor() == HudAnchor.NONE) {
            if (a.horizontalAlignment() == HudHorizontalAlignment.FREE && Float.compare(a.relativeX(), b.relativeX()) != 0) return false;
            if (a.verticalAlignment() == HudVerticalAlignment.FREE && Float.compare(a.relativeY(), b.relativeY()) != 0) return false;
         }
      }
      return true;
   }

   public final class LayoutPreview {
      private final HudLayoutPreview<LayoutSnapshot> preview;
      private LayoutPreview(HudLayoutPreview<LayoutSnapshot> preview) { this.preview = preview; }
      public boolean commit() { return this.preview.commit(captureLayout(), layoutHistory); }
      public void cancel() {
         LayoutSnapshot previous = this.preview.cancel(captureLayout());
         if (previous != null) restoreLayout(previous);
      }
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
      if (!this.layoutHistory.hasPendingEdit()) {
         this.layoutHistory.beginEdit(this.captureLayout());
      }
   }

   /** Commits one layout gesture, recording it only if something actually changed. */
   public void finishLayoutEdit() {
      if (this.layoutHistory.hasPendingEdit()) {
         this.layoutHistory.finishEdit(this.captureLayout());
      }
   }

   /** Cancels the in-progress gesture and restores its starting layout. */
   public void cancelLayoutEdit() {
      LayoutSnapshot previous = this.layoutHistory.cancelEdit();
      if (previous != null) {
         this.restoreLayout(previous);
      }
   }

   private boolean cancelPendingLayoutGesture() {
      if (!this.layoutHistory.hasPendingEdit()) {
         return false;
      }

      if (this.draggedWidget != null) {
         this.draggedWidget.setSelected(false);
         this.draggedWidget = null;
      }

      this.resizedWidget = null;
      this.groupDrag = null;
      this.groupDragMoved = false;
      this.cancelLayoutEdit();
      return true;
   }

   private void undoLayoutEdit() {
      if (this.cancelPendingLayoutGesture()) {
         return;
      }
      if (!this.layoutHistory.canUndo()) {
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
      if (!this.layoutHistory.canRedo()) {
         return;
      }
      LayoutSnapshot next = this.layoutHistory.redo(this.captureLayout());
      if (next != null) {
         this.restoreLayout(next);
      }
   }

   public boolean isResizing(ResizableHudWidgetRenderer widget) {
      return this.resizedWidget == widget;
   }

   private boolean pointerOverPanel(double x, double y) {
      double scale = GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
      for (Panel panel : this.panels) {
         if (panel.isMouseOver(x / scale, y / scale)) {
            return true;
         }
      }
      return false;
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
      this.hud = java.util.Objects.requireNonNull(
         ClientContext.getModule(HUDModule.class),
         "HUD module must be registered before creating the HUD editor"
      );
      this.panels
         .add(this.modulePanel);

      for (HudAnchor anchor : HudAnchor.values()) {
         this.anchorGroups.add(new HudAnchorGroup(anchor));
      }
   }

   private void applyDragSnapping(HudWidgetRenderer widget, float[] size, float screenWidth, float screenHeight) {
      if (this.dragGridStep > 0) {
         widget.setX(HudEditorGrid.snap(widget.getX(), screenWidth, size[0], this.dragGridStep), true);
         widget.setY(HudEditorGrid.snap(widget.getY(), screenHeight, size[1], this.dragGridStep), true);
         return;
      }
      float snappedX = HudWidgetSnap.snap(widget.getX(), size[0], screenWidth, hud.getSafeX(), 5.0F);
      float snappedY = HudWidgetSnap.snap(widget.getY(), size[1], screenHeight, hud.getSafeY(), 5.0F);
      widget.setX(snappedX, true);
      widget.setY(snappedY, true);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.pressedToolbarAction != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
      if (this.groupDrag != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         this.updateGroupDrag(event.x(), event.y());
         return true;
      }
      if (this.resizedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (this.resizedWidget.isEnabled()) {
            this.resizedWidget.resizeTo(event.x(), event.y());
         } else {
            this.cancelPendingLayoutGesture();
         }
         return true;
      }
      if (this.draggedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (!this.draggedWidget.isEnabled()) {
            this.cancelPendingLayoutGesture();
            return true;
         }
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
      if (this.pressedToolbarAction != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         HudToolbar.Action pressed = this.pressedToolbarAction;
         this.pressedToolbarAction = null;
         if (!this.closing && this.toolbarLayout().hit(event.x(), event.y()) == pressed && this.toolbarEnabled(pressed)) {
            this.runToolbarAction(pressed);
         }
         return true;
      }
      if (this.groupDrag != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         this.updateGroupDrag(event.x(), event.y());
         if (this.groupDrag != null) {
            if (this.groupDragMoved) {
               this.groupDrag = null;
               this.groupDragMoved = false;
               this.finishLayoutEdit();
            } else this.cancelPendingLayoutGesture();
         }
         return true;
      }
      if (this.resizedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (this.resizedWidget.isEnabled()) {
            this.resizedWidget = null;
            this.finishLayoutEdit();
         } else {
            this.cancelPendingLayoutGesture();
         }
         return true;
      }
      this.marginController.mouseReleased(event.button());
      boolean released = super.mouseReleased(event);
      if (this.draggedWidget != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (!this.draggedWidget.isEnabled()) {
            this.cancelPendingLayoutGesture();
            return true;
         }
         HudWidgetRenderer widget = this.draggedWidget;
         int screenWidth = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth();
         int screenHeight = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight();
         HudAnchorGroup freeGroup = this.getAnchorGroup(HudAnchor.NONE);
         HudAnchorGroup destination = freeGroup;
         if (this.dragGridStep == 0) {
            widget.updateSnapping(screenWidth, screenHeight, 5.0F);
            destination = this.findDropGroup(widget);
         } else {
            widget.resetSnapping();
         }
         if (destination != freeGroup) {
            destination.getWidgets().addLast(widget);
            freeGroup.getWidgets().remove(widget);
         }
         widget.setAnchor(destination.getAnchor());
         widget.setSelected(false);
         this.draggedWidget = null;
         this.finishLayoutEdit();
         return true;
      }

      return released;
   }

   private HudAnchorGroup findDropGroup(HudWidgetRenderer dragged) {
      FloatBounds bounds = dragged.getBounds();
      for (HudAnchorGroup group : this.anchorGroups) {
         for (Object entry : group.getWidgets()) {
            HudWidgetRenderer other = (HudWidgetRenderer)entry;
            if (other != dragged && other.getBounds().intersects(bounds)) {
               return group;
            }
         }
      }

      FloatBounds snapBounds = bounds.expand(5.0F);
      for (HudAnchor anchor : HudAnchor.values()) {
         if (anchor.getBounds().intersects(snapBounds)) {
            return this.getAnchorGroup(anchor);
         }
      }
      return this.getAnchorGroup(HudAnchor.NONE);
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
      if (this.closing || this.draggedWidget != null || this.resizedWidget != null || this.groupDrag != null) {
         return true;
      }
      HudToolbar.Layout toolbar = this.toolbarLayout();
      if (toolbar.contains(event.x(), event.y())) {
         if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.pruneSelection();
            this.pressedToolbarAction = toolbar.hit(event.x(), event.y());
            if (this.pressedToolbarAction == null) this.pressedToolbarAction = HudToolbar.Action.BACKGROUND;
         }
         return true;
      }
      if (this.pointerOverPanel(event.x(), event.y())) {
         return super.mouseClicked(event, doubleClick);
      }
      HudWidgetRenderer widget = this.findHoveredWidget(event.x(), event.y());
      if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && KeyUtils.isControlDown()) {
         if (widget == null) this.selection.clear();
         else if (!this.selection.remove(widget)) this.selection.add(widget);
         return true;
      }
      this.pruneSelection();
      if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.selection.size() >= 2
         && this.selection.contains(widget)
         && !(widget instanceof ResizableHudWidgetRenderer resizable && resizable.isResizeHandleHovered(event.x(), event.y()))) {
         this.startGroupDrag(event.x(), event.y());
         return true;
      }
      if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         this.selection.clear();
         if (widget != null) this.selection.add(widget);
      }
      if (widget != null && (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
         if (widget instanceof ResizableHudWidgetRenderer resizable && resizable.isResizeHandleHovered(event.x(), event.y())) {
            this.beginLayoutEdit();
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
               resizable.setSizeScale(1.0F);
               this.finishLayoutEdit();
            } else {
               this.resizedWidget = resizable;
            }
         } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.beginLayoutEdit();
            this.dragGridStep = this.gridStep;
            this.dragOffsetX = event.x() - widget.getX();
            this.dragOffsetY = event.y() - widget.getY();
            this.getAnchorGroup(widget.getAnchor()).getWidgets().remove(widget);
            this.addWidget(HudAnchor.NONE, widget);
            widget.setAnchor(HudAnchor.NONE);
            widget.setSelected(true);
            widget.resetSnapping();
            this.draggedWidget = widget;
         } else {
            ModuleComponent row = this.findModuleRow(widget);
            if (row != null) {
               row.toggleOpen();
            }
         }
         return true;
      }
      this.marginController.mouseClicked(event.x(), event.y(), event.button());

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

      // Guides are visual only. Drag input owns position changes.
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
         RenderUtils.blitToMain(MaskShaderRenderer.maskTarget(), ClientRenderPipelines.BLIT_PREMULTIPLIED_ALPHA);
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
         for (HudAnchorGroup groupToRefresh : this.anchorGroups) {
            for (Object widgetElement : groupToRefresh.getWidgets()) {
               HudWidgetRenderer widget = (HudWidgetRenderer)widgetElement;
               widget.updateDirections();
            }
         }

         this.directionsRefreshPending = false;
      }
   }

   public void removeWidget(HudWidgetRenderer widget) {
      this.selection.remove(widget);
      if (this.draggedWidget == widget || this.resizedWidget == widget || this.groupDrag != null && this.groupDrag.targets().contains(widget)) {
         this.cancelPendingLayoutGesture();
      }
      for (HudAnchorGroup group : this.getAnchorGroups()) {
         if (group.getWidgets()
            .remove(widget)) {
            return;
         }
      }
   }

   @Override
   protected void init() {
      this.pressedToolbarAction = null;
      this.selection.clear();
      this.cancelPendingLayoutGesture();
      this.marginController.mouseReleased(GLFW.GLFW_MOUSE_BUTTON_LEFT);
      super.init();
      this.centerPanelPending = true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.pressedToolbarAction != null) {
         if (event.key() == GLFW.GLFW_KEY_ESCAPE) this.pressedToolbarAction = HudToolbar.Action.BACKGROUND;
         return true;
      }
      if (this.focusedSetting() != null) {
         return super.keyPressed(event);
      }
      if (event.key() == GLFW.GLFW_KEY_T && !KeyUtils.isControlDown() && !KeyUtils.isShiftDown()
         && !this.layoutHistory.hasPendingEdit() && this.pressedToolbarAction == null) {
         this.toolbarVisible = !this.toolbarVisible;
         return true;
      }
      if (!this.pointerOverPanel(this.mouseX, this.mouseY)) {
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

         if (this.groupDrag != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.cancelPendingLayoutGesture();
            return true;
         }
         if (this.draggedWidget != null || this.resizedWidget != null || this.groupDrag != null) {
            return super.keyPressed(event);
         }
         if (event.key() == GLFW.GLFW_KEY_G && !KeyUtils.isControlDown() && !KeyUtils.isShiftDown()) {
            this.gridStep = HudEditorGrid.next(this.gridStep);
            return true;
         }
         HudWidgetRenderer widget = this.findHoveredWidget(this.mouseX, this.mouseY);
         IntPair offset = this.getArrowKeyOffset(event.key());
         this.pruneSelection();
         if (KeyUtils.isControlDown() && KeyUtils.isShiftDown()) {
            HudGroupAlignment.Mode mode = switch (event.key()) {
               case GLFW.GLFW_KEY_LEFT -> HudGroupAlignment.Mode.LEFT;
               case GLFW.GLFW_KEY_RIGHT -> HudGroupAlignment.Mode.RIGHT;
               case GLFW.GLFW_KEY_UP -> HudGroupAlignment.Mode.TOP;
               case GLFW.GLFW_KEY_DOWN -> HudGroupAlignment.Mode.BOTTOM;
               case GLFW.GLFW_KEY_H -> HudGroupAlignment.Mode.CENTER_X;
               case GLFW.GLFW_KEY_V -> HudGroupAlignment.Mode.CENTER_Y;
               case GLFW.GLFW_KEY_X -> HudGroupAlignment.Mode.DISTRIBUTE_X;
               case GLFW.GLFW_KEY_D -> HudGroupAlignment.Mode.DISTRIBUTE_Y;
               default -> null;
            };
            if (mode != null) {
               if (this.selection.size() >= 2) this.alignSelection(mode);
               return true;
            }
         }
         if (offset != null && (!this.selection.isEmpty() || widget != null && widget.isEnabled())) {
            List<HudWidgetRenderer> targets = this.selection.isEmpty() ? List.of(widget) : List.copyOf(this.selection);
            this.moveSelection(targets, offset.first(), offset.second());
            return true;
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

   private void pruneSelection() {
      this.selection.removeIf(widget -> !widget.isEnabled() || this.anchorGroups.stream()
         .noneMatch(group -> group.getWidgets().contains(widget)));
   }

   private void alignSelection(HudGroupAlignment.Mode mode) {
      var window = ClientAccess.schizoMinecraft.getWindow();
      List<HudWidgetRenderer> targets = List.copyOf(this.selection);
      List<HudGroupMove.Bounds> bounds = new ArrayList<>();
      for (HudWidgetRenderer widget : targets) {
         float[] size = widget.getDimensions();
         bounds.add(new HudGroupMove.Bounds(widget.getX(), widget.getY(), size[0], size[1]));
      }
      var positions = HudGroupAlignment.plan(bounds, window.getGuiScaledWidth(), window.getGuiScaledHeight(), mode);
      if (positions.isEmpty()) return;
      boolean changed = false;
      for (int i = 0; i < targets.size(); i++) {
         var old = bounds.get(i);
         var next = positions.get(i);
         if (Math.abs(next.x() - old.x()) > 0.001F || Math.abs(next.y() - old.y()) > 0.001F) changed = true;
      }
      if (!changed) return;
      this.beginLayoutEdit();
      for (int i = 0; i < targets.size(); i++) {
         var next = positions.get(i);
         // Even an unchanged anchored member can shift when another member leaves its stack.
         HudWidgetRenderer widget = targets.get(i);
         if (widget.getAnchor() != HudAnchor.NONE) {
            this.getAnchorGroup(widget.getAnchor()).getWidgets().remove(widget);
            this.addWidget(HudAnchor.NONE, widget);
            widget.setAnchor(HudAnchor.NONE);
         }
         widget.resetSnapping();
         widget.setX(next.x(), true);
         widget.setY(next.y(), true);
         widget.updateDirections();
      }
      this.requestDirectionRefresh();
      this.finishLayoutEdit();
   }

   private void startGroupDrag(double x, double y) {
      List<HudWidgetRenderer> targets = List.copyOf(this.selection);
      List<HudGroupMove.Bounds> start = new ArrayList<>();
      for (HudWidgetRenderer widget : targets) {
         float[] size = widget.getDimensions();
         start.add(new HudGroupMove.Bounds(widget.getX(), widget.getY(), size[0], size[1]));
      }
      if (!Double.isFinite(x) || !Double.isFinite(y)) return;
      this.beginLayoutEdit();
      this.groupDrag = new GroupDrag(targets, List.copyOf(start), x, y, this.gridStep);
      this.groupDragMoved = false;
   }

   private boolean groupDragValid() {
      return this.groupDrag == null || this.groupDrag.targets().stream().allMatch(widget ->
         widget.isEnabled() && this.anchorGroups.stream().anyMatch(group -> group.getWidgets().contains(widget)));
   }

   private void updateGroupDrag(double pointerX, double pointerY) {
      GroupDrag drag = this.groupDrag;
      if (drag == null) return;
      if (!this.groupDragValid()) {
         this.cancelPendingLayoutGesture();
         return;
      }
      var window = ClientAccess.schizoMinecraft.getWindow();
      float width = window.getGuiScaledWidth(), height = window.getGuiScaledHeight();
      List<HudGroupMove.Bounds> currentExtents = new ArrayList<>();
      for (int i = 0; i < drag.targets().size(); i++) {
         float[] size = drag.targets().get(i).getDimensions();
         var start = drag.start().get(i);
         currentExtents.add(new HudGroupMove.Bounds(start.x(), start.y(), size[0], size[1]));
      }
      var delta = dev.schizo.client.render.HudGroupDrag.plan(currentExtents, width, height,
         pointerX - drag.pointerX(), pointerY - drag.pointerY(), drag.gridStep());
      if (delta == null) {
         this.cancelPendingLayoutGesture();
         return;
      }
      if (delta.x() == 0 && delta.y() == 0 && !this.groupDragMoved) return;
      for (int i = 0; i < drag.targets().size(); i++) {
         HudWidgetRenderer widget = drag.targets().get(i);
         var start = drag.start().get(i);
         if (widget.getAnchor() != HudAnchor.NONE) {
            this.getAnchorGroup(widget.getAnchor()).getWidgets().remove(widget);
            this.addWidget(HudAnchor.NONE, widget);
            widget.setAnchor(HudAnchor.NONE);
         }
         widget.resetSnapping();
         widget.setX(start.x() + delta.x(), true);
         widget.setY(start.y() + delta.y(), true);
         widget.updateDirections();
      }
      this.groupDragMoved = delta.x() != 0 || delta.y() != 0;
      // Returning to origin must restore original anchoring if released there.
      if (!this.groupDragMoved) {
         this.cancelLayoutEdit();
         this.beginLayoutEdit();
      }
      this.requestDirectionRefresh();
   }

   private void moveSelection(List<HudWidgetRenderer> targets, int dx, int dy) {
      var window = ClientAccess.schizoMinecraft.getWindow();
      float width = window.getGuiScaledWidth(), height = window.getGuiScaledHeight();
      List<dev.schizo.client.render.HudGroupMove.Bounds> bounds = new ArrayList<>();
      for (HudWidgetRenderer widget : targets) {
         float[] size = widget.getDimensions();
         bounds.add(new dev.schizo.client.render.HudGroupMove.Bounds(widget.getX(), widget.getY(), size[0], size[1]));
      }
      var delta = dev.schizo.client.render.HudGroupMove.clamp(bounds, width, height, dx, dy);
      if (delta.x() == 0 && delta.y() == 0) return;
      this.beginLayoutEdit();
      for (int i = 0; i < targets.size(); i++) {
         HudWidgetRenderer widget = targets.get(i);
         var original = bounds.get(i);
         if (widget.getAnchor() != HudAnchor.NONE) {
            this.getAnchorGroup(widget.getAnchor()).getWidgets().remove(widget);
            this.addWidget(HudAnchor.NONE, widget);
            widget.setAnchor(HudAnchor.NONE);
         }
         widget.resetSnapping();
         widget.setX(original.x() + delta.x(), true);
         widget.setY(original.y() + delta.y(), true);
         widget.updateDirections();
      }
      this.requestDirectionRefresh();
      this.finishLayoutEdit();
   }

   @Override
   public void removed() {
      try {
         this.pressedToolbarAction = null;
         this.selection.clear();
         this.cancelPendingLayoutGesture();
         this.marginController.mouseReleased(GLFW.GLFW_MOUSE_BUTTON_LEFT);
      } finally {
         super.removed();
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (!this.groupDragValid() || (this.draggedWidget != null && !this.draggedWidget.isEnabled())
         || (this.resizedWidget != null && !this.resizedWidget.isEnabled())) {
         this.cancelPendingLayoutGesture();
      }
      try {
         this.renderEditor(graphics, mouseX, mouseY, partialTick);
      } finally {
         RenderUtils.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void renderEditor(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
         Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, this.getTransitionOpacity())) * 0.18F)
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
      if (this.gridStep > 0) this.renderGrid(poseStack, graphics.guiWidth(), graphics.guiHeight());
      this.marginController
         .render(poseStack, mouseX, mouseY);
      this.renderWidgets(
         poseStack,
         mouseX,
         mouseY
      );
      if (this.draggedWidget != null && this.dragGridStep == 0) {
         HudWidgetRenderer widget = this.draggedWidget;
         this.snapWidget(graphics, poseStack, widget, widget.getDimensions());
      }

      this.pruneSelection();
      for (HudWidgetRenderer widget : this.selection) {
         float[] size = widget.getDimensions();
         float x = widget.getX(), y = widget.getY();
         if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(size[0]) || !Float.isFinite(size[1]) || size[0] <= 0 || size[1] <= 0) continue;
         Color edge = ColorUtils.withAlpha(ColorUtils.getThemeColor(), 180);
         GuiDrawUtils.fillRect(poseStack, x, y, x + size[0], y + 0.5F, edge);
         GuiDrawUtils.fillRect(poseStack, x, y + size[1] - 0.5F, x + size[0], y + size[1], edge);
         GuiDrawUtils.fillRect(poseStack, x, y, x + 0.5F, y + size[1], edge);
         GuiDrawUtils.fillRect(poseStack, x + size[0] - 0.5F, y, x + size[0], y + size[1], edge);
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
      this.renderToolbar(graphics, mouseX, mouseY);
      if (this.centerPanelPending) {
         this.centerModulePanel();
         this.centerPanelPending = false;
      }
   }

   private HudToolbar.Layout toolbarLayout() {
      var window = ClientAccess.schizoMinecraft.getWindow();
      return HudToolbar.layout(window.getGuiScaledWidth(), window.getGuiScaledHeight(), this.toolbarVisible);
   }

   private ResizableHudWidgetRenderer selectedResizable() {
      if (this.selection.size() != 1) return null;
      return this.selection.getFirst() instanceof ResizableHudWidgetRenderer widget
         && widget.isEnabled() && widget.supportsResizing() ? widget : null;
   }

   private boolean toolbarEnabled(HudToolbar.Action action) {
      if (this.closing || this.focusedSetting() != null || this.layoutHistory.hasPendingEdit()
         || this.draggedWidget != null || this.resizedWidget != null || this.groupDrag != null) return false;
      if (action.isScaleAction()) {
         var widget = this.selectedResizable();
         if (widget == null) return false;
         float current = widget.getSizeScale();
         return switch (action) {
            case SCALE_DOWN -> current > 0.5F;
            case SCALE_UP -> current < 2.0F;
            case SCALE_RESET -> Float.compare(current, 1.0F) != 0;
            default -> false;
         };
      }
      return action.enabled(this.selection.size(), this.layoutHistory.canUndo(), this.layoutHistory.canRedo());
   }

   private void runToolbarAction(HudToolbar.Action action) {
      this.pruneSelection();
      if (!this.toolbarEnabled(action)) return;
      switch (action) {
         case GRID -> this.gridStep = HudEditorGrid.next(this.gridStep);
         case UNDO -> this.undoLayoutEdit();
         case REDO -> this.redoLayoutEdit();
         case CLEAR -> this.selection.clear();
         case SCALE_DOWN, SCALE_RESET, SCALE_UP -> this.adjustSelectedScale(action);
         default -> {
            if (action.mode != null) this.alignSelection(action.mode);
         }
      }
   }

   private void adjustSelectedScale(HudToolbar.Action action) {
      var widget = this.selectedResizable();
      if (widget == null) return;
      float current = widget.getSizeScale();
      int points = KeyUtils.isShiftDown() ? 10 : 1;
      float next = action == HudToolbar.Action.SCALE_RESET ? 1.0F
         : dev.schizo.client.render.HudWidgetResize.adjustPercent(current, action == HudToolbar.Action.SCALE_UP ? points : -points);
      if (Float.compare(next, current) == 0) return;
      this.beginLayoutEdit();
      widget.setSizeScale(next);
      this.requestDirectionRefresh();
      this.finishLayoutEdit();
   }

   private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY) {
      HudToolbar.Layout layout = HudToolbar.layout(graphics.guiWidth(), graphics.guiHeight(), this.toolbarVisible);
      var font = ClientAccess.schizoMinecraft.font;
      if (!layout.visible()) {
         String hint = "T: HUD tools | G: grid | Ctrl-click: select";
         if (graphics.guiWidth() >= font.width(hint) + 16 && graphics.guiHeight() >= 20)
            graphics.drawString(font, hint, 8, graphics.guiHeight() - 12, 0xffb8b9c7, true);
         return;
      }
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), theme.panel());
      graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 1, theme.accent());
      String title = "HUD TOOLS  |  " + this.selection.size() + " selected";
      var resizable = this.selectedResizable();
      if (resizable != null) title += " | " + Math.round(resizable.getSizeScale() * 100) + "% (Shift: 10%)";
      title += " | T: hide";
      graphics.drawString(font, font.plainSubstrByWidth(title, layout.width() - 12), layout.x() + 6, layout.y() + 6, 0xffeeeeef, false);
      for (HudToolbar.Action action : HudToolbar.ACTIONS) {
         HudToolbar.Cell cell = layout.cell(action);
         boolean enabled = this.toolbarEnabled(action);
         boolean hovered = enabled && cell.contains(mouseX, mouseY);
         int surface = !enabled ? theme.panel() : hovered ? theme.hover() : theme.row();
         if (enabled && this.pressedToolbarAction == action && hovered) surface = theme.selected();
         graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(), enabled && hovered ? theme.accent() : 0xff444651);
         graphics.fill(cell.x() + 1, cell.y() + 1, cell.x() + cell.width() - 1, cell.y() + cell.height() - 1, surface);
         String label = action == HudToolbar.Action.GRID ? "Grid " + (this.gridStep == 0 ? "off" : this.gridStep) : action.label;
         label = font.plainSubstrByWidth(label, cell.width() - 6);
         graphics.drawString(font, label, cell.x() + (cell.width() - font.width(label)) / 2,
            cell.y() + (cell.height() - font.lineHeight) / 2, enabled ? 0xffeeeeef : 0xff898a96, false);
      }
   }

   private void renderGrid(PoseStack poseStack, int width, int height) {
      int step = HudEditorGrid.visualStep(this.gridStep, Math.max(width, height));
      if (step <= 0) return;
      Color color = ColorUtils.withAlpha(ColorUtils.getThemeColor(), 35);
      for (int x = step; x < width; x += step) GuiDrawUtils.fillRect(poseStack, x, 0, x + 0.5F, height, color);
      for (int y = step; y < height; y += step) GuiDrawUtils.fillRect(poseStack, 0, y, width, y + 0.5F, color);
   }

   @Override
   public void renderBackgroundOverlay(PoseStack poseStack) {
   }

   public void requestDirectionRefresh() {
      this.directionsRefreshPending = true;
   }

   private HudWidgetRenderer findHoveredWidget(double x, double y) {
      for (int groupIndex = this.anchorGroups.size() - 1; groupIndex >= 0; groupIndex--) {
         List widgets = this.anchorGroups.get(groupIndex).getWidgets();
         for (int index = widgets.size() - 1; index >= 0; index--) {
            HudWidgetRenderer widget = (HudWidgetRenderer)widgets.get(index);
            if (widget.isEnabled() && (widget.isMouseOver(x, y)
               || widget instanceof ResizableHudWidgetRenderer resizable && resizable.isResizeHandleHovered(x, y))) {
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
      double scale = GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
      double width = ClientAccess.schizoMinecraft.getWindow().getGuiScaledWidth() / scale;
      double height = ClientAccess.schizoMinecraft.getWindow().getGuiScaledHeight() / scale;
      panel.placeWithinViewport((int)(width / 2.0 - panelWidth * 1.5), (int)((height - panelHeight) / 2.0), width, height);
   }

   public List<HudAnchorGroup> getAnchorGroups() {
      return this.anchorGroups;
   }

}
