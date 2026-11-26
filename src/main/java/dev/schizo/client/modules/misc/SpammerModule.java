/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

public class SpammerModule extends Module {
   private final Setting<String> pathSetting;
   private final Setting<Float> delaySetting;
   private final Setting<Boolean> refreshSetting;
   private final Setting<Boolean> smartDelaySetting;
   private final Setting<Boolean> randomSetting;
   private final Setting<Boolean> autoDisableSetting;
   private final Random messageRandom;
   private final List<String> messages;
   private static final int MAX_MESSAGE_FILE_BYTES = 262_144;
   private final ElapsedTimer sendTimer;
   private int nextMessageIndex;
   private long messageLoadGeneration;

   private void loadMessages() {
      if (!this.isToggled()) {
         return;
      }

      String selectedPath = this.pathSetting.getValue();
      long generation = ++this.messageLoadGeneration;
      try {
         ClientAccess.executorService.submit(() -> {
            List<String> loadedMessages;
            Exception failure = null;
            try {
               Path root = ConfigurationManager.SPAMMER_DIRECTORY.toAbsolutePath().normalize();
               Path requested = root.resolve(selectedPath).normalize();
               if (!requested.startsWith(root)) {
                  throw new IOException("Spammer path escapes its storage directory");
               }

               Path filePath = requested;
               if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)
                  && !selectedPath.endsWith(".txt")) {
                  filePath = Path.of(requested.toString() + ".txt").normalize();
               }
               if (!filePath.startsWith(root)
                  || Files.isSymbolicLink(filePath)
                  || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
                  throw new IOException("Spammer path is not a regular file");
               }

               String text = ConfigFileReader.readUtf8(filePath, MAX_MESSAGE_FILE_BYTES);
               loadedMessages = text.lines().toList();
            } catch (Exception loadFailure) {
               loadedMessages = List.of();
               failure = loadFailure;
            }

            List<String> completedMessages = loadedMessages;
            Exception completedFailure = failure;
            ClientAccess.schizoMinecraft.executeIfPossible(() -> {
               if (!this.isToggled() || this.messageLoadGeneration != generation) {
                  return;
               }

               this.nextMessageIndex = 0;
               this.messages.clear();
               if (completedFailure == null) {
                  this.messages.addAll(completedMessages);
               } else if (!this.isWorldUnavailable()) {
                  String errorMessage = "Failed to open spammer file " + selectedPath;
                  MutableComponent errorComponent = Component.literal(errorMessage)
                     .withStyle(style -> style.withColor(ChatFormatting.RED));
                  MessageSignature errorSignature = ChatUtils.createSignature(completedFailure.toString().hashCode());
                  ChatUtils.printChat(errorComponent, errorSignature, ChatUtils.MessagePriority.LOW);
               }
            });
         });
      } catch (java.util.concurrent.RejectedExecutionException rejected) {
         if (!this.isWorldUnavailable()) {
            MutableComponent errorComponent = Component.literal("Failed to schedule spammer file load")
               .withStyle(style -> style.withColor(ChatFormatting.RED));
            ChatUtils.printChat(
               errorComponent,
               ChatUtils.createSignature(rejected.toString().hashCode()),
               ChatUtils.MessagePriority.LOW
            );
         }
      }
   }

   @Override
   public void onEnable() {
      this.loadMessages();
   }

   @Override
   public void onDisable() {
      this.messageLoadGeneration++;
      this.messages.clear();
      this.nextMessageIndex = 0;
   }

   @EventHandler
   private void onConnectionSendPacketEvent(
      PacketEvent.Send event
   ) {
      if (event.getPacket() instanceof ServerboundChatPacket) {
         Setting<Boolean> smartDelaySnapshot = this.smartDelaySetting;
         Boolean smartDelayEnabled = smartDelaySnapshot.getValue();
         if (smartDelayEnabled) {
            this.sendTimer.reset();
         }
      }
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      ElapsedTimer timerCapture = this.sendTimer;
      Setting<Float> delaySnapshot = this.delaySetting;
      Float delayValue = delaySnapshot.getValue();
      double delaySeconds = delayValue.floatValue();
      TimeUnit delayUnit = TimeUnit.SECONDS;
      if (timerCapture.hasElapsed(delaySeconds, delayUnit)
         && !this.messages.isEmpty()) {
         List<String> messagesSnapshot = this.messages;
         Setting<Boolean> randomSnapshot = this.randomSetting;
         Boolean randomEnabled = randomSnapshot.getValue();
         int selectedIndex;
         if (randomEnabled && this.messages.size() > 1) {
            Random random = this.messageRandom;
            selectedIndex = random.nextInt(this.messages.size());
         } else {
            selectedIndex = this.nextMessageIndex;
         }

         String message = messagesSnapshot.get(selectedIndex);
         ChatUtils.sendChatOrCommand(message);

         this.nextMessageIndex = (
               this.nextMessageIndex + 1
            )
            % this.messages.size();
         timerCapture = this.sendTimer;

         timerCapture.reset();
         Setting<Boolean> autoDisableSnapshot = this.autoDisableSetting;
         Boolean autoDisableEnabled = autoDisableSnapshot.getValue();
         if (autoDisableEnabled) {
            this.toggle();
         }
      }
   }

   public SpammerModule() {
      super("Spammer", "Spams messages from a selected text file.", ModuleCategory.MISC, new String[0]);
      this.pathSetting = this.add(new StringSetting("Path", "spammer.txt"));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 5.0F, 0.0F, 300.0F).withUnit("s"));
      this.refreshSetting = this.add(new BooleanSetting("Refresh", false));
      this.smartDelaySetting = this.add(new BooleanSetting("SmartDelay", true));
      this.randomSetting = this.add(new BooleanSetting("Random", false));
      this.autoDisableSetting = this.add(new BooleanSetting("AutoDisable", false));
      this.messageRandom = new Random();
      this.messages = new ArrayList<>();
      this.sendTimer = new ElapsedTimer();
      this.nextMessageIndex = 0;
      this.pathSetting.setChangeCallback(() -> {
         if (this.isToggled()) {
            this.loadMessages();
         }
      });
      this.refreshSetting.setChangeCallback(() -> {
         Boolean refreshRequested = this.refreshSetting.getValue();
         if (refreshRequested) {
            this.refreshSetting.setValue(false);
            if (this.isToggled()) {
               this.loadMessages();
            }
         }
      });
   }

}
