package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class XrayLifecycleTest {
   @Test
   void gammaRestoreRequiresXrayToStillOwnCurrentValue() {
      assertTrue(XrayModule.shouldRestoreGamma(true, 1000.0D, 1000.0D));
      assertFalse(XrayModule.shouldRestoreGamma(false, 1000.0D, 1000.0D));
      assertFalse(XrayModule.shouldRestoreGamma(true, 1.0D, 1000.0D));
   }
}
