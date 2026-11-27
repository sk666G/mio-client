package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.HoldMacro;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.options.MacroType;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MacroRegistryDispatchMutationTest {
   @Test
   void triggerCanUnregisterLaterMacroWithoutConcurrentModification() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger removedCalls = new AtomicInteger();
         TestMacro removed = new TestMacro("removed", removedCalls::incrementAndGet);
         TestMacro remover = new TestMacro("remover", () -> registry.unregisterEntry(removed));
         assertTrue(registry.registerEntry(remover));
         assertTrue(registry.registerEntry(removed));

         assertDoesNotThrow(() -> registry.runMatchingMacros(65, false));

         assertEquals(0, removedCalls.get());
         assertEquals(1, registry.getRegistry().size());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void newlyRegisteredMacroWaitsUntilNextTrigger() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger earlyCalls = new AtomicInteger();
         AtomicInteger lateCalls = new AtomicInteger();
         TestMacro late = new TestMacro("late", lateCalls::incrementAndGet);
         TestMacro early = new TestMacro("early", () -> {
            earlyCalls.incrementAndGet();
            registry.registerEntry(late);
         });
         assertTrue(registry.registerEntry(early));

         registry.runMatchingMacros(65, false);
         assertEquals(1, earlyCalls.get());
         assertEquals(0, lateCalls.get());

         registry.runMatchingMacros(65, false);
         assertEquals(2, earlyCalls.get());
         assertEquals(1, lateCalls.get());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void replacementDoesNotExecuteUsingStaleSnapshotIdentity() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger oldCalls = new AtomicInteger();
         AtomicInteger newCalls = new AtomicInteger();
         TestMacro previous = new TestMacro("same", oldCalls::incrementAndGet);
         TestMacro replacement = new TestMacro("same", newCalls::incrementAndGet);
         TestMacro replacer = new TestMacro("replacer", () -> registry.replaceMacro(previous, replacement));
         assertTrue(registry.registerEntry(replacer));
         assertTrue(registry.registerEntry(previous));

         registry.runMatchingMacros(65, false);

         assertEquals(0, oldCalls.get());
         assertEquals(0, newCalls.get());
         assertTrue(registry.getRegistry().stream().anyMatch(macro -> macro == replacement));

         registry.runMatchingMacros(65, false);
         assertEquals(1, newCalls.get());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void holdDispatchSkipsRemovedEntriesAfterTheirLifecycleRelease() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger removedUpdateCalls = new AtomicInteger();
         TestHoldMacro removed = new TestHoldMacro("removed", removedUpdateCalls::incrementAndGet);
         TestHoldMacro remover = new TestHoldMacro("remover", () -> registry.unregisterEntry(removed));
         assertTrue(registry.registerEntry(remover));
         assertTrue(registry.registerEntry(removed));

         assertDoesNotThrow(() -> registry.updateHoldMacros(false));

         // Unregistration deliberately calls updatePressed(false) exactly once.
         // The stale iteration must not call it a second time.
         assertEquals(1, removedUpdateCalls.get());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void configurationReloadSkipsHoldMacrosRemovedDuringAnotherRelease() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger removedUpdateCalls = new AtomicInteger();
         TestHoldMacro removed = new TestHoldMacro("removed", removedUpdateCalls::incrementAndGet);
         TestHoldMacro remover = new TestHoldMacro("remover", () -> registry.unregisterEntry(removed));
         assertTrue(registry.registerEntry(remover));
         assertTrue(registry.registerEntry(removed));

         com.google.gson.JsonObject root = new com.google.gson.JsonObject();
         root.add("macros", new com.google.gson.JsonArray());
         assertDoesNotThrow(() -> registry.fromJson(root));

         // Unregistering the later macro releases it once. The outer reload
         // snapshot must not release the already-removed instance again.
         assertEquals(1, removedUpdateCalls.get());
         assertTrue(registry.getRegistry().isEmpty());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void staleMacroReferenceCannotRemoveEqualNamedReplacement() {
      MacroRegistry registry = new MacroRegistry();
      try {
         AtomicInteger staleReleases = new AtomicInteger();
         AtomicInteger liveReleases = new AtomicInteger();
         TestHoldMacro previous = new TestHoldMacro("same", staleReleases::incrementAndGet);
         TestHoldMacro current = new TestHoldMacro("same", liveReleases::incrementAndGet);
         assertTrue(registry.registerEntry(previous));
         assertTrue(registry.replaceMacro(previous, current));

         assertFalse(registry.unregisterEntry(previous));

         assertEquals(1, staleReleases.get());
         assertEquals(0, liveReleases.get());
         assertSame(current, registry.getRegistry().iterator().next());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void staleMacroReferenceCannotReplaceEqualNamedLiveEntry() {
      MacroRegistry registry = new MacroRegistry();
      try {
         TestMacro previous = new TestMacro("same", () -> {});
         TestMacro current = new TestMacro("same", () -> {});
         TestMacro newer = new TestMacro("same", () -> {});
         assertTrue(registry.registerEntry(previous));
         assertTrue(registry.replaceMacro(previous, current));

         assertFalse(registry.replaceMacro(previous, newer));

         assertSame(current, registry.getRegistry().iterator().next());
         assertEquals(1, registry.getRegistry().size());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   private static final class TestMacro extends Macro {
      private final Runnable action;

      private TestMacro(String name, Runnable action) {
         super(name, MacroType.SIMPLE, Keybind.toggle(65, false));
         this.action = action;
      }

      @Override
      public void run() {
         action.run();
      }
   }

   private static final class TestHoldMacro extends HoldMacro {
      private final Runnable action;

      private TestHoldMacro(String name, Runnable action) {
         super(name, Keybind.toggle(65, false));
         this.action = action;
      }

      @Override
      public void updatePressed(boolean pressed) {
         action.run();
      }
   }
}
