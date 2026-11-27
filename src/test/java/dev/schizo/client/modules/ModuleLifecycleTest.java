package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.InputOverrideManager;
import java.lang.reflect.Field;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.ModuleToggleEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModuleLifecycleTest {
   @Test
   void failedEnableRollsBackSubscriptionAndRunsCleanup() {
      TestModule module = new TestModule();
      module.failEnable = true;

      assertThrows(IllegalStateException.class, module::enable);
      assertFalse(module.isToggled());
      assertEquals(2, module.toggleCalls.get());
      assertFalse(module.lastToggleState);
      assertEquals(1, module.disableCalls.get());
      assertFalse(module.toggledDuringDisable);

      ClientAccess.eventBus.post(new ProbeEvent());
      assertEquals(0, module.eventCalls.get());
   }

   @Test
   void failedEnablePublishesDisabledRollbackState() {
      TestModule module = new TestModule();
      module.failEnable = true;
      RecordingToggleObserver observer = new RecordingToggleObserver(module);
      ClientAccess.eventBus.subscribe(observer);
      try {
         assertThrows(IllegalStateException.class, module::enable);
         assertEquals(List.of(true, false), observer.states);
      } finally {
         ClientAccess.eventBus.unsubscribe(observer);
      }
   }

   @Test
   void toggleNotificationFailureStillRunsLocalToggleHookAndRollsBackEnable() {
      TestModule module = new TestModule();
      FailingToggleObserver observer = new FailingToggleObserver();
      ClientAccess.eventBus.subscribe(observer);
      try {
         assertThrows(IllegalStateException.class, module::enable);
         assertFalse(module.isToggled());
         assertEquals(2, module.toggleCalls.get());
         assertFalse(module.lastToggleState);
         assertEquals(0, module.enableCalls.get());
         assertEquals(0, module.disableCalls.get());
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
      assertEquals(2, module.toggleCalls.get());
      assertFalse(module.lastToggleState);
      assertEquals(0, module.enableCalls.get());
      assertEquals(0, module.disableCalls.get());

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

   @Test
   void disableClearsModuleOwnedPendingInput() throws Exception {
      InputOverrideManager previous = ClientContext.inputOverrides;
      InputOverrideManager manager = new InputOverrideManager();
      TestModule module = new TestModule();
      ClientContext.inputOverrides = manager;
      try {
         module.enable();
         manager.requestJump(module, true);
         manager.requestForward(module, true);

         module.disable();

         assertFalse(isPending(manager, "pendingJump"));
         assertFalse(isPending(manager, "pendingForward"));
      } finally {
         if (module.isToggled()) {
            module.disable();
         }
         ClientContext.inputOverrides = previous;
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   @Test
   void failedEnableRollbackClearsModuleOwnedPendingInput() throws Exception {
      InputOverrideManager previous = ClientContext.inputOverrides;
      InputOverrideManager manager = new InputOverrideManager();
      TestModule module = new TestModule();
      module.requestInputOnEnable = true;
      module.failEnable = true;
      ClientContext.inputOverrides = manager;
      try {
         assertThrows(IllegalStateException.class, module::enable);

         assertFalse(isPending(manager, "pendingJump"));
         assertFalse(isPending(manager, "pendingForward"));
      } finally {
         ClientContext.inputOverrides = previous;
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   private static boolean isPending(InputOverrideManager manager, String fieldName) throws Exception {
      Field pendingField = InputOverrideManager.class.getDeclaredField(fieldName);
      pendingField.setAccessible(true);
      Object pendingValue = pendingField.get(manager);

      Field pending = pendingValue.getClass().getSuperclass().getDeclaredField("pending");
      pending.setAccessible(true);
      return pending.getBoolean(pendingValue);
   }

   private static final class ProbeEvent {
   }

   private static final class RecordingToggleObserver {
      private final Module target;
      private final List<Boolean> states = new ArrayList<>();

      private RecordingToggleObserver(Module target) {
         this.target = target;
      }

      @EventHandler
      private void onToggle(ModuleToggleEvent event) {
         if (event.getModule() == this.target) {
            this.states.add(event.getModule().isToggled());
         }
      }
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
      private boolean requestInputOnEnable;
      private boolean lastToggleState;
      private boolean toggledDuringDisable;

      private TestModule() {
         super("LifecycleTest", "Lifecycle regression test", ModuleCategory.MISC);
      }

      @Override
      public void onToggle() {
         this.toggleCalls.incrementAndGet();
         this.lastToggleState = this.isToggled();
         if (this.failToggle) {
            throw new IllegalStateException("toggle failure");
         }
      }

      @Override
      public void onEnable() {
         this.enableCalls.incrementAndGet();
         if (this.requestInputOnEnable) {
            ClientContext.inputOverrides.requestJump(this, true);
            ClientContext.inputOverrides.requestForward(this, true);
         }
         if (this.failEnable) {
            throw new IllegalStateException("enable failure");
         }
      }

      @Override
      public void onDisable() {
         this.disableCalls.incrementAndGet();
         this.toggledDuringDisable = this.isToggled();
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
