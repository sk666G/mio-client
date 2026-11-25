/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.world;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.MonotonicTime;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record BlockStateSnapshot(
   BlockState state,
   BlockPos position,
   long capturedAtNanos
) implements ClientAccess {
   private static final long EXPIRY_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);

   public boolean hasExpired() {
      return this.hasExpired(System.nanoTime());
   }

   public boolean hasExpired(long nowNanos) {
      return MonotonicTime.hasElapsed(this.capturedAtNanos, nowNanos, EXPIRY_NANOS);
   }

   public static BlockState captureState(BlockPos position) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      return level.getBlockState(position);
   }
}
