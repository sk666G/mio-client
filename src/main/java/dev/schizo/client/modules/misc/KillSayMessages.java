/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.ClientContext;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.settings.Setting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

public final class KillSayMessages {
   private final KillEffectsModule module;
   private static final int MAX_MESSAGE_FILE_BYTES = 262_144;
   private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
   private int lineIndex = 0;
   private long generation;

   private String nextMessage() {
      try {
         if (this.lines.isEmpty()) {
            return null;
         } else {
            Setting<Boolean> randomOption = this.module.randomSetting;
            Boolean random = randomOption.getValue();
            if (random) {
               List<String> messageLines = this.lines;
               ThreadLocalRandom randomSnapshot = ThreadLocalRandom.current();
               int lineCount = this.lines.size();
               return messageLines.get(randomSnapshot.nextInt(lineCount));
            } else {
               return this.lines.get(
                  this.lineIndex++
                     % this.lines.size()
               );
            }
         }
      } catch (Exception selectionFailure) {
         return null;
      }
   }

   public KillSayMessages(KillEffectsModule module) {
      this.module = module;
   }

   void resetSequence() {
      this.lineIndex = 0;
   }

   void sendKillMessage(Player player, String playerName) {
      Setting<Boolean> killSayOption = this.module.killSaySetting;
      Boolean killSay = killSayOption.getValue();
      if (killSay) {
         Setting<Boolean> onlyEnemiesOption = this.module.onlyEnemiesSetting;
         Boolean onlyEnemies = onlyEnemiesOption.getValue();
         if (onlyEnemies) {
            SocialRegistry enemyManagerSnapshot = ClientContext.socialRegistry;

            Player enemyPlayer = player;
            if (!enemyManagerSnapshot.isEnemyPlayer(enemyPlayer)) {
               return;
            }
         }

         String message = this.nextMessage();
         if (message != null) {

            message = message.replace("{name}", playerName);

            String messageToSend = message;
            long expectedGeneration = this.generation;
            Runnable sendMessage = () -> {
               if (this.generation == expectedGeneration
                  && this.module.isToggled()
                  && this.module.killSaySetting.getValue()) {
                  ChatUtils.sendChatOrCommand(messageToSend);
               }
            };
            Setting<Float> delayOptionSnapshot = this.module.delaySetting;

            if (delayOptionSnapshot.isAtMinimum()) {
               sendMessage.run();
            } else {
               TaskScheduler scheduler = ClientContext.taskScheduler;
               Setting<Float> delayReadOption = this.module.delaySetting;
               float delaySeconds = delayReadOption.getValue();

               int delayTicks = (int)(delaySeconds / 0.05F);
               scheduler.scheduleRotationTask(sendMessage, delayTicks);
            }
         }
      }
   }

   void reloadMessages(boolean force) {
      boolean killSay = this.module.killSaySetting.getValue();
      if (!killSay && !force) {
         this.invalidate();
         return;
      }

      String selectedPath = this.module.pathSetting.getValue();
      long expectedGeneration = ++this.generation;
      try {
         ClientAccess.executorService.submit(() -> {
            List<String> loadedLines = List.of();
            Exception failure = null;
            try {
               Path root = ConfigurationManager.KILLSAY_DIRECTORY.toAbsolutePath().normalize();
               Path requested = root.resolve(selectedPath).normalize();
               if (!requested.startsWith(root)) {
                  throw new IOException("KillSay path escapes its storage directory");
               }

               Path path = requested;
               if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                  && !selectedPath.endsWith(".txt")) {
                  path = Path.of(requested.toString() + ".txt").normalize();
               }
               if (!path.startsWith(root)) {
                  throw new IOException("KillSay path escapes its storage directory");
               }
               if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                  Path parent = path.getParent();
                  if (parent == null
                     || Files.isSymbolicLink(parent)
                     || !Files.isDirectory(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                     throw new IOException("KillSay parent directory is unsafe");
                  }
                  Files.createFile(path);
                  throw new IOException("KillSay file " + selectedPath + " not found; created an empty file");
               }

               String text = ConfigFileReader.readUtf8(path, MAX_MESSAGE_FILE_BYTES);
               loadedLines = text.lines().toList();
            } catch (Exception loadFailure) {
               failure = loadFailure;
            }

            List<String> completedLines = loadedLines;
            Exception completedFailure = failure;
            ClientAccess.schizoMinecraft.executeIfPossible(() -> {
               if (this.generation != expectedGeneration) {
                  return;
               }

               this.lines.clear();
               this.lineIndex = 0;
               if (completedFailure == null) {
                  this.lines.addAll(completedLines);
                  return;
               }

               if (this.module.isToggled() && this.module.killSaySetting.getValue()) {
                  MutableComponent notification = Component.literal(
                     "Failed to update KillSay lines: " + completedFailure
                  ).withStyle(style -> style.withColor(ChatFormatting.RED));
                  ChatUtils.printChat(
                     notification,
                     ChatUtils.createSignature(-9634482),
                     ChatUtils.MessagePriority.LOW
                  );
               }
            });
         });
      } catch (java.util.concurrent.RejectedExecutionException rejected) {
         if (this.module.isToggled() && this.module.killSaySetting.getValue()) {
            MutableComponent notification = Component.literal(
               "Failed to schedule KillSay file load"
            ).withStyle(style -> style.withColor(ChatFormatting.RED));
            ChatUtils.printChat(
               notification,
               ChatUtils.createSignature(-9634482),
               ChatUtils.MessagePriority.LOW
            );
         }
      }
   }

   void invalidate() {
      this.generation++;
      this.lineIndex = 0;
   }

}
