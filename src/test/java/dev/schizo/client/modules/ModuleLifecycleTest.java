package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.ModuleToggleEvent;
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
   void toggleNotificationFailureStillRunsLocalToggleHookAndRollsBackEnable() {
      TestModule module = new TestModule();
      FailingToggleObserver observer = new FailingToggleObserver();
      ClientAccess.eventBus.subscribe(observer);
      try {
         assertThrows(IllegalStateException.class, module::enable);
         assertFalse(module.isToggled());
         assertEquals(1, module.toggleCalls.get());
         assertEquals(0, module.enableCalls.get());
         assertEquals(1, module.disableCalls.get());
      } finally {
         ClientAccess.eventBus.unsubscribe(observer);
      }
   }


   @Test
   void failedLocalToggleHookRunsCleanupAndUnsubscribes() {
      TestModule module = new TestModule();
      module.failToggle = true;

      assertThrows(IllegalStateException.class, module::enable);
      assertFalse(module.isToggled());
      assertEquals(1, module.toggleCalls.get());
      assertEquals(0, module.enableCalls.get());
      assertEquals(1, module.disableCalls.get());

      ClientAccess.eventBus.post(new ProbeEvent());
      assertEquals(0, module.eventCalls.get());
   }

   @Test
   void toggleNotificationFailureStillRunsDisableHooks() {
      TestModule module = new TestModule();
      module.enable();
      FailingToggleObserver observer = new FailingToggleObserver();
      ClientAccess.eventBus.subscribe(observer);
      try {
         assertThrows(IllegalStateException.class, module::disable);
         assertFalse(module.isToggled());
         assertEquals(2, module.toggleCalls.get());
         assertEquals(1, module.disableCalls.get());
      } finally {
         ClientAccess.eventBus.unsubscribe(observer);
      }
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

   private static final class FailingToggleObserver {
      @EventHandler
      private void onToggle(ModuleToggleEvent event) {
         throw new IllegalStateException("observer failure");
      }
   }

   private static final class TestModule extends Module {
      private final AtomicInteger eventCalls = new AtomicInteger();
      private final AtomicInteger toggleCalls = new AtomicInteger();
      private final AtomicInteger enableCalls = new AtomicInteger();
      private final AtomicInteger disableCalls = new AtomicInteger();
      private boolean failToggle;
      private boolean failEnable;
      private boolean failDisable;

      private TestModule() {
         super("LifecycleTest", "Lifecycle regression test", ModuleCategory.MISC);
      }

      @Override
      public void onToggle() {
         this.toggleCalls.incrementAndGet();
         if (this.failToggle) {
            throw new IllegalStateException("toggle failure");
         }
      }

      @Override
      public void onEnable() {
         this.enableCalls.incrementAndGet();
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
