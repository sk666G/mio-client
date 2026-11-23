/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.misc.WebhookNotifsModule;
import dev.schizo.client.utils.chat.ChatUtils;

import net.minecraft.network.chat.Component;

/** Reports local integration configuration and lifecycle state without making network probes. */
public final class IntegrationCommand extends Command {
   public IntegrationCommand() {
      super("integrations");
      this.setAliases("integration", "integrationstatus");
   }

   @Override
   public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder builder) {
      builder.executes(context -> {
         this.showIntegrations();
         return 1;
      });
      builder.then(literal("status").executes(context -> {
         this.showIntegrations();
         return 1;
      }));
   }

   private void showIntegrations() {
      DiscordModule discord = ClientContext.getModule(DiscordModule.class);
      WebhookNotifsModule notifications = ClientContext.getModule(WebhookNotifsModule.class);

      String discordState = discord == null ? "unavailable" : discord.getConnectionStatus();
      String baritoneState = ClientContext.baritoneBridge != null
         && ClientContext.baritoneBridge.isAvailable() ? "available" : "not installed";
      String waypointState = ClientContext.waypointAdapter != null
         && ClientContext.waypointAdapter.isAvailable() ? "available" : "adapter inactive";

      String webhookState = ClientContext.webhookManager.isConfigured()
         ? ClientContext.webhookManager.getDestinationDescription()
         : "not configured";
      String notificationState = notifications == null
         ? "unavailable"
         : notifications.isToggled() ? "enabled" : "disabled";
      String deliveryState;
      if (notifications == null) {
         deliveryState = "unavailable";
      } else if (notifications.isDeliveryPaused()) {
         deliveryState = "paused";
      } else if (notifications.isBatchInFlight()) {
         deliveryState = "sending";
      } else {
         deliveryState = "idle";
      }

      String output = """
         Schizo integrations
         Discord IPC: %s
         Webhook / Telegram destination: %s
         Chat notification module: %s
         Notification delivery: %s (%d queued, %d recent records)
         Baritone: %s
         Xaero Waypoints: %s
         """.formatted(
            discordState,
            webhookState,
            notificationState,
            deliveryState,
            notifications == null ? 0 : notifications.getPendingMessageCount(),
            notifications == null ? 0 : notifications.getDeliveryHistory().size(),
            baritoneState,
            waypointState
         );
      ChatUtils.printChat(Component.literal(output.stripTrailing()), ChatUtils.DEFAULT_SIGNATURE);
   }
}
