package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModuleLifecycleTest {
   @Test
   void failedEnableRollsBackSubscriptionAndRunsCleanup() {
      TestModule module = new TestModule();
      module.failEnable = true;

      assertThrows(IllegalStateException.class, module::enable);
      assertFalse(module.isToggled());
      assertEquals(1, module.disableCalls.get());

      ClientAccess.eventBus.post(new ProbeEvent());
      assertEquals(0, module.eventCalls.get());
   }

   @Test
   void failedDisableStillLeavesModuleDisabledAndUnsubscribed() {
      TestModule module = new TestModule();
      module.enable();
      module.failDisable = true;

      assertThrows(IllegalStateException.class, module::disable);
      assertFalse(module.isToggled());

      ClientAccess.eventBus.post(new ProbeEvent());
      assertEquals(0, module.eventCalls.get());
   }

   private static final class ProbeEvent {
   }

   private static final class TestModule extends Module {
      private final AtomicInteger eventCalls = new AtomicInteger();
      private final AtomicInteger disableCalls = new AtomicInteger();
      private boolean failEnable;
      private boolean failDisable;

      private TestModule() {
         super("LifecycleTest", "Lifecycle regression test", ModuleCategory.MISC);
      }

      @Override
      public void onEnable() {
         if (this.failEnable) {
            throw new IllegalStateException("enable failure");
         }
      }

      @Override
      public void onDisable() {
         this.disableCalls.incrementAndGet();
         if (this.failDisable) {
            throw new IllegalStateException("disable failure");
         }
      }

      @EventHandler
      private void onProbe(ProbeEvent event) {
         this.eventCalls.incrementAndGet();
      }
   }
}
