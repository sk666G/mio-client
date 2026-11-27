package dev.schizo.client.gui.screens;

import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.profiling.BenchmarkCapture;
import dev.schizo.client.profiling.BenchmarkSession;
import dev.schizo.client.profiling.BenchmarkStore;
import dev.schizo.client.profiling.JvmMemorySnapshot;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Opt-in capture and two durable, local comparison slots. */
public final class BenchmarkCenterScreen extends Screen {
   private final Screen returnScreen;
   private final BenchmarkCapture capture = BenchmarkCapture.GLOBAL;
   private final BenchmarkStore store = new BenchmarkStore(ConfigurationManager.ROOT_DIRECTORY);
   private BenchmarkSession a, b;
   private JvmMemorySnapshot memory;
   private long refreshed;
   private String notice = "";
   private int left, top, panelWidth, graphY, graphHeight;
   private Button start, stop, saveA, saveB;

   public BenchmarkCenterScreen(Screen parent) {
      super(Component.literal("Schizo Benchmarks"));
      this.returnScreen = parent;
   }

   @Override protected void init() {
      panelWidth = Math.max(1, Math.min(560, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 270) / 2);
      graphY = top + 99;
      int buttonsY = Math.max(graphY + 12, height - top - 46);
      graphHeight = Math.max(1, buttonsY - graphY - 8);
      int w = Math.max(1, (panelWidth - 8) / 3);
      start = button("Capture 30s", left, buttonsY, w, () -> {
         if (minecraft.player == null) { notice = "Join a world to record gameplay."; return; }
         capture.start(System.nanoTime());
         minecraft.setScreen(null);
      });
      stop = button("Finish", left + w + 4, buttonsY, w, () -> {
         BenchmarkSession session = capture.finish();
         notice = session == null ? "No complete frame intervals recorded." : "Capture ready: " + session.frames() + " frames. Save A or B.";
         refreshControls();
      });
      button("Back", left + (w + 4) * 2, buttonsY, w, this::onClose);
      saveA = button("Save A", left, buttonsY + 22, w, () -> save(BenchmarkStore.Slot.A));
      saveB = button("Save B", left + w + 4, buttonsY + 22, w, () -> save(BenchmarkStore.Slot.B));
      button("Copy report", left + (w + 4) * 2, buttonsY + 22, w, () -> {
         minecraft.keyboardHandler.setClipboard("Schizo frame benchmarks (local)\n" + detail("A", a)
            + "\n" + detail("B", b) + "\n" + memoryLine() + "\n");
         notice = "Local report copied.";
      });
      loadSlots();
      refreshMemory();
      refreshControls();
   }

   private Button button(String label, int x, int y, int w, Runnable action) {
      return addRenderableWidget(Button.builder(Component.literal(label), ignored -> action.run())
         .bounds(x, y, w, 20).build());
   }

   private void loadSlots() {
      try { a = store.load(BenchmarkStore.Slot.A); }
      catch (IOException failure) { notice = "Slot A could not be read; file preserved."; }
      try { b = store.load(BenchmarkStore.Slot.B); }
      catch (IOException failure) { notice = "Slot B could not be read; file preserved."; }
   }

   private void save(BenchmarkStore.Slot slot) {
      if (capture.isActive()) return;
      BenchmarkSession session = capture.finish();
      if (session == null) { notice = "No completed capture."; return; }
      try {
         store.save(slot, session);
         if (slot == BenchmarkStore.Slot.A) a = session; else b = session;
         notice = "Saved slot " + slot + " (previous file backed up).";
      } catch (IOException failure) {
         notice = "Could not save slot " + slot + "; check local storage.";
      }
   }

   private void refreshControls() {
      boolean active = capture.isActive();
      start.active = !active && minecraft.player != null;
      stop.active = capture.hasCapture();
      saveA.active = !active && capture.hasCapture();
      saveB.active = saveA.active;
   }

   private void refreshMemory() {
      memory = JvmMemorySnapshot.capture();
      refreshed = System.nanoTime();
   }

   @Override public void tick() {
      refreshControls();
      if (System.nanoTime() - refreshed >= 1_000_000_000L) refreshMemory();
   }

   private String memoryLine() {
      if (memory == null) return "JVM heap: unavailable";
      return String.format(Locale.ROOT, "Heap: %d / %d MiB | GC: %s collections, %s ms cumulative",
         memory.usedBytes() / 1048576L, memory.maximumBytes() / 1048576L,
         memory.collections() < 0 ? "n/a" : Long.toString(memory.collections()),
         memory.collectionMillis() < 0 ? "n/a" : Long.toString(memory.collectionMillis()));
   }

   private static String detail(String slot, BenchmarkSession session) {
      if (session == null) return slot + " | Empty";
      return String.format(Locale.ROOT, "%s | %.1f FPS | p99 %.2f ms | max %.2f ms | %d hitches | %d frames / %.1fs measured",
         slot, session.averageFps(), session.p99Millis(), session.maxMillis(), session.hitches(), session.frames(), session.seconds());
   }

   private void line(GuiGraphics graphics, String value, int y, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(value, Math.max(0, panelWidth - 16)), left + 8, y, color, false);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      graphics.fill(left, top, left + panelWidth, Math.min(height, graphY + graphHeight + 52), theme.panel());
      graphics.fill(left, top, left + panelWidth, top + 2, theme.accent());
      line(graphics, "SCHIZO / BENCHMARK LAB", top + 8, theme.accent());
      line(graphics, capture.isActive() ? "Recording gameplay | " + capture.frameCount() + " frames"
         : capture.hasCapture() ? "Capture ready to finish or save" : "Record in-world, then reopen Benchmarks to save.", top + 22, 0xffa1a3af);
      line(graphics, detail("A", a), top + 37, theme.accent());
      line(graphics, detail("B", b), top + 50, 0xffb8b9c7);
      line(graphics, memoryLine(), top + 63, 0xffa1a3af);
      line(graphics, notice.isEmpty() ? "Compare matching workloads. 50 ms+ = hitch." : notice, top + 76, 0xffa1a3af);
      drawGraph(graphics, theme);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void drawGraph(GuiGraphics graphics, StudioScreenTheme theme) {
      int gx = left + 8, gw = Math.max(1, panelWidth - 16);
      graphics.fill(gx, graphY, gx + gw, graphY + graphHeight, theme.row());
      double max = 50;
      if (a != null) for (double sample : a.timeline()) max = Math.max(max, sample);
      if (b != null) for (double sample : b.timeline()) max = Math.max(max, sample);
      drawSeries(graphics, a, 0, max, gx, gw, theme.accent());
      drawSeries(graphics, b, 1, max, gx, gw, 0xffa8a9b7);
      if (graphHeight >= 14) line(graphics, String.format(Locale.ROOT, "30s timeline | mean ms / second | scale %.0f ms", max), graphY + 2, 0xffeeeeef);
   }

   private void drawSeries(GuiGraphics graphics, BenchmarkSession session, int series, double max, int x, int w, int color) {
      if (session == null) return;
      int plotHeight = Math.max(1, graphHeight - 14);
      for (int i = 0; i < session.timeline().size(); i++) {
         double value = session.timeline().get(i);
         if (value <= 0) continue;
         int left = x + (2 * i + series) * w / 60;
         int right = x + (2 * i + series + 1) * w / 60;
         if (right <= left) continue;
         int bar = Math.max(1, (int)Math.round(value / max * plotHeight));
         graphics.fill(left, graphY + graphHeight - bar, right, graphY + graphHeight, color);
      }
   }

   @Override public void onClose() {
      if (returnScreen != null) minecraft.setScreen(returnScreen);
      else super.onClose();
   }

   @Override public boolean isPauseScreen() { return false; }
}
