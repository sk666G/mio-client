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
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.managers.TickRateController;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class AvoidModule extends Module {
   private final Setting<Boolean> unloadSetting;
   private final Setting<Float> timerSetting;
   private final Setting<Boolean> voidSetting;
   private final Setting<ModeChoice> modeSetting;
   private final Setting<Integer> heightSetting;
   @Override
   public String getInfo() {
      if (!this.voidSetting.getValue()) {
         return null;
      }
      return FontRenderer.capitalize(this.modeSetting.getValue());
   }

   @Override
   public void onDisable() {
      ClientContext.tickRateController.releaseMultiplier(this);
   }

   @EventHandler
   private void onClientPlayerTick(AbstractClientPlayerTickEvent event) {
      if (this.unloadSetting.getValue() && !this.areNearbyChunksLoaded()) {
         TickRateController timer = ClientContext.tickRateController;
         Float multiplier = this.timerSetting.getValue();
         timer.requestMultiplier(this, multiplier);
      } else {
         TickRateController timer = ClientContext.tickRateController;
         timer.releaseMultiplier(this);
      }
   }

   private boolean areNearbyChunksLoaded() {
      ClientChunkCache chunks = ClientAccess.schizoMinecraft.level.getChunkSource();
      ChunkPos playerChunk = ClientAccess.schizoMinecraft.player.chunkPosition();
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            if (!chunks.hasChunk(playerChunk.x + x, playerChunk.z + z)) {
               return false;
            }
         }
      }
      return true;
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerTickEvent event) {
      if (this.voidSetting.getValue()) {
         this.preventVoidFall();
      }
   }

   private void preventVoidFall() {
      if (ClientAccess.schizoMinecraft.player.onGround()) {
         return;
      }
      if (PlayerStateUtils.getTrackedFallDistance() <= 0.0F) {
         return;
      }
      ClientLevel rayLevel = ClientAccess.schizoMinecraft.level;
      ClipBlockStateContext rayContext = new ClipBlockStateContext(
         ClientAccess.schizoMinecraft.player.position(),
         new Vec3(ClientAccess.schizoMinecraft.player.getX(), ClientAccess.schizoMinecraft.level.getMinY() - 1, ClientAccess.schizoMinecraft.player.getZ()),
         state -> state.isSolid()
      );
      if (rayLevel.isBlockInLine(rayContext).getType() != Type.MISS) {
         return;
      }
      int minimumY = ClientAccess.schizoMinecraft.level.getMinY();
      Integer configuredHeight = this.heightSetting.getValue();
      double triggerY = minimumY + configuredHeight;
      if (triggerY < ClientAccess.schizoMinecraft.player.getY()) {
         return;
      }
      if (this.modeSetting.getValue() == AvoidModule.ModeChoice.TELEPORT) {
         ClientAccess.schizoMinecraft.player.setDeltaMovement(0.0, 10.0D, 0.0);
      } else {
         LocalPlayer stopPlayer = ClientAccess.schizoMinecraft.player;
         Vec3 stoppedVelocity = stopPlayer.getDeltaMovement().with(Axis.Y, 0.0);
         stopPlayer.setDeltaMovement(stoppedVelocity);
         ClientAccess.schizoMinecraft.player.setOnGround(true);
      }
      return;
   }
   public AvoidModule() {
      super("Avoid", "Prevents you from getting into dangerous situations.", ModuleCategory.MOVEMENT, "AntiVoid");
      this.unloadSetting = this.add(new BooleanSetting("Unload", false).markVisibilityDependency());
      this.timerSetting = this.add(new NumberSetting<>("Timer", 0.25F, 0.1F, 0.5F).dependsOn(this.unloadSetting));
      this.voidSetting = this.add(new BooleanSetting("Void", true).markVisibilityDependency());
      this.modeSetting = this.add(new EnumSetting<>("Mode", AvoidModule.ModeChoice.CANCEL).dependsOn(this.voidSetting));
      this.heightSetting = this.add(new NumberSetting<>("Height", 30, 1, 100).withUnit("m").dependsOn(this.voidSetting));
      this.setDrawn(false);
   }
   public enum ModeChoice implements NamedEntry {
      TELEPORT("Teleport"),
      CANCEL("Cancel");
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
