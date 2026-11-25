/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ModuleConfigLoadReport;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.diagnostics.ClientHealthSnapshot;
import dev.schizo.client.events.bus.EventFailureTracker;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.profiling.EventProfiler;
import dev.schizo.client.utils.chat.ChatUtils;
import net.minecraft.network.chat.Component;

/** Local, value-free summary of Schizo runtime health diagnostics. */
public final class HealthCommand extends Command {
   private static final int ISSUE_PREVIEW_LIMIT = 5;

   public HealthCommand() {
      super("health");
      this.setAliases("doctor", "diagnostics");
   }

   @Override
   public void exec(LiteralArgumentBuilder builder) {
      builder.executes(context -> {
         this.showHealth();
         return 1;
      });
   }

   private void showHealth() {
      SafeModeManager.Status safeMode = SafeModeManager.GLOBAL.status();
      ModuleConfigLoadReport.Snapshot configReport = ClientContext.moduleRegistry == null
         ? new ModuleConfigLoadReport.Snapshot(java.util.List.of(), 0)
         : ClientContext.moduleRegistry.getLastConfigLoadReport();
      TaskScheduler.Snapshot tasks = ClientContext.taskScheduler == null
         ? new TaskScheduler.Snapshot(0, 0, 0, 0)
         : ClientContext.taskScheduler.snapshot();

      ClientHealthSnapshot snapshot = new ClientHealthSnapshot(
         safeMode.active(),
         safeMode.armed(),
         safeMode.initializationFailed(),
         safeMode.unsafeMarker(),
         configReport.totalIssueCount(),
         EventFailureTracker.GLOBAL.totalFailures(),
         tasks.totalQueued(),
         tasks.totalOutstanding(),
         EventProfiler.GLOBAL.isEnabled()
      );

      send("Schizo health: " + (snapshot.hasWarnings() ? "attention" : "healthy"));
      send("Safe Mode: " + (snapshot.safeModeActive() ? "active" : "inactive")
         + (snapshot.safeModeArmed() ? " (armed for startup)" : ""));
      if (snapshot.safeModeInitializationFailed()) {
         send("Safe Mode initialization: failed closed");
      }
      if (snapshot.safeModeUnsafeMarker()) {
         send("Safe Mode marker: unsafe path/type");
      }
      send("Module config issues: " + snapshot.moduleConfigIssues());
      send("Event callback failures: " + snapshot.eventFailures());
      send("Scheduled tasks: " + snapshot.scheduledQueued() + " queued / "
         + snapshot.scheduledOutstanding() + " outstanding");
      send("Profiler: " + (snapshot.profilerEnabled() ? "sampling" : "idle"));

      int previewCount = Math.min(ISSUE_PREVIEW_LIMIT, configReport.issues().size());
      for (int index = 0; index < previewCount; index++) {
         ModuleConfigLoadReport.Issue issue = configReport.issues().get(index);
         send("Config issue " + (index + 1) + ": " + issue.moduleName() + " / "
            + issue.settingName() + " (" + issue.failureType() + ")");
      }
      if (configReport.totalIssueCount() > previewCount) {
         send("... " + (configReport.totalIssueCount() - previewCount) + " more config issues");
      }
   }

   private static void send(String text) {
      ChatUtils.printChat(Component.literal(text), ChatUtils.DEFAULT_SIGNATURE);
   }
}
