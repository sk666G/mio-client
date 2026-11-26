package dev.schizo.client.gametest;

import dev.schizo.client.ClientContext;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.modules.client.UIModule;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StartupTrace {
   public enum Phase {
      BEFORE_MAIN, MAIN_ENTERED, FONT_READY, REGISTRY_CONSTRUCTED, MIXINS_BOUND,
      MAIN_COMPLETE, DEPENDENCIES_BOUND, CLIENT_EVENT, CONFIG_LOADING, CLIENT_COMPLETE
   }

   public enum Hook { TITLE_RENDER, GUI_SCREEN, WORLD_RENDER, PLAY_LOGIN, INPUT_TICK }

   private static volatile Phase phase = Phase.BEFORE_MAIN;
   private static final Map<Hook, Phase> firstHooks = new ConcurrentHashMap<>();

   private StartupTrace() {}

   public static void advance(Phase next) {
      if (next.ordinal() != phase.ordinal() + 1) {
         throw new AssertionError("Unexpected startup order: " + phase + " -> " + next);
      }
      phase = next;
      SchizoClient.LOGGER.info("Gametest startup phase {}", next);
   }

   public static void observe(Hook hook) {
      firstHooks.putIfAbsent(hook, phase);
   }

   public static void verify() {
      if (phase != Phase.CLIENT_COMPLETE) {
         throw new AssertionError("Startup stopped at " + phase);
      }
      UIModule canonical = ClientContext.getModule(UIModule.class);
      if (canonical == null || MixinModuleRegistry.getModule(UIModule.class) != canonical) {
         throw new AssertionError("UI module identity differs between client and mixin registries");
      }
      Map<Hook, Phase> observations = new EnumMap<>(Hook.class);
      observations.putAll(firstHooks);
      SchizoClient.LOGGER.info("First observed gametest hooks {}", observations);
      for (Map.Entry<Hook, Phase> entry : observations.entrySet()) {
         if (entry.getKey() != Hook.GUI_SCREEN && entry.getValue() != Phase.CLIENT_COMPLETE) {
            throw new AssertionError(entry.getKey() + " first ran during " + entry.getValue());
         }
      }
      for (Hook hook : new Hook[]{Hook.GUI_SCREEN, Hook.WORLD_RENDER, Hook.PLAY_LOGIN, Hook.INPUT_TICK}) {
         if (!observations.containsKey(hook)) {
            throw new AssertionError("Gametest did not exercise " + hook);
         }
      }
   }
}
