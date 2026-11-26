package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.schizo.client.integrations.baritone.DisabledBaritoneBridge;
import dev.schizo.client.integrations.xaero.DisabledWaypointAdapter;
import org.junit.jupiter.api.Test;

class IntegrationHealthTest {
   @Test
   void disabledAdaptersReportUnavailableHealth() {
      assertEquals(IntegrationHealth.UNAVAILABLE, new DisabledBaritoneBridge().health());
      assertEquals(IntegrationHealth.UNAVAILABLE, new DisabledWaypointAdapter().health());
   }

   @Test
   void labelsAreStableForDiagnostics() {
      assertEquals("connecting", IntegrationHealth.CONNECTING.label());
      assertEquals("connected", IntegrationHealth.CONNECTED.label());
      assertEquals("not configured", IntegrationHealth.NOT_CONFIGURED.label());
   }
}
