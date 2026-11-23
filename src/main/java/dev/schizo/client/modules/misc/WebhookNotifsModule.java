/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ChatMessageEvent;
import dev.schizo.client.events.MinecraftTickEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.managers.WebhookManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class WebhookNotifsModule extends Module {

   private static final int MAX_PENDING_MESSAGES = 1000;
   private static final long MAX_PENDING_CHARACTERS = 1_000_000L;
   private static final int MAX_BATCH_MESSAGES = 100;
   private static final Map<Color, String> ANSI_PALETTE = new HashMap<>();
   static {
      ANSI_PALETTE.put(new Color(0, 0, 0), "30");
      ANSI_PALETTE.put(new Color(255, 0, 0), "31");
      ANSI_PALETTE.put(new Color(0, 255, 0), "32");
      ANSI_PALETTE.put(new Color(255, 170, 0), "33");
      ANSI_PALETTE.put(new Color(0, 0, 255), "34");
      ANSI_PALETTE.put(new Color(255, 0, 255), "35");
      ANSI_PALETTE.put(new Color(0, 255, 255), "36");
      ANSI_PALETTE.put(new Color(170, 170, 170), "37");
   }

   private final Setting<Float> timeoutSetting;
   private final Setting<Boolean> colorSetting;
   private final Setting<Boolean> timestampSetting;
   private final Setting<Boolean> ignoreZioSetting;
   private final Setting<Boolean> queueSetting;
   private final Setting<Boolean> disconnectSetting;
   private final Setting<Boolean> dMSetting;
   private final Setting<Boolean> chatMessagesSetting;
   private final Setting<Boolean> visualRangeSetting;
   private final Setting<Boolean> stashSetting;
   private final Setting<Boolean> pingSetting;
   private final DateTimeFormatter timestampFormatter;
   private final List<String> pendingMessages;
   private long pendingCharacters;
   private boolean batchInFlight;
   private boolean deliveryPaused;
   private int consecutiveDeliveryFailures;
   private long pingGeneration;
   private boolean pingPending;
   private final ElapsedTimer notificationTimer;
   private String previousQueueMessage;
   private ServerData lastServer;
   @Override
   public void onEnable() {
      this.notificationTimer.reset();
      synchronized (this.pendingMessages) {
         this.deliveryPaused = false;
         this.consecutiveDeliveryFailures = 0;
      }
      if (ClientContext.webhookManager.isConfigured()) return;
      String prefix = CommandRegistry.getPrefix();
      ChatUtils.printChat(Component.literal("Set a notification destination with " + prefix + "webhook set <url> or "
         + prefix + "webhook telegram <bot-token> <chat-id>."), ChatUtils.createSignature(-10395), ChatUtils.MessagePriority.MID);
   }

   private String formatMessage(ChatMessageEvent event) {
      if (!this.colorSetting.getValue()) return event.getOriginalText();
      StringBuilder message = new StringBuilder();
      int[] previousColor = {-1};
      event.getOriginalMessage().getVisualOrderText().accept((index, style, codePoint) -> {
         int color = style.getColor() == null ? -1 : style.getColor().getValue();
         if (color != previousColor[0]) message.append("\u001b[").append(getClosestAnsiColor(color)).append("m");
         message.appendCodePoint(codePoint);
         previousColor[0] = color;
         return true;
      });
      return message.toString();
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      if (this.disconnectSetting.getValue() && this.lastServer != null) {
         this.enqueueLines(("You have been disconnected from " + this.lastServer.ip + "!").split("\n"));
         this.lastServer = null;
      }
   }

   private void enqueueLines(String[] lines) {
      StringBuilder message = new StringBuilder();
      for (String line : lines) {
         if (!line.isEmpty()) message.append(ChatFormatting.stripFormatting(line)).append('\n');
      }
      if (message.length() < 2) return;
      String queued = message.substring(0, message.length() - 1);
      synchronized (this.pendingMessages) {
         if (this.pendingMessages.size() >= MAX_PENDING_MESSAGES
            || this.pendingCharacters + queued.length() > MAX_PENDING_CHARACTERS) {
            ClientAccess.logger.warn("Webhook notification queue is full; newest message was dropped");
            return;
         }
         this.pendingMessages.add(queued);
         this.pendingCharacters += queued.length();
      }
   }

   public static String getClosestAnsiColor(int rgb) {
      String closestCode = "37";
      double closestDistance = Double.MAX_VALUE;
      for (Map.Entry<Color, String> entry : ANSI_PALETTE.entrySet()) {
         Color color = entry.getKey();
         double red = color.getRed() - ARGB.red(rgb);
         double green = color.getGreen() - ARGB.green(rgb);
         double blue = color.getBlue() - ARGB.blue(rgb);
         double distance = Math.pow(red, 2.0D) + Math.pow(green, 2.0D) + Math.pow(blue, 2.0D);
         if (distance < closestDistance) {
            closestDistance = distance;
            closestCode = entry.getValue();
         }
      }
      return closestCode;
   }

   @EventHandler
   private void onChatMessage(ChatMessageEvent event) {
      if (event.isCancelled()) {
         return;
      }
      if (!ClientContext.webhookManager.isConfigured() || event.getStage() != EventStage.Pre || event.getOriginalText() == null) return;
      if (this.ignoreZioSetting.getValue() && event.getTag() == ChatUtils.MESSAGE_TAG) return;

      String message = this.formatMessage(event);
      String[] lines = message.split(("\n"));

      boolean incomingDirectMessage = ChatUtils.isIncomingWhisper(message);

      boolean outgoingDirectMessage = ChatUtils.isOutgoingWhisper(message);
      boolean stashMessage = this.isStashMessage(event);
      boolean visualRangeMessage = this.isVisualRangeMessage(event);
      if (incomingDirectMessage || outgoingDirectMessage) {
         Setting<Boolean> directMessageOption = this.dMSetting;
         Boolean notifyDirectMessages = directMessageOption.getValue();
         if (notifyDirectMessages) {
            this.enqueueLines(lines);
         }
         return;
      }

      if (message.contains("Position in queue:")) {
         int lineCount = lines.length;

         if (lineCount > (15)) {
            if (this.previousQueueMessage == null) {
               this.previousQueueMessage = message;
            } else if (!this.previousQueueMessage
               .equals(message)) {
               Setting<Boolean> queueOption = this.queueSetting;
               Boolean notifyQueue = queueOption.getValue();
               if (notifyQueue) {
                  this.enqueueLines(lines);
                  this.previousQueueMessage = message;
               }

               return;
            }

            return;
         }
      }

      Setting<Boolean> chatMessagesOption = this.chatMessagesSetting;
      Boolean notifyChatMessages = chatMessagesOption.getValue();
      if (notifyChatMessages || stashMessage || visualRangeMessage) {
         if (stashMessage) {
            Setting<Boolean> pingOption = this.pingSetting;
            Boolean pingEveryone = pingOption.getValue();
            if (pingEveryone) {
               this.pingPending = true;
               this.pingGeneration++;
            }
         }

         this.enqueueLines(lines);
      }
   }

   private boolean isVisualRangeMessage(ChatMessageEvent event) {
      return this.visualRangeSetting.getValue() && event.getSignature() != null
         && event.getSignature().asByteBuffer().getInt() <= 0 && event.getMessage().getString().contains("visual range");
   }

   private boolean isStashMessage(ChatMessageEvent event) {
      return this.stashSetting.getValue() && event.getSignature() != null
         && event.getSignature().asByteBuffer().getInt() == -13579;
   }

   @EventHandler
   private void onClientUpdate(MinecraftTickEvent event) {
      if (ClientAccess.schizoMinecraft.level != null) {
         this.lastServer = ClientContext.serverStateManager.getServerData();
      }
      WebhookManager manager = ClientContext.webhookManager;
      if (!manager.isConfigured()
         || !this.notificationTimer.hasElapsed(this.timeoutSetting.getValue(), TimeUnit.SECONDS)) return;

      List<String> batchMessages;
      String payload;
      boolean sendPing;
      long selectedPingGeneration;
      synchronized (this.pendingMessages) {
         if (this.batchInFlight || this.deliveryPaused || this.pendingMessages.isEmpty()) return;
         batchMessages = new ArrayList<>();
         StringBuilder batch = new StringBuilder();
         String timestamp = "[" + this.timestampFormatter.format(LocalDateTime.now()) + "] ";
         for (String message : this.pendingMessages) {
            if (batchMessages.size() >= MAX_BATCH_MESSAGES) break;
            String item = (this.timestampSetting.getValue() ? timestamp : "") + message;
            if (!batchMessages.isEmpty() && batch.length() + item.length() + 1 > 1800) break;
            if (!batch.isEmpty()) batch.append('\n');
            batch.append(item);
            batchMessages.add(message);
         }
         if (batchMessages.isEmpty()) return;

         sendPing = this.pingPending && !manager.isTelegram();
         selectedPingGeneration = this.pingGeneration;
         String text = batch.toString().replace("`", "'");
         if (manager.isTelegram()) {
            payload = text;
         } else {
            String mention = sendPing ? "@everyone\n" : "";
            String language = this.colorSetting.getValue() ? "ansi\n" : "";
            payload = mention + "```" + language + text + "```";
         }
         this.batchInFlight = true;
         this.notificationTimer.reset();
      }

      // Partial chunk delivery can cause duplicates when the retained batch is retried.
      // Messages are removed only after every chunk has been acknowledged.
      manager.sendContent(payload).whenComplete((ignored, failure) -> {
         synchronized (this.pendingMessages) {
            if (failure == null) {
               boolean matchedPrefix = true;
               for (String expected : batchMessages) {
                  if (this.pendingMessages.isEmpty() || !this.pendingMessages.get(0).equals(expected)) {
                     matchedPrefix = false;
                     break;
                  }
                  this.pendingMessages.remove(0);
                  this.pendingCharacters -= expected.length();
               }
               if (matchedPrefix) {
                  this.consecutiveDeliveryFailures = 0;
                  if (sendPing && this.pingGeneration == selectedPingGeneration) this.pingPending = false;
               } else {
                  ClientAccess.logger.error("Webhook queue prefix changed during delivery; retaining queued messages");
               }
            } else {
               this.consecutiveDeliveryFailures++;
               ClientAccess.logger.warn("Webhook batch delivery attempt {} failed",
                  this.consecutiveDeliveryFailures);
               if (this.consecutiveDeliveryFailures >= 5) {
                  this.deliveryPaused = true;
                  ClientAccess.logger.error("Webhook delivery paused after repeated failures; toggle the module to retry");
               }
            }
            this.batchInFlight = false;
         }
      });
   }

   public WebhookNotifsModule() {
      super("WebhookNotifs", "Sends chat messages to your webhook.", ModuleCategory.MISC, "DiscordNotifs");
      this.timeoutSetting = this.add(new NumberSetting<>("Timeout", 1.0F, 1.0F, 10.0F).withUnit("s"));
      this.colorSetting = this.add(new BooleanSetting("Color", false));
      this.timestampSetting = this.add(new BooleanSetting("Timestamp", false));
      this.ignoreZioSetting = this.add(new BooleanSetting("IgnoreZio", true));
      BooleanSetting types = new BooleanSetting("Types", false);
      types.markAsGroup();
      types.markVisibilityDependency();
      Setting<Boolean> registeredTypes = this.add(types);
      this.queueSetting = this.add(new BooleanSetting("Queue", true).dependsOn(registeredTypes));
      this.disconnectSetting = this.add(new BooleanSetting("Disconnect", true).dependsOn(registeredTypes));
      this.dMSetting = this.add(new BooleanSetting("DM", true).dependsOn(registeredTypes));
      this.chatMessagesSetting = this.add(new BooleanSetting("ChatMessages", false).dependsOn(registeredTypes));
      this.visualRangeSetting = this.add(new BooleanSetting("VisualRange", false).dependsOn(registeredTypes));
      BooleanSetting stash = new BooleanSetting("Stash", false);
      stash.markVisibilityDependency();
      Setting<Boolean> registeredStash = this.add(stash.dependsOn(registeredTypes));
      this.stashSetting = registeredStash;
      this.pingSetting = this.add(new BooleanSetting("Ping", false).dependsOn(registeredTypes, registeredStash));
      this.timestampFormatter = DateTimeFormatter.ofPattern("HH:mm");
      this.pendingMessages = Collections.synchronizedList(new ArrayList<>());
      this.notificationTimer = new ElapsedTimer();
   }

}
