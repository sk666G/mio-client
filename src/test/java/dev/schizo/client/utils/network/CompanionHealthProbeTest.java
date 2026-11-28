package dev.schizo.client.utils.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanionHealthProbeTest {
   @Test void acceptsOnlyExplicitHttpsHealthEndpoint() {
      assertEquals("relay.example", CompanionHealthProbe.validateEndpoint("https://relay.example:443/health").getHost());
      for (String bad : new String[]{"http://relay.example/health", "https://user@relay.example/health",
         "https://relay.example/health?token=secret", "https://relay.example/health#x",
         "https://relay.example/other", "https:///health", "https://relay.example:0/health",
         "https://relay.example:65536/health"}) {
         assertThrows(IllegalArgumentException.class, () -> CompanionHealthProbe.validateEndpoint(bad), bad);
      }
   }

   @Test void rejectsMissingOrUnsafeTokens() {
      for (String bad : new String[]{"", "short", "a".repeat(32) + "\r\n", "a".repeat(257)}) {
         assertThrows(IllegalArgumentException.class,
            () -> new CompanionHealthProbe("https://relay.example/health", bad));
      }
   }

   @Test void reportsRealRequestDurationOnlyForSuccessfulHealthResponse() {
      assertEquals(new CompanionHealthProbe.Result("Reachable", 25), CompanionHealthProbe.classify(204,25_999_999));
      assertEquals(-1, CompanionHealthProbe.classify(401, 10).millis());
      assertEquals(-1, CompanionHealthProbe.classify(302, 10).millis());
      assertEquals(-1, CompanionHealthProbe.classify(200, 10).millis());
   }
}
