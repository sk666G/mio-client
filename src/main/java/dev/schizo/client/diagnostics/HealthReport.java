package dev.schizo.client.diagnostics;

import java.util.ArrayList;
import java.util.List;
import dev.schizo.client.integrations.IntegrationHealth;

/** Value-free, local diagnostics. No raw exceptions, settings, identifiers or addresses. */
public record HealthReport(ClientHealthSnapshot snapshot, IntegrationHealth baritone,
                           IntegrationHealth waypoints, boolean profilesEnabled,
                           boolean profileRecoveryPending) {
   public List<String> lines() {
      List<String> rows = new ArrayList<>();
      rows.add("Status: " + (snapshot.hasWarnings() || profileRecoveryPending ? "Attention" : "No recorded warnings"));
      rows.add("Safe Mode: " + (snapshot.safeModeActive() ? "active" : "inactive") + (snapshot.safeModeArmed() ? " / armed" : ""));
      rows.add("Startup recovery: " + (snapshot.safeModeInitializationFailed() || snapshot.safeModeUnsafeMarker() ? "attention" : "no recorded failure"));
      rows.add("Configuration issues: " + snapshot.moduleConfigIssues());
      rows.add("Callback failures: " + snapshot.eventFailures());
      rows.add("Queued tasks: " + snapshot.scheduledQueued());
      rows.add("Outstanding tasks: " + snapshot.scheduledOutstanding());
      rows.add("Profiler: " + (snapshot.profilerEnabled() ? "sampling" : "idle"));
      rows.add("Baritone: " + baritone.label());
      rows.add("Waypoints: " + waypoints.label());
      rows.add("Context profiles: " + (profilesEnabled ? "enabled" : "disabled"));
      rows.add("Profile recovery: " + (profileRecoveryPending ? "pending" : "no recorded failure"));
      return List.copyOf(rows);
   }

   public String clipboardText() {
      return "Schizo local health report\n" + String.join("\n", lines()) + "\n";
   }
}
