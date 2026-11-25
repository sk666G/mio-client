/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PlayerTotemEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.managers.AliasRegistry;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.utils.LifecycleGeneration;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.ChatVisiblity;

public class NotificationsModule extends Module {
   private static final int MAX_TOTEM_MESSAGE_FILE_BYTES = 262_144;
   private static final int MAX_PENDING_TOTEM_CHATS = 64;
   public final Setting<Color> watermarkSetting;
   public final Setting<Color> bracketsSetting;
   private final Setting<Boolean> commandsSetting;
   private final Setting<Boolean> modulesSetting;
   private final Setting<Boolean> onlyDrawnSetting;
   final Setting<Boolean> totemPopsSetting;
   private final Setting<Boolean> selfSetting;
   private final Setting<Color> primarySetting;
   private final Setting<Color> secondarySetting;
   final Setting<Boolean> sayChatSetting;
   private final Setting<String> pathSetting;
   private final Setting<Float> delaySetting;
   private final Setting<Boolean> onlyEnemiesSetting;
   private final Setting<Boolean> randomSetting;
   private final Setting<Boolean> refreshSetting;
   private int totemMessageIndex;
   private final List<String> totemMessages;
   private final Random totemRandom;
   private final AtomicInteger pendingTotemChats = new AtomicInteger();
   private final LifecycleGeneration totemWorkLifecycle = new LifecycleGeneration();

   @Override
   public void onEnable() {
      this.reloadTotemMessages();
   }

   @Override
   public void onDisable() {
      this.invalidateTotemWork();
   }

   private void printTotemNotification(String playerName, String actionText, int popCount) {
      Options options = ClientAccess.schizoMinecraft.options;
      OptionInstance visibilityOption = options.chatVisibility();
      if (visibilityOption.get() != ChatVisiblity.HIDDEN) {
         MutableComponent messageBuilder = Component.empty();
         messageBuilder = messageBuilder.append(Component.literal(playerName).withStyle(lambdaStyle -> {
            Setting<Color> lambdaColorOption = this.primarySetting;
            Color lambdaColor = lambdaColorOption.getValue();
            int lambdaColorArgb = lambdaColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
         messageBuilder = messageBuilder.append(Component.literal(actionText).withStyle(lambdaStyle -> {
            Setting<Color> lambdaColorOption = this.secondarySetting;
            Color lambdaColor = lambdaColorOption.getValue();
            int lambdaColorArgb = lambdaColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
         String countText = String.valueOf(popCount);
         messageBuilder = messageBuilder.append(Component.literal(countText).withStyle(lambdaStyle -> {
            Setting<Color> lambdaColorOption = this.primarySetting;
            Color lambdaColor = lambdaColorOption.getValue();
            int lambdaColorArgb = lambdaColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));

         String suffixText = popCount == 1 ? " totem." : " totems.";
         MutableComponent message = messageBuilder.append(Component.literal(suffixText).withStyle(lambdaStyle -> {
            Setting<Color> lambdaColorOption = this.secondarySetting;
            Color lambdaColor = lambdaColorOption.getValue();
            int lambdaColorArgb = lambdaColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
         int nameHash = playerName.hashCode();
         int signatureId = Math.abs(nameHash) * -1;

         ChatUtils.printChat(message, ChatUtils.createSignature((signatureId)), ChatUtils.MessagePriority.MID);
      }
   }

   @EventHandler
   private void onModuleToggle(ModuleToggleEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      Setting<Boolean> modulesOption = this.modulesSetting;
      Boolean notifyModules = modulesOption.getValue();
      if (!notifyModules) {
         return;
      }
      if (event.getModule() instanceof HUDModule) {
         return;
      }
      if (event.getModule() instanceof UIModule) {
         return;
      }
      Setting<Boolean> drawnOption = this.onlyDrawnSetting;
      Boolean onlyDrawn = drawnOption.getValue();
      if (onlyDrawn && !event.getModule().isDrawn()) {
         return;
      }

      MutableComponent message = Component.empty();
      Module toggledModule = event.getModule();
      if (toggledModule.isToggled()) {
         String enabledOpening = "[";
         message.append(Component.literal(enabledOpening).withStyle(lambdaStyle -> {

            Color lambdaToggleColor = this.moduleToggleColor(true);
            Color lambdaDarkColor = lambdaToggleColor.darker();
            int lambdaColorArgb = lambdaDarkColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
         String enabledSign = "+";
         message.append(Component.literal(enabledSign).withStyle(lambdaStyle -> {

            Color lambdaToggleColor = this.moduleToggleColor(true);
            int lambdaColorArgb = lambdaToggleColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));

         String enabledClosing = "] ";
         message.append(Component.literal(enabledClosing).withStyle(lambdaStyle -> {

            Color lambdaToggleColor = this.moduleToggleColor(true);
            Color lambdaDarkColor = lambdaToggleColor.darker();
            int lambdaColorArgb = lambdaDarkColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
      } else {
         String disabledOpening = "[";
         message.append(Component.literal(disabledOpening).withStyle(lambdaStyle -> {

                  Color lambdaToggleColor = this.moduleToggleColor(false);
                  Color lambdaDarkColor = lambdaToggleColor.darker();
                  int lambdaColorArgb = lambdaDarkColor.hashCode();
                  return lambdaStyle.withColor(lambdaColorArgb);
               }));
         String disabledSign = "-";
         message.append(Component.literal(disabledSign).withStyle(lambdaStyle -> {

            Color lambdaToggleColor = this.moduleToggleColor(false);
            int lambdaColorArgb = lambdaToggleColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));

         String disabledClosing = "] ";
         message.append(Component.literal(disabledClosing).withStyle(lambdaStyle -> {

            Color lambdaToggleColor = this.moduleToggleColor(false);
            Color lambdaDarkColor = lambdaToggleColor.darker();
            int lambdaColorArgb = lambdaDarkColor.hashCode();
            return lambdaStyle.withColor(lambdaColorArgb);
         }));
      }

      AliasRegistry aliasRegistry = ClientContext.aliasRegistry;
      Module aliasModule = event.getModule();
      String moduleName = AliasRegistry.getModuleAlias(aliasRegistry, aliasModule);
      message.append(Component.literal(moduleName));
      LifecycleGeneration.Token activation = this.captureActivationToken();
      ClientContext.taskScheduler.scheduleRunTickTask(() -> {
         if (!this.isActivationCurrent(activation)) {
            return;
         }
         Module signatureModule = event.getModule();
         ChatUtils.printChat(message, ChatUtils.moduleSignature(signatureModule));
      });
   }

   @EventHandler
   private void onPlayerTotem(PlayerTotemEvent event) {
      LocalPlayer localPlayer = ClientAccess.schizoMinecraft.player;
      boolean self = localPlayer == event.getPlayer();
      if (!this.totemPopsSetting.getValue()) {
         return;
      }
      if (self && !this.selfSetting.getValue()) {
         return;
      }

      String playerName = self ? "You" : event.getName();
      int popCount = event.getPopCount();
      if (event.getKind() != PlayerTotemEvent.Kind.TOTEM_POP) {
         this.printTotemNotification(playerName, " has died after popping ", popCount);
         return;
      }

      this.printTotemNotification(playerName, self ? " have popped " : " has popped ", popCount);
      if (self || ClientContext.socialRegistry.isFriendName(playerName)) {
         return;
      }
      if (!ClientContext.socialRegistry.isEnemyName(playerName)
         && this.onlyEnemiesSetting.getValue()) {
         return;
      }
      if (!this.sayChatSetting.getValue() || this.totemMessages.isEmpty()) {
         return;
      }

      LifecycleGeneration.Token expectedGeneration = this.totemWorkLifecycle.capture();
      boolean randomOrder = this.randomSetting.getValue();
      long delayMillis = (long)(this.delaySetting.getValue() * 1000.0F);
      if (delayMillis > 0L) {
         delayMillis += this.totemRandom.nextLong(500L);
      }

      Runnable dispatch = () -> ClientAccess.schizoMinecraft.executeIfPossible(
         () -> this.sendTotemChat(playerName, popCount, randomOrder, expectedGeneration)
      );
      if (delayMillis <= 0L) {
         dispatch.run();
      } else {
         int pending = this.pendingTotemChats.incrementAndGet();
         if (pending > MAX_PENDING_TOTEM_CHATS) {
            this.pendingTotemChats.decrementAndGet();
            ClientAccess.logger.warn("Dropping delayed totem chat because the pending queue is full");
            return;
         }

         CompletableFuture.runAsync(
            dispatch,
            CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS)
         ).whenComplete((ignored, failure) -> this.pendingTotemChats.decrementAndGet());
      }
   }

   private void sendTotemChat(
      String playerName,
      int popCount,
      boolean randomOrder,
      LifecycleGeneration.Token expectedGeneration
   ) {
      if (!this.isToggled()
         || !expectedGeneration.isCurrent()
         || !this.sayChatSetting.getValue()
         || this.totemMessages.isEmpty()) {
         return;
      }

      int messageCount = this.totemMessages.size();
      int selectedIndex;
      if (randomOrder && messageCount > 1) {
         do {
            selectedIndex = this.totemRandom.nextInt(messageCount);
         } while (selectedIndex == this.totemMessageIndex);
      } else if (randomOrder) {
         selectedIndex = 0;
      } else {
         selectedIndex = Math.floorMod(this.totemMessageIndex, messageCount);
      }

      this.totemMessageIndex = randomOrder
         ? selectedIndex
         : (selectedIndex + 1) % messageCount;

      String chatMessage = this.totemMessages.get(selectedIndex)
         .replace("{name}", playerName)
         .replace("{totems}", Integer.toString(popCount));
      ChatUtils.sendChatOrCommand(chatMessage);
   }

   private Color moduleToggleColor(boolean enabled) {
      return enabled ? new Color(0, 190, 50) : new Color(200, 0, 0);
   }

   public static boolean shouldNotifyCommands(NotificationsModule notifications) {
      return notifications.commandsSetting.getValue();
   }

   private void reloadTotemMessages() {
      String selectedPath = this.pathSetting.getValue();
      this.totemWorkLifecycle.invalidate();
      LifecycleGeneration.Token expectedGeneration = this.totemWorkLifecycle.capture();
      try {
         ClientAccess.executorService.submit(() -> {
            List<String> loadedMessages = List.of();
            Exception failure = null;
            try {
               Path root = ConfigurationManager.ROOT_DIRECTORY.toAbsolutePath().normalize();
               Path requested = root.resolve(selectedPath).normalize();
               if (!requested.startsWith(root)) {
                  throw new IOException("Totem message path escapes the client data directory");
               }

               Path messagePath = requested;
               if (!Files.exists(messagePath, LinkOption.NOFOLLOW_LINKS)
                  && !selectedPath.endsWith(".txt")) {
                  messagePath = Path.of(requested.toString() + ".txt").normalize();
               }
               if (!messagePath.startsWith(root)
                  || Files.isSymbolicLink(messagePath)
                  || !Files.isRegularFile(messagePath, LinkOption.NOFOLLOW_LINKS)) {
                  throw new IOException("Totem message path is not a regular file");
               }

               String text = ConfigFileReader.readUtf8(
                  messagePath, MAX_TOTEM_MESSAGE_FILE_BYTES
               );
               loadedMessages = text.lines().toList();
            } catch (Exception loadFailure) {
               failure = loadFailure;
            }

            List<String> completedMessages = loadedMessages;
            Exception completedFailure = failure;
            ClientAccess.schizoMinecraft.executeIfPossible(() -> {
               if (!this.isToggled() || !expectedGeneration.isCurrent()) {
                  return;
               }

               this.totemMessageIndex = 0;
               this.totemMessages.clear();
               if (completedFailure == null) {
                  this.totemMessages.addAll(completedMessages);
               } else if (this.sayChatSetting.getValue()) {
                  MutableComponent errorMessage = Component.literal("Failed to open totem pop file")
                     .withStyle(style -> style.withColor(ChatFormatting.RED));
                  ChatUtils.printChat(
                     errorMessage,
                     ChatUtils.createSignature(completedFailure.toString().hashCode()),
                     ChatUtils.MessagePriority.LOW
                  );
               }
            });
         });
      } catch (java.util.concurrent.RejectedExecutionException rejected) {
         if (this.sayChatSetting.getValue()) {
            MutableComponent errorMessage = Component.literal("Failed to schedule totem message reload")
               .withStyle(style -> style.withColor(ChatFormatting.RED));
            ChatUtils.printChat(
               errorMessage,
               ChatUtils.createSignature(rejected.toString().hashCode()),
               ChatUtils.MessagePriority.LOW
            );
         }
      }
   }

   private void invalidateTotemWork() {
      this.totemWorkLifecycle.invalidate();
      this.totemMessageIndex = 0;
   }

   public int getBracketColor() {
      Color bracketColor = this.bracketsSetting.getValue();
      return bracketColor.hashCode();
   }

   public int getWatermarkColor() {
      Color watermarkColor = this.watermarkSetting.getValue();
      return watermarkColor.hashCode();
   }

   public NotificationsModule() {
      super("Notifications", "Lets you know about what's going on in the chat.", ModuleCategory.CLIENT, new String[0]);
      this.watermarkSetting = this.add(new ColorSetting(("Watermark"), new Color(190, 255, 64)));
      this.bracketsSetting = this.add(new ColorSetting("Brackets", new Color(190, 255, 64)));
      this.commandsSetting = this.add(new BooleanSetting(("Commands"), true));
      BooleanSetting modulesOption = new BooleanSetting("Modules", true);
      modulesOption.markVisibilityDependency();
      this.modulesSetting = this.add(modulesOption);
      this.onlyDrawnSetting = this.add(new BooleanSetting("OnlyDrawn", false)
         .dependsOn(this.modulesSetting));
      BooleanSetting popsOption = new BooleanSetting("TotemPops", false);
      popsOption.markVisibilityDependency();
      this.totemPopsSetting = this.add(popsOption);
      this.selfSetting = this.add(new BooleanSetting("Self", false)
         .dependsOn(this.totemPopsSetting));
      Predicate primaryVisibility = ignored -> this.totemPopsSetting.isExpanded();
      this.primarySetting = this.add(new ColorSetting("Primary", new Color(-2828, true), primaryVisibility));
      Predicate secondaryVisibility = ignored -> this.totemPopsSetting.isExpanded();
      this.secondarySetting = this.add(new ColorSetting("Secondary", new Color(-4351489, true), secondaryVisibility));
      Predicate sayChatVisibility = ignored -> this.totemPopsSetting.isExpanded();
      BooleanSetting sayChatOption = new BooleanSetting("SayChat", false, sayChatVisibility);
      sayChatOption.markVisibilityDependency();
      this.sayChatSetting = this.add(sayChatOption);
      Predicate pathVisibility = ignored -> this.totemPopsSetting.isExpanded() && this.sayChatSetting.isExpanded();
      this.pathSetting = this.add(new StringSetting(("Path"), "totempop.txt", pathVisibility));
      Predicate delayVisibility = ignored -> this.totemPopsSetting.isExpanded() && this.sayChatSetting.isExpanded();
      NumberSetting<Float> delayOption = new NumberSetting<>("Delay", 0.0F, 0.0F, 15.0F, delayVisibility);
      delayOption.withUnit(("s"));
      this.delaySetting = this.add(delayOption);
      this.onlyEnemiesSetting = this.add(new BooleanSetting("OnlyEnemies", false)
         .dependsOn(this.totemPopsSetting, this.sayChatSetting));
      Predicate randomVisibility = ignored -> this.totemPopsSetting.isExpanded() && this.sayChatSetting.isExpanded();
      this.randomSetting = this.add(new BooleanSetting("Random", true, randomVisibility));
      Predicate refreshVisibility = ignored -> this.totemPopsSetting.isExpanded() && this.sayChatSetting.isExpanded();
      this.refreshSetting = this.add(new BooleanSetting("Refresh", false, refreshVisibility));
      this.totemMessageIndex = 0;
      this.totemMessages = new ArrayList<>();
      this.totemRandom = new Random();

      this.setDrawn(false);
      this.sayChatSetting.setChangeCallback(() -> {
         if (!this.isToggled()) {
            return;
         }
         if (this.sayChatSetting.getValue()) {
            this.reloadTotemMessages();
         } else {
            this.invalidateTotemWork();
         }
      });
      this.refreshSetting.setChangeCallback(() -> {
         if (this.refreshSetting.getValue()) {
            this.refreshSetting.setValue(false);
            if (this.isToggled()) {
               this.reloadTotemMessages();
            }
         }
      });
      this.randomSetting.setChangeCallback(() -> this.totemMessageIndex = 0);
      this.pathSetting.setChangeCallback(() -> {
         if (this.isToggled()) {
            this.reloadTotemMessages();
         }
      });
   }

}
