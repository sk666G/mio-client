/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.profiling.EventProfiler;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.utils.FrameTiming;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Live view over Schizo's existing frame telemetry and opt-in profiler.
 *
 * Frame measurements represent the complete rendered frame. Profiler rows are
 * inclusive callback timings and are a separate view; they must not be added
 * together to produce a frame-time total.
 */
public final class PerformanceCenterScreen extends ClientScreen {
   private static final int VISIBLE_ROWS = 7;
   private static final int MAX_STORED_ROWS = 64;
   private static final long REFRESH_INTERVAL_MILLIS = 250L;
   private static final String[] TAB_NAMES = {"MODULES", "CALLBACKS", "HUD WIDGETS"};

   private int selectedTab;
   private int rowOffset;
   private long lastRefreshMillis;
   private List<DisplayRow> rows = List.of();
   private MetricsSnapshot metrics = MetricsSnapshot.EMPTY;

   private float panelLeft;
   private float panelTop;
   private float panelWidth;
   private float panelHeight;

   public PerformanceCenterScreen() {
      super();
   }

   @Override
   public void renderBackgroundOverlay(PoseStack poseStack) {
      GuiDrawUtils.fillRect(
         poseStack,
         0.0F,
         0.0F,
         GuiScaleUtils.getScaledWidth(),
         GuiScaleUtils.getScaledHeight(),
         new Color(4, 6, 10, 188)
      );
   }

   @Override
   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
      this.updateLayout();
      this.refreshData(false);

      UIModule ui = UIModule.INSTANCE;
      Color surface = ui.backgroundSetting.getValue();
      Color accent = ui.colorSetting.getValue();
      Color muted = new Color(155, 160, 170);
      Color panelEdge = ColorUtils.withAlpha(accent, 190);

      GuiDrawUtils.fillRect(
         poseStack,
         this.panelLeft,
         this.panelTop,
         this.panelLeft + this.panelWidth,
         this.panelTop + this.panelHeight,
         surface
      );
      GuiDrawUtils.drawRectOutline(
         poseStack,
         this.panelLeft,
         this.panelTop,
         this.panelLeft + this.panelWidth,
         this.panelTop + this.panelHeight,
         panelEdge
      );

      FontRenderer font = FontRenderer.INSTANCE;
      font.drawWithShadow(
         poseStack,
         "SCHIZO / PERFORMANCE CENTER",
         this.panelLeft + 16.0F,
         this.panelTop + 12.0F,
         accent
      );
      font.drawWithShadow(
         poseStack,
         "Frame pacing, module callback cost and HUD render cost",
         this.panelLeft + 16.0F,
         this.panelTop + 29.0F,
         muted
      );

      this.drawButton(
         poseStack,
         font,
         this.panelLeft + 16.0F,
         this.panelTop + 47.0F,
         82.0F,
         20.0F,
         EventProfiler.GLOBAL.isEnabled() ? "STOP" : "START",
         accent,
         EventProfiler.GLOBAL.isEnabled()
      );
      this.drawButton(
         poseStack,
         font,
         this.panelLeft + 105.0F,
         this.panelTop + 47.0F,
         70.0F,
         20.0F,
         "RESET",
         muted,
         false
      );
      String profilerStatus = EventProfiler.GLOBAL.isEnabled() ? "SAMPLING ON" : "PROFILER IDLE";
      font.drawWithShadow(
         poseStack,
         profilerStatus,
         this.panelLeft + this.panelWidth - font.getWidth(profilerStatus) - 16.0F,
         this.panelTop + 53.0F,
         EventProfiler.GLOBAL.isEnabled() ? accent : muted
      );

      float cardGap = 7.0F;
      float cardWidth = (this.panelWidth - 32.0F - cardGap * 2.0F) / 3.0F;
      float cardHeight = 39.0F;
      float cardTop = this.panelTop + 77.0F;
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F, cardTop, cardWidth, cardHeight,
         "FPS", this.metrics.fps(), accent, surface
      );
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F + cardWidth + cardGap, cardTop,
         cardWidth, cardHeight, "FRAME TIME", this.metrics.frameTime(), accent, surface
      );
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F + (cardWidth + cardGap) * 2.0F, cardTop,
         cardWidth, cardHeight, "AVG FRAME", this.metrics.averageFrameTime(), accent, surface
      );
      cardTop += cardHeight + 6.0F;
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F, cardTop, cardWidth, cardHeight,
         "1% LOW", this.metrics.onePercentLow(), accent, surface
      );
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F + cardWidth + cardGap, cardTop,
         cardWidth, cardHeight, "HITCHES / SEC", this.metrics.hitches(), accent, surface
      );
      this.drawMetricCard(
         poseStack, font, this.panelLeft + 16.0F + (cardWidth + cardGap) * 2.0F, cardTop,
         cardWidth, cardHeight, "FRAME VARIANCE", this.metrics.variance(), accent, surface
      );

      float tabsTop = this.panelTop + 170.0F;
      float tabGap = 6.0F;
      float tabWidth = (this.panelWidth - 32.0F - tabGap * 2.0F) / 3.0F;
      for (int tab = 0; tab < TAB_NAMES.length; tab++) {
         float tabLeft = this.panelLeft + 16.0F + tab * (tabWidth + tabGap);
         boolean active = this.selectedTab == tab;
         GuiDrawUtils.fillRect(
            poseStack,
            tabLeft,
            tabsTop,
            tabLeft + tabWidth,
            tabsTop + 21.0F,
            active ? ColorUtils.withAlpha(accent, 54) : ColorUtils.withAlpha(surface, 150)
         );
         GuiDrawUtils.drawRectOutline(
            poseStack,
            tabLeft,
            tabsTop,
            tabLeft + tabWidth,
            tabsTop + 21.0F,
            ColorUtils.withAlpha(active ? accent : muted, active ? 180 : 60)
         );
         font.drawWithShadow(
            poseStack,
            TAB_NAMES[tab],
            tabLeft + 9.0F,
            tabsTop + 7.0F,
            active ? Color.WHITE : muted
         );
      }

      float tableTop = this.panelTop + 200.0F;
      float tableLeft = this.panelLeft + 16.0F;
      float tableRight = this.panelLeft + this.panelWidth - 16.0F;
      GuiDrawUtils.fillRect(
         poseStack,
         tableLeft,
         tableTop,
         tableRight,
         tableTop + 18.0F,
         ColorUtils.withAlpha(accent, 28)
      );
      float tableWidth = tableRight - tableLeft;
      float callsRight = tableLeft + tableWidth * 0.66F;
      float averageRight = tableLeft + tableWidth * 0.78F;
      float maximumRight = tableLeft + tableWidth * 0.88F;
      float totalRight = tableRight - 7.0F;
      int nameLimit = Math.max(10, (int)(tableWidth * 0.075F));
      font.drawWithShadow(poseStack, "NAME", tableLeft + 7.0F, tableTop + 5.0F, muted);
      this.drawRightAligned(font, poseStack, "CALLS", callsRight, tableTop + 5.0F, muted);
      this.drawRightAligned(font, poseStack, "AVG", averageRight, tableTop + 5.0F, muted);
      this.drawRightAligned(font, poseStack, "MAX", maximumRight, tableTop + 5.0F, muted);
      this.drawRightAligned(font, poseStack, "TOTAL", totalRight, tableTop + 5.0F, muted);

      if (this.rows.isEmpty()) {
         String hint = EventProfiler.GLOBAL.isEnabled()
            ? "Waiting for profiled callbacks..."
            : "Start the profiler, play normally, then review the captured cost.";
         font.drawWithShadow(poseStack, hint, tableLeft + 8.0F, tableTop + 34.0F, muted);
      } else {
         int visible = Math.min(VISIBLE_ROWS, this.rows.size() - this.rowOffset);
         for (int index = 0; index < visible; index++) {
            DisplayRow row = this.rows.get(this.rowOffset + index);
            float rowTop = tableTop + 20.0F + index * 20.0F;
            if ((index & 1) == 1) {
               GuiDrawUtils.fillRect(
                  poseStack,
                  tableLeft,
                  rowTop - 2.0F,
                  tableRight,
                  rowTop + 17.0F,
                  ColorUtils.withAlpha(surface, 90)
               );
            }
            font.drawWithShadow(
               poseStack,
               abbreviate(row.name(), nameLimit),
               tableLeft + 7.0F,
               rowTop + 3.0F,
               Color.WHITE
            );
            this.drawRightAligned(font, poseStack, Long.toString(row.calls()), callsRight, rowTop + 3.0F, muted);
            this.drawRightAligned(font, poseStack, milliseconds(row.averageNanos()), averageRight, rowTop + 3.0F, muted);
            this.drawRightAligned(font, poseStack, milliseconds(row.maximumNanos()), maximumRight, rowTop + 3.0F, muted);
            this.drawRightAligned(font, poseStack, milliseconds(row.totalNanos()), totalRight, rowTop + 3.0F, accent);
         }
      }

      String footer = this.panelWidth < 590.0F
         ? (this.selectedTab == 2
            ? "HUD timings are separate from total frame time."
            : "Callback / module timings overlap; they are not additive.")
         : (this.selectedTab == 2
            ? "HUD timings cover direct widget renders. Use frame metrics for total frame duration."
            : "Callback / module timings are inclusive and overlap; do not add them to frame time.");
      font.drawWithShadow(
         poseStack,
         footer,
         this.panelLeft + 16.0F,
         this.panelTop + this.panelHeight - 23.0F,
         muted
      );
      if (this.rows.size() > VISIBLE_ROWS && this.panelWidth >= 590.0F) {
         String page = (this.rowOffset + 1) + "-" + Math.min(this.rows.size(), this.rowOffset + VISIBLE_ROWS)
            + " / " + this.rows.size() + "  ·  scroll to browse";
         this.drawRightAligned(
            font, poseStack, page, this.panelLeft + this.panelWidth - 16.0F,
            this.panelTop + this.panelHeight - 23.0F, muted
         );
      }
   }

   @Override
   public void onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return;
      }
      this.updateLayout();

      if (this.contains(mouseX, mouseY, this.panelLeft + 16.0F, this.panelTop + 47.0F, 82.0F, 20.0F)) {
         if (EventProfiler.GLOBAL.isEnabled()) {
            EventProfiler.GLOBAL.stop();
         } else {
            EventProfiler.GLOBAL.start();
         }
         return;
      }

      if (this.contains(mouseX, mouseY, this.panelLeft + 105.0F, this.panelTop + 47.0F, 70.0F, 20.0F)) {
         EventProfiler.GLOBAL.reset();
         this.rowOffset = 0;
         this.refreshData(true);
         return;
      }

      float tabsTop = this.panelTop + 170.0F;
      float tabGap = 6.0F;
      float tabWidth = (this.panelWidth - 32.0F - tabGap * 2.0F) / 3.0F;
      for (int tab = 0; tab < TAB_NAMES.length; tab++) {
         float tabLeft = this.panelLeft + 16.0F + tab * (tabWidth + tabGap);
         if (this.contains(mouseX, mouseY, tabLeft, tabsTop, tabWidth, 21.0F)) {
            this.selectedTab = tab;
            this.rowOffset = 0;
            this.refreshData(true);
            return;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount != 0.0D) {
         this.refreshData(false);
         int maximumOffset = Math.max(0, this.rows.size() - VISIBLE_ROWS);
         this.rowOffset = Math.max(
            0,
            Math.min(maximumOffset, this.rowOffset + (verticalAmount < 0.0D ? 1 : -1))
         );
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   private void refreshData(boolean force) {
      long now = System.currentTimeMillis();
      if (!force && now - this.lastRefreshMillis < REFRESH_INTERVAL_MILLIS) {
         return;
      }
      this.lastRefreshMillis = now;

      FrameTiming timing = ClientContext.frameTiming;
      if (timing == null) {
         this.metrics = MetricsSnapshot.EMPTY;
      } else {
         this.metrics = new MetricsSnapshot(
            Integer.toString(timing.getFramesPerSecond()),
            formatMilliseconds(timing.getFrameTimeMillis()),
            formatMilliseconds(timing.getAverageFrameTimeMillis()),
            timing.getOnePercentLowFramesPerSecond() > 0.0F
               ? String.format(Locale.ROOT, "%.1f FPS", timing.getOnePercentLowFramesPerSecond())
               : "—",
            Integer.toString(timing.getStuttersPerSecond()),
            String.format(Locale.ROOT, "%.2f ms²", timing.getFrameTimeVarianceMillisSquared())
         );
      }

      EventProfiler profiler = EventProfiler.GLOBAL;
      this.rows = switch (this.selectedTab) {
         case 1 -> callbackRows(profiler.topCallbacks(MAX_STORED_ROWS));
         case 2 -> hudRows(profiler.topHudWidgets(MAX_STORED_ROWS));
         default -> moduleRows(profiler.topModules(MAX_STORED_ROWS));
      };
      this.rowOffset = Math.min(this.rowOffset, Math.max(0, this.rows.size() - VISIBLE_ROWS));
   }

   private void updateLayout() {
      int screenWidth = GuiScaleUtils.getScaledWidth();
      int screenHeight = GuiScaleUtils.getScaledHeight();
      this.panelWidth = Math.min(740.0F, Math.max(280.0F, screenWidth - 20.0F));
      this.panelHeight = Math.min(420.0F, Math.max(320.0F, screenHeight - 20.0F));
      this.panelLeft = (screenWidth - this.panelWidth) / 2.0F;
      this.panelTop = (screenHeight - this.panelHeight) / 2.0F;
   }

   private void drawMetricCard(
      PoseStack poseStack,
      FontRenderer font,
      float x,
      float y,
      float width,
      float height,
      String label,
      String value,
      Color accent,
      Color surface
   ) {
      GuiDrawUtils.fillRect(
         poseStack, x, y, x + width, y + height,
         ColorUtils.withAlpha(surface, 210)
      );
      GuiDrawUtils.drawRectOutline(
         poseStack, x, y, x + width, y + height,
         ColorUtils.withAlpha(accent, 72)
      );
      font.drawWithShadow(poseStack, label, x + 8.0F, y + 5.0F, Color.GRAY);
      font.drawWithShadow(poseStack, value, x + 8.0F, y + 20.0F, Color.WHITE);
   }

   private void drawButton(
      PoseStack poseStack,
      FontRenderer font,
      float x,
      float y,
      float width,
      float height,
      String label,
      Color accent,
      boolean active
   ) {
      Color background = active
         ? ColorUtils.withAlpha(accent, 68)
         : ColorUtils.withAlpha(accent, 28);
      GuiDrawUtils.fillRect(poseStack, x, y, x + width, y + height, background);
      GuiDrawUtils.drawRectOutline(
         poseStack, x, y, x + width, y + height, ColorUtils.withAlpha(accent, 145)
      );
      font.drawWithShadow(
         poseStack, label,
         x + (width - font.getWidth(label)) / 2.0F,
         y + 7.0F,
         Color.WHITE
      );
   }

   private void drawRightAligned(
      FontRenderer font, PoseStack poseStack, String text, float right, float y, Color color
   ) {
      font.drawWithShadow(poseStack, text, right - font.getWidth(text), y, color);
   }

   private static List<DisplayRow> moduleRows(List<EventProfiler.ModuleSnapshot> snapshots) {
      List<DisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.ModuleSnapshot snapshot : snapshots) {
         result.add(new DisplayRow(
            snapshot.moduleName(), snapshot.invocationCount(),
            snapshot.totalNanos(), snapshot.averageNanos(), snapshot.maximumNanos()
         ));
      }
      return List.copyOf(result);
   }

   private static List<DisplayRow> callbackRows(List<EventProfiler.CallbackSnapshot> snapshots) {
      List<DisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.CallbackSnapshot snapshot : snapshots) {
         result.add(new DisplayRow(
            snapshot.label(), snapshot.invocationCount(),
            snapshot.totalNanos(), snapshot.averageNanos(), snapshot.maximumNanos()
         ));
      }
      return List.copyOf(result);
   }

   private static List<DisplayRow> hudRows(List<EventProfiler.HudSnapshot> snapshots) {
      List<DisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.HudSnapshot snapshot : snapshots) {
         result.add(new DisplayRow(
            snapshot.widgetName(), snapshot.invocationCount(),
            snapshot.totalNanos(), snapshot.averageNanos(), snapshot.maximumNanos()
         ));
      }
      return List.copyOf(result);
   }

   private static String formatMilliseconds(float milliseconds) {
      return milliseconds > 0.0F
         ? String.format(Locale.ROOT, "%.2f ms", milliseconds)
         : "—";
   }

   private static String milliseconds(long nanos) {
      return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
   }

   private static String abbreviate(String value, int maximumLength) {
      if (value.length() <= maximumLength) {
         return value;
      }
      return value.substring(0, maximumLength - 1) + "…";
   }

   private static boolean contains(
      double mouseX, double mouseY, float x, float y, float width, float height
   ) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private record DisplayRow(
      String name, long calls, long totalNanos, long averageNanos, long maximumNanos
   ) {
   }

   private record MetricsSnapshot(
      String fps,
      String frameTime,
      String averageFrameTime,
      String onePercentLow,
      String hitches,
      String variance
   ) {
      private static final MetricsSnapshot EMPTY = new MetricsSnapshot("—", "—", "—", "—", "—", "—");
   }
}
