/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo compatibility warning listener.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.modules.Module;
import dev.schizo.client.utils.chat.ChatUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Emits deduplicated, informational compatibility warnings while connected to a remote server. */
public final class MultiplayerCompatibilityWarnings implements ClientAccess {
   private static final int MAX_CHAT_WARNINGS_PER_CONNECTION = 8;
   private static final Set<Class<?>> WARNED_MODULES = ConcurrentHashMap.newKeySet();
   private static final AtomicInteger CHAT_WARNINGS_SHOWN = new AtomicInteger();
   private static volatile boolean inspectionPending = true;

   public MultiplayerCompatibilityWarnings() {
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   public static void warnIfNeeded(Module module) {
      if (module == null || !isMultiplayerSession()) {
         return;
      }

      Minecraft minecraft = ClientAccess.schizoMinecraft;
      // Do not mark a warning as displayed until the chat message can be emitted on the client thread.
      if (minecraft == null || !minecraft.isSameThread()) {
         return;
      }

      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(module.getClass());
      boolean policyBlocked = MultiplayerCompatibilityPolicy.GLOBAL.shouldBlock(module);
      if (!assessment.requiresMultiplayerWarning() || !WARNED_MODULES.add(module.getClass())) {
         return;
      }

      ClientAccess.logger.warn(
         "Multiplayer compatibility warning for {}: {} — {}",
         module.getName(),
         assessment.classification(),
         assessment.reason()
      );

      int warningsShown = CHAT_WARNINGS_SHOWN.incrementAndGet();
      if (warningsShown <= MAX_CHAT_WARNINGS_PER_CONNECTION) {
         String className = module.getClass().getSimpleName();
         if (className.endsWith("Module")) {
            className = className.substring(0, className.length() - "Module".length());
         }
         String message = "Multiplayer compatibility: " + module.getName()
            + " is " + assessment.classification() + ". " + assessment.reason()
            + (policyBlocked
               ? " Activation blocked by the conservative profile; use ;compat policy allow " + className + " only after reviewing it."
               : "")
            + " See ;compat module " + className + ".";
         ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
      } else if (warningsShown == MAX_CHAT_WARNINGS_PER_CONNECTION + 1) {
         ChatUtils.printChat(
            Component.literal("Additional compatibility warnings were suppressed. Use ;compat list for the full inventory."),
            ChatUtils.DEFAULT_SIGNATURE
         );
      }
   }

   @EventHandler(priority = 1000)
   private void inspectActiveModules(LocalPlayerPreTickEvent event) {
      if (inspectionPending) {
         enforceActiveModules();
      }
   }

   /** Applies a newly enabled policy to active modules. Suspended modules are not auto-reactivated. */
   public static void enforceActiveModules() {
      if (!isMultiplayerSession() || ClientContext.moduleRegistry == null) {
         inspectionPending = true;
         return;
      }

      inspectionPending = false;
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)entry;
         if (!module.isToggled()) {
            continue;
         }
         warnIfNeeded(module);
         if (MultiplayerCompatibilityPolicy.GLOBAL.shouldBlock(module)) {
            module.suspendForCompatibilityPolicy();
         }
      }
   }

   @EventHandler(priority = -100)
   private void resetForNewConnection(ConnectScreenConnectEvent event) {
      requestInspection();
      resetWarnings();
   }

   @EventHandler
   private void resetAfterDisconnect(MinecraftDisconnectEvent event) {
      requestInspection();
      resetWarnings();
   }

   public static void requestInspection() {
      inspectionPending = true;
   }

   public static boolean isMultiplayerSession() {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      return minecraft != null
         && minecraft.isSameThread()
         && minecraft.player != null
         && minecraft.level != null
         && !minecraft.hasSingleplayerServer();
   }

   private static void resetWarnings() {
      WARNED_MODULES.clear();
      CHAT_WARNINGS_SHOWN.set(0);
   }
}
