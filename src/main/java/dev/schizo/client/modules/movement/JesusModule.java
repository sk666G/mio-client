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
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.BlockCollisionShapeEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.mixin.ServerboundMovePlayerPacketAccessor;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JesusModule extends Module {
   final Setting<ModeChoice> modeSetting;
   private final Setting<Boolean> strictSetting;
   private final Setting<Float> ascendingSetting;
   private final double[] strictYOffsetSequence;
   private int strictOffsetIndex;

   @Override
   public String getInfo() {
      return (this.modeSetting.getValue()).getName();
   }

   @Override
   public void onToggle() {
      this.strictOffsetIndex = 0;
   }

   private boolean isStableAboveWater() {
      if (Math.abs(ClientAccess.schizoMinecraft.player.yo - ClientAccess.schizoMinecraft.player.getY()) > 0.01D) {
         return false;
      }
      for (BlockPos position : PlayerStateUtils.getOccupiedBlockPositions(ClientAccess.schizoMinecraft.player)) {
         if (WorldUtils.getBlock(position.below()) != Blocks.WATER) {
            return false;
         }
      }
      return true;
   }

   public boolean canWalkOnWater() {
      if (this.isWorldUnavailable() || ClientAccess.schizoMinecraft.player.input == null) {
         return false;
      }
      if (ClientAccess.schizoMinecraft.player.isPassenger()) {
         return false;
      }
      return !ClientAccess.schizoMinecraft.player.input.keyPresses.shift();
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerTickEvent event) {
      if (!this.canWalkOnWater() || !ClientAccess.schizoMinecraft.player.isInWater()) {
         return;
      }
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      double upwardImpulse = this.modeSetting.getValue() == ModeChoice.DOLPHIN
         ? 0.0399D : 0.08D * this.ascendingSetting.getValue();
      player.push(0.0, upwardImpulse, 0.0);
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.modeSetting.getValue() != JesusModule.ModeChoice.SOLID || !this.canWalkOnWater()) {
         return;
      }
      if (!(event.getPacket() instanceof ServerboundMovePlayerPacket movementPacket)) {
         return;
      }
      if (!this.strictSetting.getValue() || !this.isStableAboveWater()) {
         return;
      }
      ServerboundMovePlayerPacketAccessor packetAccessor = (ServerboundMovePlayerPacketAccessor)movementPacket;
      packetAccessor.setOnGround(false);
      packetAccessor.setY(movementPacket.getY(0.0) + this.strictYOffsetSequence[this.strictOffsetIndex++]);
      this.strictOffsetIndex %= this.strictYOffsetSequence.length;
   }

   @EventHandler
   private void onCollisionShape(BlockCollisionShapeEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      if (ClientAccess.schizoMinecraft.player.isInWater()) {
         return;
      }
      if (this.modeSetting.getValue() != JesusModule.ModeChoice.SOLID || !this.canWalkOnWater()) {
         return;
      }
      float blockYAsFloat = event.getPos().getY();
      double liquidSurfaceY = blockYAsFloat + 0.2F;
      if (liquidSurfaceY > ClientAccess.schizoMinecraft.player.getY()) {
         return;
      }
      if (event.getState().is(Blocks.WATER)) {
         AABB waterBounds = new AABB(0.0, 0.0, 0.0, 1.0D, 0.98D, 1.0D);
         VoxelShape waterShape = Shapes.create(waterBounds);
         event.setShape(waterShape);
      }
   }

   public JesusModule() {
      super("Jesus", "Allows you to walk on water.", ModuleCategory.MOVEMENT, new String[0]);
      this.modeSetting = this.add(new EnumSetting<>("Mode", JesusModule.ModeChoice.SOLID));
      Predicate<Object> showStrict = ignored -> this.modeSetting.getValue() == ModeChoice.SOLID;
      this.strictSetting = this.add(new BooleanSetting("Strict", false, showStrict));
      Predicate<Object> showAscending = ignored -> this.modeSetting.getValue() == ModeChoice.SOLID;
      this.ascendingSetting = this.add(new NumberSetting<>("Ascending", 1.0F, 0.1F, 1.0F, showAscending));
      this.strictYOffsetSequence = new double[]{
         0.08232659236482401D, 0.13927999979019162D, 0.1654239999427795D,
         0.11427136035293574D, 0.04194693730418576D, 0.01236341326161235D
      };
   }
   public enum ModeChoice implements NamedEntry {
      SOLID("Solid"),
      DOLPHIN("Dolphin");
      private final String displayName;
      @Override
      public String getName() {
         return this.displayName;
      }
      ModeChoice(String displayName) {
         this.displayName = displayName;
      }
   }
}
