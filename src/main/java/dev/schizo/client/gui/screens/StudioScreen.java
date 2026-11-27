package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.UiMotionPolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared entry treatment. Input geometry and screen lifecycle stay native. */
abstract class StudioScreen extends Screen {
   private boolean entryStarted;
   private long entryNanos;

   protected StudioScreen(Component title) {
      super(title);
   }

   @Override protected void init() {
      super.init();
      entryStarted = false;
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
