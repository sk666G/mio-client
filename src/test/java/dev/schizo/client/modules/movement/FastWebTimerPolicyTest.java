package dev.schizo.client.modules.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FastWebTimerPolicyTest {
   @Test
   void reportsStrictTimerOnlyWhileModuleIsEnabled() {
      assertTrue(FastWebModule.shouldReportStrictTimer(true, true));
      assertFalse(FastWebModule.shouldReportStrictTimer(false, true));
      assertFalse(FastWebModule.shouldReportStrictTimer(true, false));
      assertFalse(FastWebModule.shouldReportStrictTimer(false, false));
   }
}
