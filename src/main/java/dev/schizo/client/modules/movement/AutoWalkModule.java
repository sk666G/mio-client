/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.AbstractClientPlayerTickEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.utils.combat.ExtraBreakTarget;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.audio.SoundClip;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.utils.collections.NamedIdentifier;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.IdentifierSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class AutoWalkModule extends Module {
   private static SpeedMineModule speedMineModule;
   private final Setting<Boolean> autoJumpSetting;
   private final Setting<Boolean> stuckNotifySetting;
   private final Setting<NamedIdentifier> soundSetting;
   private final Setting<Float> volumeSetting;
   private final Setting<Boolean> pauseSetting;
   private final Setting<Boolean> miningSetting;
   private final Setting<Boolean> eatingSetting;
   private int stuckTicks;
   private final Setting<Boolean> pauseUnloadedSetting;
   private final Setting<Boolean> manualOverrideSetting;

   private boolean isMining() {
      if (!speedMineModule.isToggled()) {
         return ClientAccess.schizoMinecraft.gameMode.isDestroying();
      }
      BlockPos miningPosition = speedMineModule.getMiningPos();
      ExtraBreakTarget extraTarget = speedMineModule.getExtraBreakTarget();
      return WorldUtils.canMineBlock(miningPosition)
         || extraTarget != null && WorldUtils.canMineBlock(extraTarget.getPosition());
   }

   static {
      ClientAccess.eventBus.subscribeStatic(AutoWalkModule.class);
   }

   private boolean shouldPauseForAction() {
      this.clearPauseReason();
      if (this.isWorldUnavailable()) return this.pauseWithReason("Waiting for world");
      if (this.manualOverrideSetting.getValue()
         && (ClientAccess.schizoMinecraft.options.keyUp.isDown() || ClientAccess.schizoMinecraft.options.keyDown.isDown()
            || ClientAccess.schizoMinecraft.options.keyLeft.isDown() || ClientAccess.schizoMinecraft.options.keyRight.isDown())) {
         return this.pauseWithReason("Manual movement");
      }
      if (this.pauseUnloadedSetting.getValue()) {
         var player = ClientAccess.schizoMinecraft.player;
         double heading = Math.toRadians(player.getYRot());
         BlockPos ahead = BlockPos.containing(player.getX() - Math.sin(heading) * 4, player.getY(), player.getZ() + Math.cos(heading) * 4);
         if (!ClientAccess.schizoMinecraft.level.hasChunkAt(ahead)) return this.pauseWithReason("Terrain not loaded");
      }
      if (this.eatingSetting.getValue() && ClientAccess.schizoMinecraft.player.isUsingItem()) return this.pauseWithReason("Using item");
      return this.miningSetting.getValue() && this.isMining() && this.pauseWithReason("Mining");
   }

   @Override
   public void onToggle() {
      this.stuckTicks = 0;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      if (this.shouldPauseForAction()) {
         return;
      }
      double verticalDistance = Math.abs(ClientAccess.schizoMinecraft.player.getY() - ClientAccess.schizoMinecraft.player.yo);
      if (ClientContext.movementSpeedTracker.getCurrentSpeed() != 0.0 || !(verticalDistance < 0.2D)) {
         this.stuckTicks = 0;
         return;
      }
      if (this.stuckTicks == 5 && this.stuckNotifySetting.getValue()) {
         ChatUtils.printChat(Component.literal("You are stuck!"), ChatUtils.moduleSignature(this), ChatUtils.MessagePriority.HIGH);
         SoundClip sound = ClientContext.soundManager.getSound(this.soundSetting.getValue());
         sound.play(this.volumeSetting.getValue());
      }
      this.stuckTicks++;
   }

   @EventHandler
   private void onAbstractClientPlayerTickEvent(AbstractClientPlayerTickEvent event) {
      if (!this.shouldPauseForAction()) {
         ClientContext.inputOverrides.requestForwardMovement(this, 1.0F);
         ClientContext.inputOverrides.requestForward(this, true);
         if (this.autoJumpSetting.getValue()) {
            ClientContext.inputOverrides.requestJump(this, true);
         }
      }
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      speedMineModule = (SpeedMineModule)registry.getModule(SpeedMineModule.class);
   }

   public AutoWalkModule() {
      super("AutoWalk", "Lets your forward key rest.", ModuleCategory.MOVEMENT, new String[0]);
      this.pauseUnloadedSetting = this.add(new BooleanSetting("PauseUnloaded", true));
      this.manualOverrideSetting = this.add(new BooleanSetting("ManualOverride", true));
      this.autoJumpSetting = this.add(new BooleanSetting("AutoJump", false));
      this.stuckNotifySetting = this.add(new BooleanSetting("StuckNotify", false).markVisibilityDependency());
      this.soundSetting = this.add(new IdentifierSetting("Sound", BuiltinSound.WARNING.getIdentifier()).dependsOn(this.stuckNotifySetting));
      this.volumeSetting = this.add(new NumberSetting<>("Volume", 1.0F, 0.0F, 1.0F).dependsOn(this.stuckNotifySetting));
      this.pauseSetting = this.add(new BooleanSetting("Pause", false).markAsGroup());
      this.miningSetting = this.add(new BooleanSetting("Mining", false).dependsOn(this.pauseSetting));
      this.eatingSetting = this.add(new BooleanSetting("Eating", false).dependsOn(this.pauseSetting));
   }
}
