/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */
package dev.schizo.client.commands;

import dev.schizo.client.config.SafeModeManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.schizo.client.utils.chat.ChatUtils;
import java.io.IOException;
import net.minecraft.network.chat.Component;

public final class SafeModeCommand extends Command {
   private final SafeModeManager safeMode = SafeModeManager.GLOBAL;

   public SafeModeCommand() {
      super("safemode");
      this.setAliases("safe");
   }

   @Override
   public void exec(LiteralArgumentBuilder commandBuilder) {
      commandBuilder.executes(context -> this.showStatus());
      commandBuilder.then(literal("status").executes(context -> this.showStatus()));
      commandBuilder.then(literal("arm").executes(context -> {
         try {
            boolean created = this.safeMode.armForNextStartup();
            this.print(created
               ? "Safe Mode is armed for the next launch. Restart Schizo to enter Safe Mode."
               : "Safe Mode is already armed for the next launch.");
         } catch (IOException | IllegalStateException failure) {
            this.print("Could not arm Safe Mode: " + failure.getMessage());
         }
         return 1;
      }));
      commandBuilder.then(literal("disarm").executes(context -> {
         try {
            boolean removed = this.safeMode.disarmForNextStartup();
            if (this.safeMode.isActive()) {
               this.print(removed
                  ? "Safe Mode is disabled for the next launch. This session remains in Safe Mode until restart."
                  : "No Safe Mode marker was present. This session remains in Safe Mode until restart.");
            } else {
               this.print(removed
                  ? "Safe Mode is disarmed. Normal startup will resume on the next launch."
                  : "Safe Mode is not armed.");
            }
         } catch (IOException | IllegalStateException failure) {
            this.print("Could not disarm Safe Mode: " + failure.getMessage());
         }
         return 1;
      }));
   }

   private int showStatus() {
      SafeModeManager.Status status = this.safeMode.status();
      if (status.initializationFailed()) {
         this.print("Safe Mode fail-safe is active because the configuration safety state could not be checked.");
      } else if (status.unsafeMarker()) {
         this.print("Safe Mode is active: the marker is not a regular file. Use ;safemode disarm to remove it.");
      } else if (status.active()) {
         this.print("Safe Mode is ACTIVE. All modules are blocked; module enablement settings are preserved.");
      } else if (status.armed()) {
         this.print("Safe Mode is armed for the next launch; the current session is unchanged.");
      } else {
         this.print("Safe Mode is inactive. Use ;safemode arm and restart for a diagnostic startup.");
      }
      return 1;
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
