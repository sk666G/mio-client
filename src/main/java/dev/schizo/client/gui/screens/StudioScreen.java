package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.UiMotionPolicy;
import dev.schizo.mixin.EditBoxSelectionAccessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Shared entry treatment. Input geometry and screen lifecycle stay native. */
abstract class StudioScreen extends Screen {
   private final StudioResizeFocus<AbstractWidget> resizeFocus = new StudioResizeFocus<>();
   private record FocusSnapshot(String role, StudioTextSelection selection) {}
   private final StudioReturnFocus<Screen,FocusSnapshot> returnFocus = new StudioReturnFocus<>();
   private final StudioRowHover rowHover = new StudioRowHover();
   private String rowHelpText;
   private int rowHelpWidth, rowHelpLimit;
   private List<FormattedCharSequence> rowHelpLines = List.of();
   private boolean entryStarted;
   private long entryNanos;

   protected StudioScreen(Component title) {
      super(title);
   }

   @Override protected void init() {
      super.init();
      resizeFocus.clear();
      rowHover.clear();
      rowHelpText = null;
      rowHelpLines = List.of();
      entryStarted = false;
   }

   /** Register a stable control role, independent of label changes and widget position. */
   protected final <T extends AbstractWidget> T trackResizeFocus(String id, T widget) {
      resizeFocus.register(id, widget);
      return widget;
   }

   private FocusSnapshot captureFocus() {
      var focused = getFocused();
      String role = resizeFocus.roleOf(focused);
      StudioTextSelection selection = role != null && focused instanceof EditBox box
         ? new StudioTextSelection(box.getValue(), box.getCursorPosition(),
            ((EditBoxSelectionAccessor)box).schizo$getSelectionAnchor())
         : null;
      return new FocusSnapshot(role, selection);
   }

   private void restoreFocus(FocusSnapshot snapshot) {
      if (snapshot == null) return;
      StudioTextSelection selection = snapshot.selection();
      AbstractWidget restored = resizeFocus.resolve(snapshot.role(), widget -> widget.active && widget.visible);
      if (restored != null) {
         setInitialFocus(restored);
         if (selection != null && restored instanceof EditBox box && selection.matches(box.getValue())) {
            box.setCursorPosition(selection.caret());
            box.setHighlightPos(selection.anchor());
         }
      }
   }

   @Override public void resize(int width, int height) {
      FocusSnapshot snapshot = captureFocus();
      boolean wasStarted = entryStarted;
      long previousStart = entryNanos;
      super.resize(width, height);
      entryStarted = wasStarted;
      entryNanos = previousStart;
      restoreFocus(snapshot);
   }

   protected final void openStudioChild(StudioScreen child) {
      child.returnFocus.remember(this, captureFocus());
      minecraft.setScreen(child);
   }

   protected final void returnToParent(Screen parent) {
      FocusSnapshot snapshot = returnFocus.take(parent);
      minecraft.setScreen(parent);
      if (parent instanceof StudioScreen studio && minecraft.screen == parent) {
         studio.restoreFocus(snapshot);
      }
   }

   /** Call once per frame with the actual visible row identity or null. */
   protected final boolean rowHelpReady(Object candidate) {
      return rowHover.ready(candidate, System.nanoTime());
   }

   protected final void showRowHelp(GuiGraphics graphics, String text, int x, int y) {
      if (text == null || text.isBlank() || width < 48 || height < 48) return;
      int wrap = Math.min(260, width - 24);
      int limit = Math.max(1, (height - 24) / Math.max(1, font.lineHeight + 2));
      if (!text.equals(rowHelpText) || rowHelpWidth != wrap || rowHelpLimit != limit) {
         rowHelpText = text;
         rowHelpWidth = wrap;
         rowHelpLimit = limit;
         var wrapped = font.split(Component.literal(text), wrap);
         if (wrapped.size() > limit) {
            var bounded = new java.util.ArrayList<FormattedCharSequence>(wrapped.subList(0, limit - 1));
            bounded.add(Component.literal("…").getVisualOrderText());
            rowHelpLines = List.copyOf(bounded);
         } else rowHelpLines = List.copyOf(wrapped);
      }
      if (!rowHelpLines.isEmpty()) graphics.setTooltipForNextFrame(font, rowHelpLines, x, y);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      long now = System.nanoTime();
      if (!entryStarted) {
         entryNanos = now;
         entryStarted = true;
      }
      if (UiMotionPolicy.isReducedMotion()) {
         // Do not resume an old transition if motion is re-enabled.
         entryNanos = now - StudioEntryMotion.DURATION_NANOS;
         return;
      }
      int alpha = StudioEntryMotion.veilAlpha(now - entryNanos);
      if (alpha > 0 && width > 0 && height > 0) {
         graphics.fill(0, 0, width, height, alpha << 24);
      }
   }
}
