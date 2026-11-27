package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class FreeLookLifecycleTest {
   @Test
   void cleanupIsSafeWithoutAWorldOrPlayer() {
      FreeLookModule module = new FreeLookModule();
      assertDoesNotThrow(module::onDisable);
   }
}
