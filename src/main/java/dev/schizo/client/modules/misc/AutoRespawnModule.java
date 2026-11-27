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
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

public class AutoRespawnModule extends Module {
   private final ElapsedTimer respawnTimer = new ElapsedTimer();
   private final Setting<Boolean> saySetting;
   private final Setting<String> messageSetting;
   private long lifecycleGeneration;

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      if (ClientAccess.schizoMinecraft.screen instanceof DeathScreen) {
         ElapsedTimer timer = this.respawnTimer;
         if (timer.hasElapsed(100L)) {
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            player.respawn();
            this.respawnTimer.reset();
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundRespawnPacket) {
         long expectedGeneration = this.lifecycleGeneration;
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (!ownsQueuedPacketAction(
               this.isToggled(),
               expectedGeneration,
               this.lifecycleGeneration
            ) || !this.saySetting.getValue()) {
               return;
            }

            String message = this.messageSetting.getValue();
            TaskScheduler scheduler = ClientContext.taskScheduler;
            scheduler.scheduleRotationTask(this, () -> {
               if (ownsQueuedPacketAction(
                     this.isToggled(),
                     expectedGeneration,
                     this.lifecycleGeneration
                  )
                  && this.saySetting.getValue()) {
                  ChatUtils.sendChatOrCommand(message);
               }
            }, 2);
         });
      }

      if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
         long expectedGeneration = this.lifecycleGeneration;
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (ownsQueuedPacketAction(
               this.isToggled(),
               expectedGeneration,
               this.lifecycleGeneration
            )) {
               this.respawnTimer.reset();
            }
         });
      }
   }

   @Override
   public void onEnable() {
      this.lifecycleGeneration++;
   }

   @Override
   public void onDisable() {
      this.lifecycleGeneration++;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.lifecycleGeneration++;
      this.respawnTimer.clear();
   }

   static boolean ownsQueuedPacketAction(
      boolean enabled,
      long expectedGeneration,
      long currentGeneration
   ) {
      return enabled && expectedGeneration == currentGeneration;
   }

   public AutoRespawnModule() {
      super("AutoRespawn", "Respawns automatically.", ModuleCategory.MISC, new String[0]);
      Boolean sayDefault = false;
      BooleanSetting say = new BooleanSetting("Say", sayDefault);
      say.markVisibilityDependency();
      this.saySetting = this.add(say);
      StringSetting message = new StringSetting("Message", "/kit cattyn");
      Setting<?>[] dependencies = new Setting<?>[1];
      dependencies[0] = this.saySetting;
      this.messageSetting = this.add(message.dependsOn(dependencies));

      this.setDrawn(false);
   }

}
