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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.utils.movement.MovementPhysics;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;

public class FastSwimModule extends Module {
   private static SpeedMineModule speedMineModule;
   private final Setting<Float> glideSetting;
   private final Setting<Boolean> waterSetting;
   private final Setting<Float> waterSpeedSetting;
   private final Setting<Boolean> lavaSetting;
   private final Setting<Float> lavaSpeedSetting;
   private final Setting<Boolean> verticalSetting;
   private final Setting<Float> vSpeedSetting;
   public final Setting<Boolean> accelerateSetting;
   public final Setting<Float> accelMinSetting;
   public final Setting<Float> accelTimeSetting;
   private long accelerationStartMillis;

   private boolean shouldSkipSwimming() {
      if (!ClientAccess.schizoMinecraft.player.isInLava()) {
         if (!PlayerStateUtils.isInWater(ClientAccess.schizoMinecraft.player)) {
            return true;
         }
      }
      if (ClientAccess.schizoMinecraft.player.isInLava()) {
         if (!this.lavaSetting.getValue()) {
            return true;
         }
      }
      if (PlayerStateUtils.isInWater(ClientAccess.schizoMinecraft.player)) {
         if (!this.waterSetting.getValue()) {
            return true;
         }
      }
      if (ClientAccess.schizoMinecraft.player.isFallFlying()) {
         return true;
      }
      if (!ClientContext.serverStateManager.getSetbackTimer().hasElapsed(500L)) {
         return true;
      }
      if (speedMineModule.isToggled()) {
         double miningProgress = speedMineModule.getMiningProgress();
         double nextMiningProgress = miningProgress + speedMineModule.getTickBreakProgress();
         double threshold = speedMineModule.damageSetting.getValue();
         if (nextMiningProgress >= threshold - 0.1D) {
            if (speedMineModule.getMiningOrNearlyBrokenPos() != null) {
               BlockPos miningTargetSnapshot = speedMineModule.getMiningOrNearlyBrokenPos();
               if (WorldUtils.canMineBlock(miningTargetSnapshot)) {
                  return true;
               }
            }
         }
      }
      return ClientContext.baritoneBridge.isPathingOrCalculating();
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerTickEvent event) {
      if (!this.shouldSkipSwimming()) {
         ClientAccess.schizoMinecraft.player.avatarState().walkDist = 0.0F;
      }
   }

   @EventHandler
   private void onMove(LocalPlayerMoveEvent event) {
      if (this.shouldSkipSwimming()) {
         this.accelerationStartMillis = System.currentTimeMillis();
         return;
      }
      if (!MovementPhysics.isLocalPlayerMoving()) {
         this.accelerationStartMillis = System.currentTimeMillis();
      }
      float verticalSpeed = this.verticalSetting.getValue() ? 0.12F * this.vSpeedSetting.getValue() : 0.0F;
      float minimumSpeed = 0.12F * this.accelMinSetting.getValue();
      double horizontalSpeed = 0.12F * this.lavaSpeedSetting.getValue();
      if (this.waterSetting.getValue() && PlayerStateUtils.isInWater(ClientAccess.schizoMinecraft.player)) {
         horizontalSpeed = 0.12F * this.waterSpeedSetting.getValue();
      }
      if (this.accelerateSetting.getValue() && minimumSpeed < horizontalSpeed) {
         float elapsedMillis = (float)(System.currentTimeMillis() - this.accelerationStartMillis);
         float accelerationSeconds = this.accelTimeSetting.getValue();
         float progress = Mth.clamp(elapsedMillis / (accelerationSeconds * 1000.0F), 0.0F, 1.0F);
         horizontalSpeed = Math.min(minimumSpeed + (horizontalSpeed - minimumSpeed) * progress, horizontalSpeed);
      }
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player.input.keyPresses.jump()) {
         event.setY(verticalSpeed);
         player.setDeltaMovement(player.getDeltaMovement().with(Axis.Y, verticalSpeed));
      } else if (player.input.keyPresses.shift()) {
         event.setY(-verticalSpeed);
         player.setDeltaMovement(player.getDeltaMovement().with(Axis.Y, -verticalSpeed));
         horizontalSpeed /= 2.0D;
      } else {
         event.setY(0.0);
         player.setDeltaMovement(player.getDeltaMovement().with(Axis.Y, 0.0));
         if (!player.verticalCollision && this.glideSetting.getValue() != 0.0F) {
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, -this.glideSetting.getValue(), 0.0));
            event.setY(player.getDeltaMovement().y);
         }
      }
      double[] horizontal = MovementPhysics.applyHorizontalMotion(event, horizontalSpeed);
      player.setDeltaMovement(horizontal[0], player.getDeltaMovement().y, horizontal[1]);
   }
   public static void bindReferencedModules(ModuleRegistry modules) {
      speedMineModule = (SpeedMineModule)modules.getModule(SpeedMineModule.class);
   }

   public FastSwimModule() {
      super("FastSwim", "Makes you move faster while in liquids.", ModuleCategory.MOVEMENT, new String[0]);
      this.glideSetting = this.add(new NumberSetting<>("Glide", 0.05F, 0.0F, 5.0F));
      this.waterSetting = this.add(new BooleanSetting("Water", false).markVisibilityDependency());
      this.waterSpeedSetting = this.add(new NumberSetting<>("Speed", 1.0F, 0.1F, 3.0F).dependsOn(this.waterSetting));
      this.lavaSetting = this.add(new BooleanSetting("Lava", true).markVisibilityDependency());
      this.lavaSpeedSetting = this.add(new NumberSetting<>("Speed", 1.0F, 0.1F, 3.0F).dependsOn(this.lavaSetting));
      this.verticalSetting = this.add(new BooleanSetting("Vertical", false).markVisibilityDependency());
      this.vSpeedSetting = this.add(new NumberSetting<>("VSpeed", 1.0F, 0.1F, 2.0F).dependsOn(this.verticalSetting));
      this.accelerateSetting = this.add(new BooleanSetting("Accelerate", false).markVisibilityDependency());
      this.accelMinSetting = this.add(new NumberSetting<>("AccelMin", 1.0F, 0.0F, 5.0F).dependsOn(this.accelerateSetting));
      NumberSetting<Float> accelTime = new NumberSetting<>("AccelTime", 1.0F, 0.1F, 10.0F);
      accelTime.withUnit("s");
      this.accelTimeSetting = this.add(accelTime.dependsOn(this.accelerateSetting));
      Setting<Float> identitySetting = this.waterSpeedSetting;
      identitySetting.setConfigName("WaterSpeed");
      identitySetting = this.lavaSpeedSetting;
      identitySetting.setConfigName("LavaSpeed");
   }
}
