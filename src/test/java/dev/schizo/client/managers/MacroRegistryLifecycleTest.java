package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.HoldMacro;
import dev.schizo.client.settings.options.PresetCategory;
import org.junit.jupiter.api.Test;

class MacroRegistryLifecycleTest {
   @Test
   void disablingHoldMacroRequestsRelease() {
      TrackingHoldMacro macro = new TrackingHoldMacro("hold");
      macro.updatePressed(true);

      macro.setToggled(false);

      assertFalse(macro.isToggled());
      assertFalse(macro.lastPressed);
      assertEquals(2, macro.updateCalls);
   }

   @Test
   void unregisteringPressedHoldMacroRequestsRelease() {
      MacroRegistry registry = new MacroRegistry();
      try {
         TrackingHoldMacro macro = new TrackingHoldMacro("hold");
         assertTrue(registry.registerEntry(macro));
         macro.updatePressed(true);

         assertTrue(registry.unregisterEntry(macro));

         assertFalse(macro.lastPressed);
         assertTrue(registry.getRegistry().isEmpty());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void replacingPressedHoldMacroReleasesPreviousBinding() {
      MacroRegistry registry = new MacroRegistry();
      try {
         TrackingHoldMacro previous = new TrackingHoldMacro("hold");
         TrackingHoldMacro replacement = new TrackingHoldMacro("hold");
         assertTrue(registry.registerEntry(previous));
         previous.updatePressed(true);

         assertTrue(registry.replaceMacro(previous, replacement));

         assertFalse(previous.lastPressed);
         assertSame(replacement, registry.getRegistry().iterator().next());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void configurationReloadReleasesPressedHoldMacrosBeforeReplacingState() {
      MacroRegistry registry = new MacroRegistry();
      try {
         TrackingHoldMacro macro = new TrackingHoldMacro("hold");
         assertTrue(registry.registerEntry(macro));
         macro.updatePressed(true);

         JsonObject root = new JsonObject();
         root.add("macros", new JsonArray());
         registry.fromJson(root);

         assertFalse(macro.lastPressed);
         assertTrue(registry.getRegistry().isEmpty());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void macroPresetLoadUsesLifecycleSafeRegistryReplacement() {
      MacroRegistry registry = new MacroRegistry();
      MacroRegistry previousRegistry = ClientContext.macroRegistry;
      ClientContext.macroRegistry = registry;
      try {
         TrackingHoldMacro macro = new TrackingHoldMacro("hold");
         assertTrue(registry.registerEntry(macro));
         macro.updatePressed(true);

         JsonObject root = new JsonObject();
         root.add("macros", new JsonArray());
         PresetCategory.MACRO.fromJson(root);

         assertFalse(macro.lastPressed);
         assertTrue(registry.getRegistry().isEmpty());
      } finally {
         ClientContext.macroRegistry = previousRegistry;
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void disconnectReleasesPressedHoldMacros() {
      MacroRegistry registry = new MacroRegistry();
      try {
         TrackingHoldMacro macro = new TrackingHoldMacro("hold");
         assertTrue(registry.registerEntry(macro));
         macro.updatePressed(true);

         ClientAccess.eventBus.post(new MinecraftDisconnectEvent());

         assertFalse(macro.lastPressed);
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void disablingHoldMacroContainsReleaseFailure() {
      ThrowingReleaseHoldMacro macro = new ThrowingReleaseHoldMacro("hold");

      assertDoesNotThrow(() -> macro.setToggled(false));

      assertFalse(macro.isToggled());
      assertEquals(1, macro.releaseAttempts);
   }

   @Test
   void oneHoldMacroReleaseFailureDoesNotBlockDisconnectCleanup() {
      MacroRegistry registry = new MacroRegistry();
      try {
         ThrowingReleaseHoldMacro failing = new ThrowingReleaseHoldMacro("failing");
         TrackingHoldMacro healthy = new TrackingHoldMacro("healthy");
         assertTrue(registry.registerEntry(failing));
         assertTrue(registry.registerEntry(healthy));
         healthy.updatePressed(true);

         assertDoesNotThrow(() -> ClientAccess.eventBus.post(new MinecraftDisconnectEvent()));

         assertEquals(1, failing.releaseAttempts);
         assertFalse(healthy.lastPressed);
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void unregisterStillRemovesMacroWhenReleaseFails() {
      MacroRegistry registry = new MacroRegistry();
      try {
         ThrowingReleaseHoldMacro macro = new ThrowingReleaseHoldMacro("hold");
         assertTrue(registry.registerEntry(macro));

         assertTrue(registry.unregisterEntry(macro));

         assertEquals(1, macro.releaseAttempts);
         assertTrue(registry.getRegistry().isEmpty());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   private static final class TrackingHoldMacro extends HoldMacro {
      private boolean lastPressed;
      private int updateCalls;

      private TrackingHoldMacro(String name) {
         super(name, Keybind.toggle(65, false));
      }

      @Override
      public void updatePressed(boolean pressed) {
         this.lastPressed = pressed;
         this.updateCalls++;
      }
   }

   private static final class ThrowingReleaseHoldMacro extends HoldMacro {
      private int releaseAttempts;

      private ThrowingReleaseHoldMacro(String name) {
         super(name, Keybind.toggle(65, false));
      }

      @Override
      public void updatePressed(boolean pressed) {
         if (!pressed) {
            this.releaseAttempts++;
            throw new IllegalStateException("release failure");
         }
      }
   }
}
