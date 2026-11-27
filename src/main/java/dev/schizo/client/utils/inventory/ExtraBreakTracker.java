/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import dev.schizo.client.ClientAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ExtraBreakTracker implements ClientAccess {
   private static final long TRACK_TTL_NANOS = TimeUnit.MILLISECONDS.toNanos(200L);
   private final List<BlockPos> expiredPositions = new ArrayList<>();
   private final Map<BlockPos, Long> trackedBreaks = new HashMap<>();
   private Block lastBlock;

   public synchronized void pruneExpired() {
      this.trackedBreaks.forEach(this::collectExpired);
      List<BlockPos> expiredSnapshot = this.expiredPositions;
      Map<BlockPos, Long> removalTarget = this.trackedBreaks;
      expiredSnapshot.forEach(removalTarget::remove);
      this.expiredPositions.clear();
   }

   public Block getLastBlock() {
      return this.lastBlock;
   }

   public synchronized boolean isTracked(BlockPos position) {
      return this.trackedBreaks.containsKey(position);
   }

   private void collectExpired(BlockPos position, long trackedAtNanos) {
      if (System.nanoTime() - trackedAtNanos >= TRACK_TTL_NANOS) {
         this.expiredPositions.add(position);
      }
   }

   public synchronized void clear() {
      this.trackedBreaks.clear();
      this.expiredPositions.clear();
      this.lastBlock = null;
   }

   public synchronized void trackBreak(BlockPos position) {
      Map<BlockPos, Long> breaksSnapshot = this.trackedBreaks;
      long trackedAt = System.nanoTime();
      breaksSnapshot.put(position, trackedAt);
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState blockState = level.getBlockState(position);
      if (!blockState.isAir()) {
         this.lastBlock = blockState.getBlock();
      }
   }

}
