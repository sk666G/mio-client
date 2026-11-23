/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.misc.WebhookNotifsModule;
import dev.schizo.client.managers.NotificationDeliveryHistory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.managers.WebhookManager;
import net.minecraft.network.chat.Component;

public final class WebhookCommand extends Command {
   public WebhookCommand() {
      super("webhook");
   }

   @Override
   public void exec(LiteralArgumentBuilder builder) {
      builder.then(literal("get").executes(context -> {
         reply(ClientContext.webhookManager.getDestinationDescription(), -1035361);
         return 1;
      }));
      builder.then(literal("status").executes(context -> showStatus()));
      builder.then(literal("history").executes(context -> showHistory()));
      builder.then(literal("retry").executes(context -> retryDelivery()));
      builder.then(literal("clearhistory").executes(context -> clearHistory()));
      builder.then(literal("set").then(argument("url", StringArgumentType.greedyString()).executes(context -> {
         String url = ((String)context.getArgument("url", String.class)).trim();
         if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
         try {
            WebhookManager manager = ClientContext.webhookManager;
            manager.setUrl(url);
            reply("Notification destination updated: " + manager.getDestinationDescription(), -1035362);
         } catch (IllegalArgumentException invalid) {
            reply("Invalid webhook URL. Telegram URLs require /bot<token>/sendMessage?chat_id=<id>.", -1035362);
         }
         return 1;
      })));
      builder.then(literal("telegram").then(argument("botToken", FancyStringArgument.fancyString())
         .then(argument("chatId", FancyStringArgument.fancyString()).executes(context -> {
            try {
               ClientContext.webhookManager.setTelegram((String)context.getArgument("botToken", String.class),
                  (String)context.getArgument("chatId", String.class));
               reply("Telegram notifications configured for this session. The bot token is not saved; re-enter it after restart.", -1035362);
            } catch (IllegalArgumentException invalid) {
               reply("Invalid Telegram credentials. Use webhook telegram <bot-token> <chat-id>.", -1035362);
            }
            return 1;
         }))));
      builder.then(literal("chatid").then(argument("id", FancyStringArgument.fancyString()).executes(context -> {
         WebhookManager manager = ClientContext.webhookManager;
         if (!manager.isTelegram()) {
            reply("Chat ID is not supported for current webhook.", -1);
            return 1;
         }
         String chatId = (String)context.getArgument("id", String.class);
         try {
            manager.setTelegramChatId(chatId);
            ChatUtils.printChat(Component.literal("Your webhook Chat ID has been updated to ").append(this.formatText(chatId)), ChatUtils.DEFAULT_SIGNATURE);
         } catch (IllegalArgumentException invalid) {
            reply("Invalid Telegram chat ID.", -1);
         }
         return 1;
      })).executes(context -> {
         WebhookManager manager = ClientContext.webhookManager;
         if (manager.isTelegram()) {
            ChatUtils.printChat(Component.literal("Your webhook Chat ID is ").append(this.formatText(manager.getTelegramChatId())), ChatUtils.DEFAULT_SIGNATURE);
         } else {
            reply("Chat ID is not supported for current webhook.", -1);
         }
         return 1;
      }));
      builder.then(literal("clear").executes(context -> {
         ClientContext.webhookManager.setUrl("");
         reply("Your notification destination has been cleared", -1035363);
         return 1;
      }));
   }

   private static int showStatus() {
      WebhookManager manager = ClientContext.webhookManager;
      WebhookNotifsModule notifications = ClientContext.getModule(WebhookNotifsModule.class);
      String moduleState = notifications == null
         ? "unavailable"
         : notifications.isToggled() ? "enabled" : "disabled";
      String queueState = notifications == null
         ? "no queue"
         : notifications.getPendingMessageCount() + " queued";
      String deliveryState = notifications == null
         ? "unavailable"
         : notifications.isDeliveryPaused()
            ? "paused after repeated failures"
            : notifications.isBatchInFlight() ? "sending" : "idle";
      int failures = notifications == null ? 0 : notifications.getConsecutiveDeliveryFailures();

      reply(
         "Notification destination: " + manager.getDestinationDescription()
            + "\nDestination configured: " + (manager.isConfigured() ? "yes" : "no")
            + "\nWebhook notifications module: " + moduleState
            + "\nQueue: " + queueState
            + "\nDelivery: " + deliveryState
            + "\nConsecutive failures: " + failures,
         -1035361
      );
      return 1;
   }

   private static int showHistory() {
      WebhookNotifsModule notifications = ClientContext.getModule(WebhookNotifsModule.class);
      if (notifications == null) {
         reply("Webhook notifications module is unavailable.", -1);
         return 0;
      }

      List<NotificationDeliveryHistory.Entry> history = notifications.getDeliveryHistory();
      if (history.isEmpty()) {
         reply("No notification delivery history yet.", -1);
         return 1;
      }

      DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
      StringBuilder output = new StringBuilder("Recent notification deliveries:");
      int start = Math.max(0, history.size() - 10);
      for (int index = start; index < history.size(); index++) {
         NotificationDeliveryHistory.Entry entry = history.get(index);
         String time = timeFormat.format(
            Instant.ofEpochMilli(entry.timestampMillis()).atZone(ZoneId.systemDefault())
         );
         output.append("\n")
            .append(time).append("  ")
            .append(entry.status()).append("  ")
            .append(entry.messageCount()).append(" message(s)");
         if (!entry.detail().isBlank()) {
            output.append("  ").append(entry.detail());
         }
      }
      reply(output.toString(), -1);
      return 1;
   }

   private static int retryDelivery() {
      WebhookNotifsModule notifications = ClientContext.getModule(WebhookNotifsModule.class);
      if (notifications == null) {
         reply("Webhook notifications module is unavailable.", -1);
         return 0;
      }
      if (!ClientContext.webhookManager.isConfigured()) {
         reply("Configure a webhook or Telegram destination first.", -1);
         return 0;
      }
      if (!notifications.isToggled()) {
         notifications.enable();
      }
      boolean pending = notifications.retryDelivery();
      reply(
         pending ? "Webhook delivery resumed. Queued messages will retry."
            : "Webhook delivery resumed. The queue is currently empty.",
         -1035362
      );
      return 1;
   }

   private static int clearHistory() {
      WebhookNotifsModule notifications = ClientContext.getModule(WebhookNotifsModule.class);
      if (notifications == null) {
         reply("Webhook notifications module is unavailable.", -1);
         return 0;
      }
      notifications.clearDeliveryHistory();
      reply("Notification delivery history cleared. Queued messages were not changed.", -1035363);
      return 1;
   }

   private static void reply(String message, int signature) {
      ChatUtils.printChat(Component.literal(message), signature);
   }
}
