/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.world.WorldUtils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpeedMineRebreakTracker implements ClientAccess {
   private static final long MINIMUM_REBREAK_DELAY_MILLIS = 750L;
   private final SpeedMineModule speedMineModule;
   private final ElapsedTimer rebreakTimer = new ElapsedTimer();
   private boolean tracking;
   private BlockPos pos;
   private volatile long generation;

   public void validateTarget() {
      if (this.tracking && this.pos != null) {
         BlockPos trackedPos = this.pos;
         boolean validTarget = false;
         if (WorldUtils.canMineBlock(trackedPos)) {
            BlockPos currentPos = this.pos;
            validTarget = !WorldUtils.isAir(currentPos);
         }
         if (!validTarget) {
            this.reset();
         }

         if (this.pos != null) {
            BlockPos positionToCompare = this.pos;
            SpeedMineModule module = this.speedMineModule;

            if (!positionToCompare.equals(module.getMiningPos())) {
               this.reset();
            }
         }
      }
   }

   public boolean hasRebreakDelayElapsed() {
      if (this.tracking && this.pos != null) {
         double latencyTicks = (ClientContext.serverStateManager.getLatencyMillis() * 15.0F) / 5e+01F;

         long delayMillisScratch = (long)(Math.ceil(latencyTicks) * 50.0D);

         long minimumDelayMillis = MINIMUM_REBREAK_DELAY_MILLIS;
         long latencyDelayMillis = delayMillisScratch;
         delayMillisScratch = Math.max(latencyDelayMillis, minimumDelayMillis);
         float clampedDelayMillis = (float)delayMillisScratch;
         ServerStateManager tickRateTracker = ClientContext.serverStateManager;

         delayMillisScratch = (long)(clampedDelayMillis / tickRateTracker.getTickRateScale());
         float scaledDelayMillis = (float)delayMillisScratch;
         Float instantDelay = this.speedMineModule.instantDelaySetting.getValue();
         delayMillisScratch = (long)(scaledDelayMillis + instantDelay);
         return this.rebreakTimer.hasElapsed(delayMillisScratch);
      } else {
         return false;
      }
   }

   public SpeedMineRebreakTracker(SpeedMineModule module) {
      this.speedMineModule = module;
   }

   public void reset() {
      this.generation++;
      this.tracking = false;
      this.pos = null;
      this.rebreakTimer.clear();
   }

   public void handleBlockUpdates(
      PacketEvent.Receive event) {
      long expectedGeneration = this.generation;

      if (event.getPacket() instanceof ClientboundBlockUpdatePacket blockUpdate
         && blockUpdate.getBlockState().isAir()) {
         BlockPos updatedPosition = blockUpdate.getPos().immutable();
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (this.generation == expectedGeneration
               && this.tracking
               && updatedPosition.equals(this.pos)) {
               this.reset();
            }
         });
      }

      if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
         ArrayList<BlockPos> airPositions = new ArrayList<>();
         sectionUpdate.runUpdates((updatedPos, updatedState) -> {
            if (updatedState.isAir()) {
               airPositions.add(updatedPos.immutable());
            }
         });
         if (!airPositions.isEmpty()) {
            List<BlockPos> airSnapshot = List.copyOf(airPositions);
            ClientAccess.schizoMinecraft.executeIfPossible(() -> {
               BlockPos currentPosition = this.pos;
               if (this.generation == expectedGeneration
                  && this.tracking
                  && currentPosition != null
                  && airSnapshot.contains(currentPosition)) {
                  this.reset();
               }
            });
         }
      }

      if (event.getPacket() instanceof ClientboundAddEntityPacket entitySpawn) {
         Vec3 spawnPosition = new Vec3(entitySpawn.getX(), entitySpawn.getY(), entitySpawn.getZ());
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            BlockPos currentPosition = this.pos;
            if (this.generation != expectedGeneration
               || !this.tracking
               || currentPosition == null) {
               return;
            }

            if (new AABB(currentPosition).contains(spawnPosition)) {
               this.reset();
            }
         });
      }
   }

   public void startTracking(BlockPos blockPos) {
      if (!this.tracking) {
         this.generation++;
         this.tracking = true;
         this.pos = blockPos.immutable();
         this.rebreakTimer.reset();
      }
   }

}
