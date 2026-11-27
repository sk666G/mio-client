package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.config.ModuleConfigLoadReport;
import dev.schizo.client.diagnostics.ClientHealthSnapshot;
import dev.schizo.client.diagnostics.HealthReport;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.integrations.IntegrationHealth;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.profiling.EventProfiler;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local, read-only health surface. Explicit clipboard action never exports raw exception text. */
public final class HealthCenterScreen extends Screen {
   private final Screen returnScreen;
   private HealthReport report;
   private int left, top, panelWidth, rowCount, offset;
   private long refreshedAt;
   private String notice = "";

   public HealthCenterScreen(Screen returnScreen) {
      super(Component.literal("Schizo Health Center"));
      this.returnScreen = returnScreen;
   }

   @Override
   protected void init() {
      panelWidth = Math.max(1, Math.min(440, width - 20));
      left = (width - panelWidth) / 2;
      top = Math.max(8, (height - 300) / 2);
      rowCount = Math.max(1, Math.min(12, (height - top - 84) / 17));
      int buttonY = Math.min(height - 23, top + 42 + rowCount * 17);
      int w = Math.max(1, (panelWidth - 8) / 3);
      addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
         .bounds(left, buttonY, w, 20).build());
      addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> refresh())
         .bounds(left + w + 4, buttonY, w, 20).build());
      addRenderableWidget(Button.builder(Component.literal("Copy report"), button -> {
         refresh();
         minecraft.keyboardHandler.setClipboard(report.clipboardText());
         notice = "Local diagnostic summary copied";
      }).bounds(left + 2 * (w + 4), buttonY, w, 20).build());
      refresh();
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
         profiles != null && profiles.isEnabled(), profiles != null && profiles.getFailedContextDescription() != null);
      refreshedAt = System.nanoTime();
      offset = Math.max(0, Math.min(offset, report.lines().size() - rowCount));
   }

   @Override public void tick() {
      if (System.nanoTime() - refreshedAt >= 500_000_000L) refresh();
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xee08090d);
      int bottom = Math.min(height, top + 68 + rowCount * 17);
      graphics.fill(left, top, left + panelWidth, bottom, 0xff13141a);
      graphics.fill(left, top, left + panelWidth, top + 2, 0xffe7394d);
      graphics.drawString(font, "SCHIZO / HEALTH CENTER", left + 9, top + 10, 0xffe7394d, false);
      String hint = notice.isEmpty() ? "Local diagnostics  |  Scroll to browse" : notice;
      graphics.drawString(font, font.plainSubstrByWidth(hint, Math.max(0, panelWidth - 18)), left + 9, top + 24, 0xffa1a3af, false);
      if (report != null) {
         var rows = report.lines();
         for (int i = 0; i < rowCount && offset + i < rows.size(); i++) {
            int y = top + 38 + i * 17;
            graphics.fill(left + 6, y, left + panelWidth - 6, y + 15, 0xff21222a);
            graphics.drawString(font, font.plainSubstrByWidth(rows.get(offset + i), Math.max(0, panelWidth - 20)),
               left + 10, y + 3, 0xffeeeeef, false);
         }
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (report != null && vertical != 0 && x >= left && x < left + panelWidth) {
         offset = Math.max(0, Math.min(Math.max(0, report.lines().size() - rowCount), offset + (vertical < 0 ? 1 : -1)));
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   @Override public void onClose() {
      if (returnScreen != null) minecraft.setScreen(returnScreen);
      else super.onClose();
   }

   @Override public boolean isPauseScreen() { return false; }
}
