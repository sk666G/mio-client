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
import dev.schizo.client.events.ItemUseEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MultiPlayerGameModeDestroyBlockEvent;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.movement.MovementSpeedTracker;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.SocialRole;
import dev.schizo.client.utils.math.MathUtils;

import com.mojang.authlib.GameProfile;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class AnnouncerModule extends Module {
   private final Setting<Boolean> clientSideSetting;
   private final Setting<Boolean> compactSetting;
   private final Setting<Boolean> joinsSetting;
   private final Setting<Boolean> friendsSetting;
   private final Setting<Boolean> enemiesSetting;
   private final Setting<Boolean> othersSetting;
   private final Setting<Boolean> moveSetting;
   private final Setting<Boolean> breakSetting;
   private final Setting<Boolean> eatSetting;
   private final Setting<Double> delaySetting;
   private final ElapsedTimer announcementTimer;
   private double distanceTravelled;
   private int foodConsumed;
   private int blocksBroken;
   private final Map<UUID, String> profileNames = new ConcurrentHashMap<>();
   private long announcementGeneration;

   private String chooseMovementMessage() {
      String[] messages = {
         "I just flew over {blocks} blocks thanks to https://github.com/spectrumdevs/zio!",
         "Я только что пролетел над {blocks} блоками с помощью https://github.com/spectrumdevs/zio!",
         "https://github.com/spectrumdevs/zio sayesinde {blocks} blok uçtum!",
         "我刚刚用 https://github.com/spectrumdevs/zio 走了 {blocks} 米!",
         "Dank https://github.com/spectrumdevs/zio bin ich gerade über {blocks} Blöcke geflogen!",
         "Jag hoppade precis över {blocks} blocks tack vare https://github.com/spectrumdevs/zio!",
         "Właśnie przeleciałem ponad {blocks} bloki dzięki https://github.com/spectrumdevs/zio!",
         "Es tikko nolidoju {blocks} blokus, paldies https://github.com/spectrumdevs/zio!",
         "Я щойно пролетів понад {blocks} блоками завдяки https://github.com/spectrumdevs/zio!",
         "I just fwew ovew {blocks} bwoccs thanks to https://github.com/spectrumdevs/zio! :3",
         "Ho appena camminato per {blocks} blocchi grazie a https://github.com/spectrumdevs/zio!",
         "עכשיו עפתי {blocks} הודות ל https://github.com/spectrumdevs/zio!",
         "Právě jsem proletěl {blocks} bloků díky https://github.com/spectrumdevs/zio!",
         "Lensin juuri yli {blocks} blokkia https://github.com/spectrumdevs/zio:n ansiosta!",
         "Ravnokar sem preletel {blocks} blokov v zahvali https://github.com/spectrumdevs/zio!",
         "أنا هلق طاير فوق {blocks} بلوكس بفضل https://github.com/spectrumdevs/zio!"
      };
      return messages[ThreadLocalRandom.current().nextInt(messages.length)];
   }

   @Override
   public void onEnable() {
      this.foodConsumed = 0;
      this.blocksBroken = 0;
      this.announcementTimer.reset();
      this.announcementGeneration++;
      this.profileNames.clear();

      ClientPacketListener connection = ClientAccess.schizoMinecraft.getConnection();
      if (connection != null) {
         for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
            GameProfile profile = playerInfo.getProfile();
            if (profile != null) {
               this.profileNames.put(profile.id(), profile.name());
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.invalidateAnnouncements();
   }

   @EventHandler
   private void onConnectScreenConnect(ConnectScreenConnectEvent event) {
      this.invalidateAnnouncements();
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.invalidateAnnouncements();
   }

   private void invalidateAnnouncements() {
      this.announcementGeneration++;
      this.profileNames.clear();
   }

   private void announce(String message) {
      MessageSignature signature = ChatUtils.moduleSignature(this);
      this.scheduleAnnouncement(message, signature);
   }

   private String chooseEatingMessage() {
      String[] messages = {
         "I just ate {amount} {name} thanks to https://github.com/spectrumdevs/zio!",
         "Я только что съел {amount} {name} с помощью https://github.com/spectrumdevs/zio!",
         "Tam olarak {amount} tane {name} yedim. TeŞekkürler https://github.com/spectrumdevs/zio",
         "我刚用 https://github.com/spectrumdevs/zio 吃了 {amount} 个 {name}!",
         "Ich habe gerade {amount} {name} dank https://github.com/spectrumdevs/zio gegessen!",
         "Jag åt precis {amount} {name} tack vare https://github.com/spectrumdevs/zio",
         "Właśnie zjadłem {amount} {name} dzięki https://github.com/spectrumdevs/zio",
         "Es tikko apēdu {amount} {name} ar https://github.com/spectrumdevs/zio spēku!",
         "Я щойно з’їв {amount} {name} завдяки https://github.com/spectrumdevs/zio!",
         "I just ate {amount} {name} thanks to https://github.com/spectrumdevs/zio! ^-^",
         "Ho appena mangiato {amount} {name} grazie a https://github.com/spectrumdevs/zio!",
         "כרגע אכלתי {amount} {name} הודות לhttps://github.com/spectrumdevs/zio!",
         "Právě jsem snědl {amount} {name} díky https://github.com/spectrumdevs/zio",
         "Söin juuri {amount} {name}a https://github.com/spectrumdevs/zio:n ansiosta!",
         "Ravnokar sem pojedel {amount} {name} v zahvali https://github.com/spectrumdevs/zio",
         "أنا هلق أكلت {amount} {name} بفضل https://github.com/spectrumdevs/zio!"
      };
      return messages[ThreadLocalRandom.current().nextInt(messages.length)];
   }

   private String chooseBreakingMessage() {
      String[] messages = {
         "I just destroyed {amount} {name} with the power of https://github.com/spectrumdevs/zio!",
         "Я только что разрушил {amount} {name} с помощью https://github.com/spectrumdevs/zio!",
         "Az önce {amount} tane {name} kırdım. TeŞekkürler https://github.com/spectrumdevs/zio!",
         "我刚刚用 https://github.com/spectrumdevs/zio 破坏了 {amount} {name}!",
         "Ich habe gerade {amount} {name} mit der Kraft von https://github.com/spectrumdevs/zio zerstört!",
         "Jag förstörde precis {amount} {name} tack vare https://github.com/spectrumdevs/zio!",
         "Właśnie zniszczyłem {amount} {name} za pomocą https://github.com/spectrumdevs/zio",
         "Es tikko salauzu {amount} {name} ar spēku https://github.com/spectrumdevs/zio!",
         "Я щойно знищив {amount} {name} за допомогою https://github.com/spectrumdevs/zio!",
         "I just destwoyed {amount} {name} with the powew of https://github.com/spectrumdevs/zio! :3",
         "Ho appena distrutto {amount} {name} grazie al potere di https://github.com/spectrumdevs/zio!",
         "בדיוק חצבתי {amount} {name} בעזרת הכוח של https://github.com/spectrumdevs/zio!",
         "Právě jsem zničil {amount} {name} díky https://github.com/spectrumdevs/zio!",
         "Rikoin juuri {amount} {name}ia https://github.com/spectrumdevs/zio:n ansiosta!",
         "Ravnokar sem uničil {amount} {name} z močjo https://github.com/spectrumdevs/zio",
         "أنا هلق دمّرت {amount} {name} بقوة https://github.com/spectrumdevs/zio!"
      };
      return messages[ThreadLocalRandom.current().nextInt(messages.length)];
   }

   @EventHandler
   private void announceMovement(LocalPlayerTickEvent event) {
      if (!this.isWorldUnavailable()) {
         double previousDistance = this.distanceTravelled;
         MovementSpeedTracker movementTracker = ClientContext.movementSpeedTracker;
         double speed = movementTracker.getCurrentSpeed();

         double distancePerSecond = speed / 3.6D;

         this.distanceTravelled = previousDistance
            + distancePerSecond / 20.0D;
         double distanceSnapshot = this.distanceTravelled;

         if (distanceSnapshot >= 1000.0D) {
            this.distanceTravelled = 0.0;
         }

         Setting<Boolean> moveSettingSnapshot = this.moveSetting;
         Boolean moveValue = moveSettingSnapshot.getValue();
         if (moveValue) {
            distanceSnapshot = this.distanceTravelled;

            if (distanceSnapshot >= 1.0D) {
               ElapsedTimer delayTimer = this.announcementTimer;

               double millisPerSecond = 1000.0D;
               Setting<Double> delaySettingSnapshot = this.delaySetting;
               Double delayValue = delaySettingSnapshot.getValue();
               long delayMillis = (long)(millisPerSecond * delayValue);
               if (delayTimer.hasElapsed(delayMillis)) {
                  String template = this.chooseMovementMessage();

                  DecimalFormat distanceFormat = new DecimalFormat("0.00");
                  double announcedDistance = this.distanceTravelled;
                  String message = template.replace("{blocks}", distanceFormat.format(announcedDistance));

                  this.announce(message);
                  this.distanceTravelled = 0.0;
                  this.announcementTimer.reset();
               }
            }
         }
      }
   }

   private void scheduleAnnouncement(String message, MessageSignature signature) {
      long expectedGeneration = this.announcementGeneration;
      ClientAccess.schizoMinecraft.executeIfPossible(() -> {
         if (!this.isToggled() || this.announcementGeneration != expectedGeneration) {
            return;
         }

         if (this.clientSideSetting.getValue()) {
            ChatUtils.printChat(Component.literal(message).withStyle(style -> {
               Color color = new Color(161, 161, 161);
               return style.withColor(color.hashCode());
            }), signature);
         } else if (ClientAccess.schizoMinecraft.player != null) {
            ClientAccess.schizoMinecraft.player.connection.sendChat(message);
         }
      });
   }

   @EventHandler
   private void announceEating(
      ItemUseEvent.BeforeFinish event
   ) {
      if (!this.isWorldUnavailable()) {
         int announceCount = MathUtils.randomInt(1, (6));
         Setting<Boolean> eatSettingSnapshot = this.eatSetting;
         Boolean eatValue = eatSettingSnapshot.getValue();
         if (eatValue) {
            if (event.getStack().has(DataComponents.FOOD)) {
               this.foodConsumed++;
               if (this.foodConsumed >= announceCount) {
                  ElapsedTimer timer = this.announcementTimer;

                  double millisPerSecond = 1000.0D;
                  Setting<Double> delaySettingSnapshot = this.delaySetting;
                  Double delayValue = delaySettingSnapshot.getValue();
                  long delayMillis = (long)(millisPerSecond * delayValue);
                  if (timer.hasElapsed(delayMillis)) {
                     String template = this.chooseEatingMessage();

                     int announcedCount = this.foodConsumed;
                     String countedMessage = template.replace("{amount}", String.valueOf(announcedCount));
                     ItemStack finishedStack = event.getStack();
                     Item food = finishedStack.getItem();
                     String message = countedMessage.replace("{name}", food.getName().getString());

                     this.announce(message);
                     this.foodConsumed = 0;
                     timer = this.announcementTimer;

                     timer.reset();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void announceBlockBreak(MultiPlayerGameModeDestroyBlockEvent event) {
      if (!this.isWorldUnavailable()) {
         int announceCount = MathUtils.randomInt(1, (6));
         this.blocksBroken++;
         Setting<Boolean> breakSettingSnapshot = this.breakSetting;
         Boolean breakValue = breakSettingSnapshot.getValue();
         if (breakValue && this.blocksBroken >= announceCount) {
            ElapsedTimer timer = this.announcementTimer;

            double millisPerSecond = 1000.0D;
            Setting<Double> delaySettingSnapshot = this.delaySetting;
            Double delayValue = delaySettingSnapshot.getValue();
            long delayMillis = (long)(millisPerSecond * delayValue);
            if (timer.hasElapsed(delayMillis)) {
               String[] blockNames = new String[4];
               BlockPos position = event.getPos();

               Block block = BlockInteractionUtils.getBlock(
                  position
               );
               blockNames[0] = block.getName().getString();

               blockNames[1] = "Air";

               blockNames[(2)] = "Bedrock";

               blockNames[3] = "Barrier";

               String template = this.chooseBreakingMessage();

               int announcedCount = this.blocksBroken;
               String countedMessage = template.replace(
                  "{amount}",
                  String.valueOf(announcedCount)
               );

               Random random = new Random();
               int blockNameCount = blockNames.length;
               String message = countedMessage.replace("{name}", blockNames[random.nextInt(blockNameCount)]);

               this.announce(message);
               this.blocksBroken = 0;
               timer = this.announcementTimer;

               timer.reset();
            }
         }
      }
   }

   @EventHandler
   private void resetDelayAfterChat(
      PacketEvent.Send event
   ) {
      if (event.getPacket() instanceof ServerboundChatPacket) {
         this.announcementTimer.reset();
      }
   }

   private boolean shouldAnnounceRelationship(SocialRole relationship) {
      Setting<Boolean> friendsSettingSnapshot = this.friendsSetting;
      Boolean friendsValue = friendsSettingSnapshot.getValue();
      if (!friendsValue && relationship == SocialRole.FRIEND) {
         return false;
      }

      Setting<Boolean> enemiesSettingSnapshot = this.enemiesSetting;
      Boolean enemiesValue = enemiesSettingSnapshot.getValue();
      if (!enemiesValue && relationship == SocialRole.ENEMY) {
         return false;
      }

      Setting<Boolean> othersSettingSnapshot = this.othersSetting;
      Boolean othersValue = othersSettingSnapshot.getValue();
      return othersValue || relationship != null;
   }

   @EventHandler
   private void announcePlayerListChanges(
      PacketEvent.Receive event
   ) {
      List<String> joinedNames = new ArrayList<>();
      List<String> departingNames = new ArrayList<>();

      if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket joinPacket) {
         for (Entry entry : joinPacket.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile == null) {
               continue;
            }
            this.profileNames.put(profile.id(), profile.name());
            if (entry.listed()) {
               joinedNames.add(profile.name());
            }
         }
      }

      if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket leavePacket) {
         for (UUID departingId : leavePacket.profileIds()) {
            String departingName = this.profileNames.remove(departingId);
            if (departingName != null) {
               departingNames.add(departingName);
            }
         }
      }

      if (joinedNames.isEmpty() && departingNames.isEmpty()) {
         return;
      }

      long expectedGeneration = this.announcementGeneration;
      List<String> joinedSnapshot = List.copyOf(joinedNames);
      List<String> departingSnapshot = List.copyOf(departingNames);
      ClientAccess.schizoMinecraft.executeIfPossible(() -> {
         if (!this.isToggled()
            || this.announcementGeneration != expectedGeneration
            || !this.joinsSetting.getValue()
            || !this.announcementTimer.hasElapsed(this.delaySetting.getValue(), TimeUnit.SECONDS)) {
            return;
         }

         String localName = ClientAccess.schizoMinecraft.player == null
            ? null
            : ClientAccess.schizoMinecraft.player.getName().getString();

         for (String joiningName : joinedSnapshot) {
            if (joiningName.equals(localName)) {
               continue;
            }
            SocialRole relationship = ClientContext.socialRegistry.getRole(joiningName);
            if (!this.shouldAnnounceRelationship(relationship)) {
               continue;
            }
            this.schedulePlayerListAnnouncement(joiningName, relationship, true);
            this.announcementTimer.reset();
         }

         for (String departingName : departingSnapshot) {
            SocialRole relationship = ClientContext.socialRegistry.getRole(departingName);
            if (!this.shouldAnnounceRelationship(relationship)) {
               continue;
            }
            this.schedulePlayerListAnnouncement(departingName, relationship, false);
            this.announcementTimer.reset();
         }
      });
   }

   private void schedulePlayerListAnnouncement(String playerName, SocialRole relationship, boolean joined) {
      int signatureId = joined ? -254721 : -254722;
      if (this.clientSideSetting.getValue() && this.compactSetting.getValue()) {
         String compactMessage = playerName + (joined ? " joined" : " left");
         if (relationship == SocialRole.FRIEND) {
            compactMessage += " [friend]";
         } else if (relationship == SocialRole.ENEMY) {
            compactMessage += " [enemy]";
         }
         this.scheduleAnnouncement(compactMessage, ChatUtils.createSignature(signatureId));
         return;
      }

      String displayName = relationship == SocialRole.FRIEND ? "My friend " + playerName : playerName;
      String message = displayName + (joined ? " has joined the game!" : " has left the game!");
      this.scheduleAnnouncement(message, ChatUtils.createSignature(signatureId));
   }

   public AnnouncerModule() {
      super("Announcer", "Lets the others know what you're doing.", ModuleCategory.MISC, new String[0]);
      BooleanSetting clientSideOption = new BooleanSetting("ClientSide", false);
      clientSideOption.markVisibilityDependency();
      this.clientSideSetting = this.add(clientSideOption);
      this.compactSetting = this.add(new BooleanSetting("Compact", false).dependsOn(this.clientSideSetting));
      BooleanSetting joinsOption = new BooleanSetting("Joins", false);
      joinsOption.markVisibilityDependency();
      this.joinsSetting = this.add(joinsOption);
      this.friendsSetting = this.add(new BooleanSetting("Friends", true).dependsOn(this.joinsSetting));
      this.enemiesSetting = this.add(new BooleanSetting("Enemies", true).dependsOn(this.joinsSetting));
      this.othersSetting = this.add(new BooleanSetting("Others", true).dependsOn(this.joinsSetting));
      this.moveSetting = this.add(new BooleanSetting("Move", true));
      this.breakSetting = this.add(new BooleanSetting("Break", true));
      this.eatSetting = this.add(new BooleanSetting("Eat", true));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 10.0, 0.0, 30.0).withUnit("s"));
      this.announcementTimer = new ElapsedTimer();
      Setting<Double> delaySettingSnapshot = this.delaySetting;

      SettingLabelCondition minimumChoice = SettingLabelCondition.MIN;
      delaySettingSnapshot.setBoundaryLabel("None", minimumChoice);

      this.setDrawn(false);
   }

}
