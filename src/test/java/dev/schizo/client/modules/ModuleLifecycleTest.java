package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import org.junit.jupiter.api.Test;

class ModuleLifecycleTest {
   @Test
   void failedEnableRollsBackEnabledStateAndSubscription() {
      FailingEnableModule module = new FailingEnableModule();

      IllegalStateException failure = assertThrows(IllegalStateException.class, module::enable);

      assertEquals("boom", failure.getMessage());
      assertFalse(module.isToggled());
      assertEquals(2, module.toggleCalls);
      assertEquals(1, module.disableCalls);

      ClientAccess.eventBus.post(new ProbeEvent());
      assertEquals(0, module.probeCalls);
   }

   private static final class ProbeEvent {}

   private static final class FailingEnableModule extends Module {
      int toggleCalls;
      int disableCalls;
      int probeCalls;

      FailingEnableModule() {
         super("LifecycleProbe", "Lifecycle test module", ModuleCategory.CLIENT);
      }

      @Override
      public void onEnable() {
         throw new IllegalStateException("boom");
      }

      @Override
      public void onToggle() {
         this.toggleCalls++;
      }

      @Override
      public void onDisable() {
         this.disableCalls++;
      }

      @EventHandler
      private void onProbe(ProbeEvent event) {
         this.probeCalls++;
      }
   }
}
