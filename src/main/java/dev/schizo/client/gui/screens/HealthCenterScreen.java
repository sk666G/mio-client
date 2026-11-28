package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.config.ModuleConfigLoadReport;
import dev.schizo.client.diagnostics.ClientHealthSnapshot;
import dev.schizo.client.diagnostics.ClientBindingDiagnostics;
import dev.schizo.client.diagnostics.HealthReport;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.integrations.IntegrationHealth;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.profiling.EventProfiler;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local, read-only health surface. Explicit clipboard action never exports raw exception text. */
public final class HealthCenterScreen extends StudioScreen {
   private final Screen returnScreen;
   private HealthReport report;
   private List<String> reportRows = List.of();
   private int left, top, panelWidth, rowCount, offset;
   private StudioViewportLayouts.Review layout;
   private long refreshedAt;
   private String notice = "";

   public HealthCenterScreen(Screen returnScreen) {
      super(Component.literal("Schizo Health Center"));
      this.returnScreen = returnScreen;
   }

   @Override
   protected void init() {
      super.init();
      layout=StudioViewportLayouts.health(width,height);
      panelWidth=layout.width();left=layout.left();top=layout.top();rowCount=layout.rows();
      if(layout.compact()) {
         button("Back",left,Math.max(0,height-23),panelWidth,this::onClose);
         return;
      }
      int y=layout.footerY(),w=(panelWidth-8)/3;
      button("Back",left,y,w,this::onClose);
      button("Refresh",left+w+4,y,w,this::refresh).help("Refresh the local diagnostic snapshot.");
      button("Copy report",left+2*(w+4),y,w,()->{
         refresh();
         minecraft.keyboardHandler.setClipboard(report.clipboardText());
         notice="Local diagnostic summary copied";
      }).help("Copy a local summary without raw exception text.");
      refresh();
   }

   private StudioButton button(String label,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:"+label,addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored->action.run())));
   }

   private void refresh() {
      var safe = SafeModeManager.GLOBAL.status();
      var config = ClientContext.moduleRegistry == null
         ? new ModuleConfigLoadReport.Snapshot(List.of(), 0) : ClientContext.moduleRegistry.getLastConfigLoadReport();
      var tasks = ClientContext.taskScheduler == null
         ? new TaskScheduler.Snapshot(0, 0, 0, 0) : ClientContext.taskScheduler.snapshot();
      var profiles = ClientContext.contextProfileRegistry;
      report = new HealthReport(new ClientHealthSnapshot(safe.active(), safe.armed(),
         safe.initializationFailed(), safe.unsafeMarker(), config.totalIssueCount(), EventFailureTracker.GLOBAL.totalFailures(),
         tasks.totalQueued(), tasks.totalOutstanding(), EventProfiler.GLOBAL.isEnabled()),
         ClientContext.baritoneBridge == null ? IntegrationHealth.UNAVAILABLE : ClientContext.baritoneBridge.health(),
         ClientContext.waypointAdapter == null ? IntegrationHealth.UNAVAILABLE : ClientContext.waypointAdapter.health(),
         profiles != null && profiles.isEnabled(), profiles != null && profiles.getFailedContextDescription() != null,
         ClientBindingDiagnostics.capture().groupCount());
      var nextRows=report.lines();
      if(!nextRows.equals(reportRows)) reportRows=nextRows;
      refreshedAt = System.nanoTime();
      offset=StudioViewportLayouts.scrollOffset(offset,0,reportRows.size(),rowCount);
   }

   @Override public void tick() {
      if (!layout.compact() && System.nanoTime() - refreshedAt >= 500_000_000L) refresh();
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      if(layout.compact()) {
         graphics.drawString(font,font.plainSubstrByWidth("Reduce GUI scale to view diagnostics",panelWidth),left,top,0xffeeeeef,false);
         super.render(graphics,mouseX,mouseY,partialTick);
         return;
      }
      int bottom=layout.bottom();
      graphics.fill(left, top, left + panelWidth, bottom, theme.panel());
      graphics.fill(left, top, left + panelWidth, top + 2, theme.accent());
      graphics.drawString(font,font.plainSubstrByWidth("SCHIZO / HEALTH CENTER",panelWidth-18),left+9,top+10,theme.accent(),false);
      String hint = notice.isEmpty() ? "Local diagnostics | Scroll / PgUp / PgDn" : notice;
      graphics.drawString(font, font.plainSubstrByWidth(hint, Math.max(0, panelWidth - 18)), left + 9, top + 24, 0xffa1a3af, false);
      if (report != null) {
         var rows = reportRows;
         for (int i = 0; i < rowCount && offset + i < rows.size(); i++) {
            int y=layout.rowsY()+i*layout.rowHeight();
            graphics.fill(left + 6, y, left + panelWidth - 6, y + 15, theme.row());
            graphics.drawString(font, font.plainSubstrByWidth(rows.get(offset + i), Math.max(0, panelWidth - 20)),
               left + 10, y + 3, 0xffeeeeef, false);
         }
      }
      int hovered=layout.rowIndex(mouseX,mouseY,offset,reportRows.size());
      if(rowHelpReady(hovered<0?null:reportRows.get(hovered))) showRowHelp(graphics,reportRows.get(hovered),mouseX,mouseY);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (!layout.compact() && report != null && vertical != 0 && x>=left && x<left+panelWidth
         && y>=layout.rowsY() && y<layout.rowsY()+rowCount*layout.rowHeight()) {
         offset=StudioViewportLayouts.scrollOffset(offset,vertical<0?1:-1,reportRows.size(),rowCount);
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if(!layout.compact()) {
         int delta=switch(event.key()) {
            case GLFW.GLFW_KEY_PAGE_UP -> -rowCount;
            case GLFW.GLFW_KEY_PAGE_DOWN -> rowCount;
            case GLFW.GLFW_KEY_HOME -> Integer.MIN_VALUE;
            case GLFW.GLFW_KEY_END -> Integer.MAX_VALUE;
            default -> 0;
         };
         if(delta!=0) {
            offset=StudioViewportLayouts.scrollOffset(offset,delta,reportRows.size(),rowCount);
            return true;
         }
      }
      return super.keyPressed(event);
   }

   @Override public void onClose() {
      if (returnScreen != null) returnToParent(returnScreen);
      else super.onClose();
   }

   @Override public boolean isPauseScreen() { return false; }
}
