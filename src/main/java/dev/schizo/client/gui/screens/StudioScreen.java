package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.UiMotionPolicy;
import dev.schizo.mixin.EditBoxSelectionAccessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared entry treatment. Input geometry and screen lifecycle stay native. */
abstract class StudioScreen extends Screen {
   private final StudioResizeFocus<AbstractWidget> resizeFocus = new StudioResizeFocus<>();
   private boolean entryStarted;
   private long entryNanos;

   protected StudioScreen(Component title) {
      super(title);
   }

   @Override protected void init() {
      super.init();
      resizeFocus.clear();
      entryStarted = false;
   }

   /** Register a stable control role, independent of label changes and widget position. */
   protected final <T extends AbstractWidget> T trackResizeFocus(String id, T widget) {
      resizeFocus.register(id, widget);
      return widget;
   }

   @Override public void resize(int width, int height) {
      var focused = getFocused();
      String role = resizeFocus.roleOf(focused);
      StudioTextSelection selection = role != null && focused instanceof EditBox box
         ? new StudioTextSelection(box.getValue(), box.getCursorPosition(),
            ((EditBoxSelectionAccessor)box).schizo$getSelectionAnchor())
         : null;
      super.resize(width, height);
      AbstractWidget restored = resizeFocus.resolve(role, widget -> widget.active && widget.visible);
      if (restored != null) {
         setInitialFocus(restored);
         if (selection != null && restored instanceof EditBox box && selection.matches(box.getValue())) {
            box.setCursorPosition(selection.caret());
            box.setHighlightPos(selection.anchor());
         }
      }
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
