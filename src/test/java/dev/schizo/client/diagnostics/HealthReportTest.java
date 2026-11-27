package dev.schizo.client.diagnostics;

import dev.schizo.client.integrations.IntegrationHealth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HealthReportTest {
   @Test void reportsCountsAndRecoveryWithoutRawValues() {
      var health = new ClientHealthSnapshot(false, true, false, false, 2, 3, 4, 5, true);
      var report = new HealthReport(health, IntegrationHealth.UNAVAILABLE, IntegrationHealth.AVAILABLE, true, true, 2);
      String text = report.clipboardText();
      assertTrue(text.contains("Configuration issues: 2"));
      assertTrue(text.contains("Callback failures: 3"));
      assertTrue(text.contains("Outstanding tasks: 5"));
      assertTrue(text.contains("Baritone: unavailable"));
      assertTrue(text.contains("Profile recovery: pending"));
      assertTrue(text.contains("Status: Attention"));
      assertTrue(text.contains("Shared module input groups: 2"));
      assertThrows(UnsupportedOperationException.class, () -> report.lines().add("unexpected"));
   }
}
