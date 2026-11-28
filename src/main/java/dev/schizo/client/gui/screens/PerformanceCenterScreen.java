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
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.ClientRenderTypes;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.GuiScaleUtils;
import dev.schizo.client.render.MeshBuffer;
import dev.schizo.client.render.MeshDrawContext;
import dev.schizo.client.render.VertexGeometry;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.utils.FrameTiming;
import dev.schizo.client.utils.MonotonicRefreshGate;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

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
   private static final Color BACKDROP=new Color(4,6,10,188), MUTED=new Color(155,160,170), PANEL_EDGE=new Color(55,56,64,200);
   private static final String[] TAB_NAMES = {"MODULES", "CALLBACKS", "HUD WIDGETS", "SCANS"};

   private final DashboardText fittedText=new DashboardText();
   private int selectedTab;
   private int rowOffset;
   private final MonotonicRefreshGate refreshGate = new MonotonicRefreshGate(250_000_000L);
   private List<ProfilerDisplayRow> rows = List.of();
   private MetricsSnapshot metrics = MetricsSnapshot.EMPTY;
   private float[] frameHistorySamples = new float[0];

   private float fitScale = 1.0F;
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
         BACKDROP
      );
   }

   @Override
   public void renderContents(
      GuiGraphics graphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick
   ) {
      poseStack.pushPose();
      try {
         poseStack.scale(this.fitScale, this.fitScale, 1.0F);
         this.renderDashboard(poseStack);
      } finally {
         poseStack.popPose();
      }
   }

   private void renderDashboard(PoseStack poseStack) {
      this.refreshData(false);

      UIModule ui = UIModule.INSTANCE;
      Color surface = ui.backgroundSetting.getValue();
      Color accent = ui.colorSetting.getValue();
      Color muted=MUTED,panelEdge=PANEL_EDGE;

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
      fittedText.begin(font.measurementIdentity(),System.nanoTime());
      drawFitted(font,poseStack,this.panelWidth<420?"PERFORMANCE":"SCHIZO / PERFORMANCE CENTER",
         panelLeft+16,panelTop+12,panelWidth-122,accent);
      this.drawButton(poseStack, font, this.panelLeft + this.panelWidth - 100.0F, this.panelTop + 5.0F,
         84.0F, 20.0F, "BENCHMARKS", accent, false);
      String subtitle = this.panelWidth < 590.0F
         ? "Arrow keys navigate tabs and rows"
         : "Frame pacing, module callback cost and HUD render cost  •  LEFT/RIGHT tabs  •  UP/DOWN rows  •  HOME/END";
      drawFitted(font,poseStack,subtitle,panelLeft+16,panelTop+29,panelWidth-32,muted);

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
      TaskScheduler.Snapshot schedulerSnapshot = ClientContext.taskScheduler == null
         ? new TaskScheduler.Snapshot(0, 0, 0, 0)
         : ClientContext.taskScheduler.snapshot();
      String profilerStatus = (EventProfiler.GLOBAL.isEnabled() ? "SAMPLING ON" : "PROFILER IDLE")
         + "  •  ERR " + EventFailureTracker.GLOBAL.totalFailures()
         + "  •  TASKS " + schedulerSnapshot.totalQueued() + "/" + schedulerSnapshot.totalOutstanding();
      drawRightFitted(font,poseStack,profilerStatus,panelLeft+panelWidth-16,panelTop+53,
         panelWidth-199,EventProfiler.GLOBAL.isEnabled()?accent:muted);

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

      this.drawFrameHistoryGraph(
         poseStack,
         font,
         accent,
         surface,
         muted
      );

      float tabsTop = this.panelTop + 224.0F;
      float tabGap = 6.0F;
      float tabWidth = (this.panelWidth - 32.0F - tabGap * (TAB_NAMES.length - 1)) / TAB_NAMES.length;
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
         drawFitted(font,poseStack,TAB_NAMES[tab],tabLeft+5,tabsTop+7,tabWidth-10,active?Color.WHITE:muted);
      }

      float tableTop = this.panelTop + 254.0F;
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
      float nameRight=tableLeft+tableWidth*0.43F;
      font.drawWithShadow(poseStack, "NAME", tableLeft + 7.0F, tableTop + 5.0F, muted);
      drawRightFitted(font,poseStack,selectedTab==3?"CELLS":"CALLS",callsRight,tableTop+5,callsRight-nameRight-4,muted);
      drawRightFitted(font,poseStack,"AVG",averageRight,tableTop+5,averageRight-callsRight-4,muted);
      drawRightFitted(font,poseStack,"MAX",maximumRight,tableTop+5,maximumRight-averageRight-4,muted);
      drawRightFitted(font,poseStack,"TOTAL",totalRight,tableTop+5,totalRight-maximumRight-4,muted);

      if (this.rows.isEmpty()) {
         String hint = EventProfiler.GLOBAL.isEnabled()
            ? "Waiting for profiled callbacks..."
            : "Start the profiler, play normally, then review the captured cost.";
         drawFitted(font,poseStack,hint,tableLeft+8,tableTop+25,tableWidth-16,muted);
      } else {
         int visible = Math.min(this.visibleRowCount(), this.rows.size() - this.rowOffset);
         for (int index = 0; index < visible; index++) {
            ProfilerDisplayRow row = this.rows.get(this.rowOffset + index);
            ProfilerDisplayRow.Values values = row.values();
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
            drawFitted(font,poseStack,row.name(),tableLeft+7,rowTop+3,nameRight-tableLeft-7,Color.WHITE);
            drawRightFitted(font,poseStack,values.calls(),callsRight,rowTop+3,callsRight-nameRight-4,muted);
            drawRightFitted(font,poseStack,values.average(),averageRight,rowTop+3,averageRight-callsRight-4,muted);
            drawRightFitted(font,poseStack,values.maximum(),maximumRight,rowTop+3,maximumRight-averageRight-4,muted);
            drawRightFitted(font,poseStack,values.total(),totalRight,rowTop+3,totalRight-maximumRight-4,accent);
         }
      }

      String footer = this.panelWidth < 590.0F
         ? (this.selectedTab == 2
            ? "HUD timings are separate from total frame time."
            : "Callback / module timings overlap; they are not additive.")
         : (this.selectedTab == 2
            ? "HUD timings cover direct widget renders. Use frame metrics for total frame duration."
            : "Callback / module timings are inclusive and overlap; do not add them to frame time.");
      if (this.selectedTab == 3) footer = "Scan ms per slice; CELLS counts examined positions.";
      float footerWidth=panelWidth-32;
      if(rows.size()>visibleRowCount() && panelWidth>=590) {
         String page=(rowOffset+1)+"-"+Math.min(rows.size(),rowOffset+visibleRowCount())+" / "+rows.size()+" · scroll";
         float pageWidth=Math.min(footerWidth*0.33F,140F);
         drawRightFitted(font,poseStack,page,panelLeft+panelWidth-16,panelTop+panelHeight-23,pageWidth,muted);
         footerWidth-=pageWidth+8;
      }
      drawFitted(font,poseStack,footer,panelLeft+16,panelTop+panelHeight-23,footerWidth,muted);
   }

   @Override
   public void onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return;
      }
      mouseX /= this.fitScale;
      mouseY /= this.fitScale;
      if (this.contains(mouseX, mouseY, this.panelLeft + this.panelWidth - 100.0F, this.panelTop + 5.0F, 84.0F, 20.0F)) {
         this.minecraft.setScreen(new BenchmarkCenterScreen(this));
         return;
      }

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
         dev.schizo.client.profiling.ScanWorkTracker.GLOBAL.reset();
         // Reset profiling only; health diagnostics have their own explicit reset.
         this.rowOffset = 0;
         this.refreshData(true);
         return;
      }

      float tabsTop = this.panelTop + 224.0F;
      float tabGap = 6.0F;
      float tabWidth = (this.panelWidth - 32.0F - tabGap * (TAB_NAMES.length - 1)) / TAB_NAMES.length;
      for (int tab = 0; tab < TAB_NAMES.length; tab++) {
         float tabLeft = this.panelLeft + 16.0F + tab * (tabWidth + tabGap);
         if (this.contains(mouseX, mouseY, tabLeft, tabsTop, tabWidth, 21.0F)) {
            this.selectTab(tab);
            return;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      double scale = GuiScaleUtils.calculateScaleMultiplier(this.guiScale) * this.fitScale;
      double scaledMouseX = mouseX / scale;
      double scaledMouseY = mouseY / scale;
      float tableLeft = this.panelLeft + 16.0F;
      float tableTop = this.panelTop + 254.0F;
      float tableWidth = this.panelWidth - 32.0F;
      int renderedRows = this.rows.isEmpty()
         ? 1
         : Math.min(this.visibleRowCount(), this.rows.size() - this.rowOffset);
      float tableHeight = 18.0F + renderedRows * 20.0F;
      if (verticalAmount == 0.0D
         || !this.contains(scaledMouseX, scaledMouseY, tableLeft, tableTop, tableWidth, tableHeight)) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

      this.moveRowOffset(verticalAmount < 0.0D ? 1 : -1);
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.closing) {
         return false;
      }
      switch (event.key()) {
         case GLFW.GLFW_KEY_LEFT -> {
            this.selectTab(this.selectedTab - 1);
            return true;
         }
         case GLFW.GLFW_KEY_RIGHT -> {
            this.selectTab(this.selectedTab + 1);
            return true;
         }
         case GLFW.GLFW_KEY_UP -> {
            this.moveRowOffset(-1);
            return true;
         }
         case GLFW.GLFW_KEY_DOWN -> {
            this.moveRowOffset(1);
            return true;
         }
         case GLFW.GLFW_KEY_PAGE_UP -> {
            this.moveRowOffset(-this.visibleRowCount());
            return true;
         }
         case GLFW.GLFW_KEY_PAGE_DOWN -> {
            this.moveRowOffset(this.visibleRowCount());
            return true;
         }
         case GLFW.GLFW_KEY_HOME -> {
            this.refreshData(false);
            this.rowOffset = 0;
            return true;
         }
         case GLFW.GLFW_KEY_END -> {
            this.refreshData(false);
            this.rowOffset = Math.max(0, this.rows.size() - this.visibleRowCount());
            return true;
         }
         default -> {
            return super.keyPressed(event);
         }
      }
   }

   private void selectTab(int tab) {
      this.selectedTab = Math.floorMod(tab, TAB_NAMES.length);
      this.rowOffset = 0;
      this.refreshData(true);
   }

   private void moveRowOffset(int delta) {
      this.refreshData(false);
      int maximumOffset = Math.max(0, this.rows.size() - this.visibleRowCount());
      this.rowOffset = Math.max(0, Math.min(maximumOffset, this.rowOffset + delta));
   }

   private void refreshData(boolean force) {
      long now = System.nanoTime();
      if (!this.refreshGate.shouldRefresh(now, force)) {
         return;
      }

      FrameTiming timing = ClientContext.frameTiming;
      if (timing == null) {
         this.metrics = MetricsSnapshot.EMPTY;
         this.frameHistorySamples = new float[0];
      } else {
         this.frameHistorySamples = timing.getRecentFrameTimesMillis(512);
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
         case 3 -> dev.schizo.client.profiling.ScanWorkTracker.GLOBAL.snapshot().stream()
            .map(scan -> new ProfilerDisplayRow(scan.source().name(), scan.cells(), scan.totalNanos(),
               scan.averageSliceNanos(), scan.maximumSliceNanos())).toList();
         default -> moduleRows(profiler.topModules(MAX_STORED_ROWS));
      };
      this.rowOffset = Math.min(this.rowOffset, Math.max(0, this.rows.size() - this.visibleRowCount()));
   }

   @Override
   protected void init() {
      super.init();
      double guiMultiplier = GuiScaleUtils.calculateScaleMultiplier(this.guiScale);
      float width = (float)(this.width / guiMultiplier);
      float height = (float)(this.height / guiMultiplier);
      this.fitScale = PerformanceViewport.fitScale(width, height);
      float screenWidth = width / this.fitScale;
      float screenHeight = height / this.fitScale;
      this.panelWidth = Math.min(740.0F, Math.max(280.0F, screenWidth - 20.0F));
      this.panelHeight = Math.min(500.0F, Math.max(320.0F, screenHeight - 20.0F));
      this.panelLeft = (screenWidth - this.panelWidth) / 2.0F;
      this.panelTop = (screenHeight - this.panelHeight) / 2.0F;
      this.rowOffset = Math.min(this.rowOffset, Math.max(0, this.rows.size() - this.visibleRowCount()));
   }


   private void drawFrameHistoryGraph(
      PoseStack poseStack,
      FontRenderer font,
      Color accent,
      Color surface,
      Color muted
   ) {
      float left = this.panelLeft + 16.0F;
      float top = this.panelTop + 168.0F;
      float width = this.panelWidth - 32.0F;
      float height = 48.0F;
      GuiDrawUtils.fillRect(
         poseStack,
         left,
         top,
         left + width,
         top + height,
         ColorUtils.withAlpha(surface, 210)
      );
      GuiDrawUtils.drawRectOutline(
         poseStack,
         left,
         top,
         left + width,
         top + height,
         ColorUtils.withAlpha(accent, 72)
      );

      String label = "RECENT FRAME HISTORY";
      String scaleLabel = "50 ms SCALE";
      font.drawWithShadow(poseStack, label, left + 8.0F, top + 5.0F, muted);
      this.drawRightAligned(font, poseStack, scaleLabel, left + width - 8.0F, top + 5.0F, muted);

      if (this.frameHistorySamples.length < 2) {
         font.drawWithShadow(
            poseStack,
            "Waiting for frame samples...",
            left + 8.0F,
            top + 29.0F,
            Color.GRAY
         );
         return;
      }

      float plotLeft = left + 8.0F;
      float plotRight = left + width - 8.0F;
      float plotTop = top + 17.0F;
      float plotBottom = top + height - 5.0F;
      float plotHeight = plotBottom - plotTop;
      float sixtyFpsY = plotBottom - (1000.0F / 60.0F) / 50.0F * plotHeight;

      MeshBuffer buffer = MeshDrawContext.getBuffer();
      buffer.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
      int guideColor = ColorUtils.withAlpha(accent, 75).hashCode();
      int graphColor = ColorUtils.withAlpha(accent, 245).hashCode();
      VertexGeometry.appendLine(
         buffer,
         poseStack.last(),
         plotLeft,
         sixtyFpsY,
         0.0F,
         plotRight,
         sixtyFpsY,
         0.0F,
         1.0F,
         guideColor
      );

      float stepX = (plotRight - plotLeft) / (this.frameHistorySamples.length - 1);
      for (int index = 1; index < this.frameHistorySamples.length; index++) {
         float previousX = plotLeft + (index - 1) * stepX;
         float currentX = plotLeft + index * stepX;
         float previousFrameTime = Math.min(50.0F, this.frameHistorySamples[index - 1]);
         float currentFrameTime = Math.min(50.0F, this.frameHistorySamples[index]);
         float previousY = plotBottom - previousFrameTime / 50.0F * plotHeight;
         float currentY = plotBottom - currentFrameTime / 50.0F * plotHeight;
         VertexGeometry.appendLine(
            buffer,
            poseStack.last(),
            previousX,
            previousY,
            0.0F,
            currentX,
            currentY,
            0.0F,
            1.0F,
            graphColor
         );
      }
      buffer.draw(ClientRenderTypes.worldLines());
   }

   private int visibleRowCount() {
      float tableTop = this.panelTop + 254.0F;
      float footerTop = this.panelTop + this.panelHeight - 23.0F;
      int availableRows = (int)Math.floor((footerTop - tableTop - 23.0F) / 20.0F);
      return Math.max(1, Math.min(VISIBLE_ROWS, availableRows));
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
      drawFitted(font,poseStack,label,x+8,y+5,width-16,Color.GRAY);
      drawFitted(font,poseStack,value,x+8,y+20,width-16,Color.WHITE);
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
      var fitted=fittedText.fit(label,width-8,PerformanceCenterScreen::measureText);
      font.drawWithShadow(poseStack,fitted.text(),x+(width-fitted.width())/2,y+7,Color.WHITE);
   }

   private static double measureText(String text) { return FontRenderer.INSTANCE.getWidth(text); }

   private void drawFitted(FontRenderer font,PoseStack poses,String text,float x,float y,float available,Color color) {
      var fitted=fittedText.fit(text,available,PerformanceCenterScreen::measureText);
      font.drawWithShadow(poses,fitted.text(),x,y,color);
   }

   private void drawRightFitted(FontRenderer font,PoseStack poses,String text,float right,float y,float available,Color color) {
      var fitted=fittedText.fit(text,available,PerformanceCenterScreen::measureText);
      font.drawWithShadow(poses,fitted.text(),right-fitted.width(),y,color);
   }

   private void drawRightAligned(
      FontRenderer font, PoseStack poseStack, String text, float right, float y, Color color
   ) {
      font.drawWithShadow(poseStack, text, right - font.getWidth(text), y, color);
   }

   private static List<ProfilerDisplayRow> moduleRows(List<EventProfiler.ModuleSnapshot> snapshots) {
      List<ProfilerDisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.ModuleSnapshot snapshot : snapshots) {
         result.add(new ProfilerDisplayRow(
            snapshot.moduleName(), snapshot.invocationCount(),
            snapshot.totalNanos(), snapshot.averageNanos(), snapshot.maximumNanos()
         ));
      }
      return List.copyOf(result);
   }

   private static List<ProfilerDisplayRow> callbackRows(List<EventProfiler.CallbackSnapshot> snapshots) {
      List<ProfilerDisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.CallbackSnapshot snapshot : snapshots) {
         result.add(new ProfilerDisplayRow(
            snapshot.label(), snapshot.invocationCount(),
            snapshot.totalNanos(), snapshot.averageNanos(), snapshot.maximumNanos()
         ));
      }
      return List.copyOf(result);
   }

   private static List<ProfilerDisplayRow> hudRows(List<EventProfiler.HudSnapshot> snapshots) {
      List<ProfilerDisplayRow> result = new ArrayList<>(snapshots.size());
      for (EventProfiler.HudSnapshot snapshot : snapshots) {
         result.add(new ProfilerDisplayRow(
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

   private static boolean contains(
      double mouseX, double mouseY, float x, float y, float width, float height
   ) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
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
