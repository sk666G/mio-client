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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;

public class FastClimbModule extends Module {
   private static AntiCheatModule antiCheatModule;
   public final Setting<Float> speedSetting;
   public final Setting<Boolean> airborneSetting;
   private boolean climbableWhenGrounded;

   public boolean canFastClimb() {
      if (!this.isToggled()) {
         return false;
      }
      return this.airborneSetting.getValue() || this.climbableWhenGrounded;
   }

   private boolean isClimbableAtPlayer(BlockPos position) {
      return ClientAccess.schizoMinecraft.level.getBlockState(position).is(BlockTags.CLIMBABLE)
         && BlockPos.containing(ClientAccess.schizoMinecraft.player.position()).equals(position);
   }

   private void updateGroundedClimbable() {
      if (!ClientAccess.schizoMinecraft.player.onGround()) {
         return;
      }
      boolean foundClimbable = false;
      AABB bounds = ClientAccess.schizoMinecraft.player.getBoundingBox().inflate(0.1D);
      for (BlockPos position : BlockPos.betweenClosed(bounds)) {
         if (this.isClimbableAtPlayer(position)) {
            foundClimbable = true;
            break;
         }
      }
      this.climbableWhenGrounded = foundClimbable;
   }

   @EventHandler
   private void onSendPosition(LocalPlayerSendPositionEvent event) {
      if (EventStage.Pre == event.getStage()) {
         this.updateGroundedClimbable();
      }
      if (event.getStage() == EventStage.Post) {
         return;
      }
      if (!RotationManager.isMovementSyncEnabled()) {
         return;
      }
      if (!this.canFastClimb()) {
         return;
      }
      AABB scanBounds = ClientAccess.schizoMinecraft.player.getBoundingBox().inflate(0.1D);
      BlockPos.betweenClosedStream(scanBounds)
         .map(BlockPos::immutable)
         .filter(this::isClimbableAtPlayer)
         .forEach(position -> {
            if (!antiCheatModule.is2b2tEnabled()) {
               PacketSender.sendPlayerAction(Action.ABORT_DESTROY_BLOCK, position, Direction.UP);
            }
            PacketSender.sendPlayerAction(Action.STOP_DESTROY_BLOCK, position, Direction.UP);
         });
   }
   public static void bindModules(ModuleRegistry modules) {
      antiCheatModule = (AntiCheatModule)modules.getModule(AntiCheatModule.class);
   }

   public FastClimbModule() {
      super("FastClimb", "Makes you climb faster.", ModuleCategory.MOVEMENT, new String[0]);
      this.speedSetting = this.add(new NumberSetting<>("Speed", 0.2F, 0.0F, 1.0F));
      this.airborneSetting = this.add(new BooleanSetting("Airborne", true));
      this.speedSetting.setBoundaryLabel("Ignore", SettingLabelCondition.MIN);
   }
}
