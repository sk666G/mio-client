/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.world.BlockStateUtils;
import dev.schizo.client.utils.world.HoleInfo;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.settings.options.DimensionType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class HoleManager implements ClientAccess {

   private static final Direction[] horizontalDirections;

   private static final MutableBlockPos classificationCursor;

   private static final int SCAN_MIN_OFFSET = -16;
   private static final int SCAN_SIZE = 32;
   private static final int TOTAL_SCAN_POSITIONS = SCAN_SIZE * SCAN_SIZE * SCAN_SIZE;
   private static final int SCAN_BUDGET_PER_TICK = 2048;

   private volatile List<HoleInfo> holes = List.of();
   private final ArrayList<HoleInfo> pendingHoles = new ArrayList<>();
   private final Set<Long> coveredPositions = new HashSet<>();
   private final MutableBlockPos scanCursor = new MutableBlockPos();
   private ClientLevel scanLevel;
   private LocalPlayer scanPlayer;
   private BlockPos scanOrigin;
   private int scanIndex;
   private boolean inHole;

   public boolean intersectsHole(BlockPos position) {
      return this.intersectsHole(new AABB(position));
   }

   public boolean intersectsHole(AABB bounds) {
      return this.holes.stream().anyMatch(
         hole -> {
            AABB holeBounds = hole.bounds();
            return holeBounds.intersects(bounds);
         }
      );
   }

   public List getHoles() {
      return this.holes;
   }

   public HoleInfo findIntersectingHole(BlockPos position) {
      Stream<HoleInfo> matchingHoles = this.holes.stream().filter(
            hole -> {
               AABB holeBounds = hole.bounds();
               return holeBounds.intersects(new AABB(position));
            }
      );
      Optional<HoleInfo> firstMatch = matchingHoles.findFirst();
      return firstMatch.orElse(null);
   }

   public boolean isInHole() {
      return this.inHole;
   }

   public HoleManager() {
      ClientAccess.eventBus.subscribe(this);
   }

   static {
      Plane horizontalPlane = Plane.HORIZONTAL;
      horizontalDirections = horizontalPlane.stream().toArray((IntFunction<Direction[]>)Direction[]::new);
      classificationCursor = new MutableBlockPos();
   }

   static HoleInfo classifyHole(
      BlockPos position, Direction fromDirection) {
      classificationCursor.set(position);
      MutableBlockPos cursorSnapshot = classificationCursor;
      if (BlockStateUtils.isBlockingExceptCarpet(cursorSnapshot)) {
         return null;
      }
      Direction down = Direction.DOWN;
      classificationCursor.move(down);
      MutableBlockPos floorPosition = classificationCursor;
      BlockState floorState = ClientAccess.schizoMinecraft.level.getBlockState(floorPosition);
      if (!BlockStateUtils.isBlocking(floorState)) {
         return null;
      }
      if (floorState.getBlock() instanceof Portal) {
         return null;
      }
      if (floorState.getBlock() instanceof LiquidBlock) {
         return null;
      }
      cursorSnapshot = classificationCursor;
      Direction up = Direction.UP;
      cursorSnapshot.move(up, 2);
      cursorSnapshot = classificationCursor;
      if (BlockStateUtils.isBlocking(cursorSnapshot)) {
         return null;
      }
      Direction roofDirection = Direction.UP;
      MutableBlockPos roofCursor = classificationCursor;
      cursorSnapshot = roofCursor.move(roofDirection);
      boolean trapped = BlockStateUtils.isBlocking(cursorSnapshot);
      HoleInfo.Safety safety = BlockStateUtils.isBlastResistantUnbreakable(
            floorState)
         ? HoleInfo.Safety.SAFE
         : HoleInfo.Safety.UNSAFE;
      Direction doubleDirection = null;
      for (Direction direction : horizontalDirections) {
         if (fromDirection == direction.getOpposite()) {
            continue;
         }
         MutableBlockPos sideResetCursor = classificationCursor;
         cursorSnapshot = sideResetCursor.set(position);
         cursorSnapshot.move(direction);
         MutableBlockPos sidePosition = classificationCursor;
         BlockState sideState = ClientAccess.schizoMinecraft.level.getBlockState(sidePosition);
         cursorSnapshot = classificationCursor;
         if (!BlockStateUtils.isBlocking(cursorSnapshot)) {
            if (doubleDirection != null || fromDirection != null) {
               return null;
            }
            BlockPos adjacentBlock = position.relative(direction);
            HoleInfo adjacentHole = classifyHole(adjacentBlock, direction);
            if (adjacentHole == null) {
               return null;
            }
            if (!adjacentHole.trapped()) {
               trapped = false;
            }
            if (safety == HoleInfo.Safety.SAFE) {
               safety = adjacentHole.safety();
            }
            doubleDirection = direction;
            continue;
         }
         Block anchorBlock = Blocks.RESPAWN_ANCHOR;
         if (sideState.is(anchorBlock)) {
            DimensionType anchorPolicy = ClientContext.worldStateManager.getDimension();
            if (!anchorPolicy.isNether()) {
               return null;
            }
         }
         if (BlockStateUtils.isWeakOrInstantlyBreakable(sideState)) {
            return null;
         }
         if (BlockStateUtils.isBlastResistantBreakable(sideState)) {
            safety = HoleInfo.Safety.UNSAFE;
         }
      }
      return new HoleInfo(safety, position.immutable(), createHoleBounds(position, doubleDirection), trapped);
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (level == null || player == null) {
         this.resetScanState(true);
         return;
      }

      if (this.scanLevel != level || this.scanPlayer != player || this.scanOrigin == null) {
         this.startScan(level, player, player.blockPosition());
      }

      this.scanSlice();
      BlockPos roundedPosition = PlayerStateUtils.getLocalRoundedBlockPosition();
      this.inHole = this.intersectsHole(roundedPosition);
   }

   @EventHandler
   private void onDisconnect(MinecraftDisconnectEvent event) {
      this.resetScanState(true);
   }

   private void startScan(ClientLevel level, LocalPlayer player, BlockPos origin) {
      this.scanLevel = level;
      this.scanPlayer = player;
      this.scanOrigin = origin.immutable();
      this.scanIndex = 0;
      this.pendingHoles.clear();
      this.coveredPositions.clear();
   }

   private void scanSlice() {
      if (this.scanOrigin == null) {
         return;
      }

      int processed = 0;
      boolean measuring = dev.schizo.client.profiling.EventProfiler.GLOBAL.isEnabled();
      long scanStart = measuring ? System.nanoTime() : 0L;
      try {
         while (processed < SCAN_BUDGET_PER_TICK && this.scanIndex < TOTAL_SCAN_POSITIONS) {
            int index = this.scanIndex++;
            int xOffset = index / (SCAN_SIZE * SCAN_SIZE) + SCAN_MIN_OFFSET;
            int remainder = index % (SCAN_SIZE * SCAN_SIZE);
            int yOffset = remainder / SCAN_SIZE + SCAN_MIN_OFFSET;
            int zOffset = remainder % SCAN_SIZE + SCAN_MIN_OFFSET;

            this.scanCursor.set(
               this.scanOrigin.getX() + xOffset,
               this.scanOrigin.getY() + yOffset,
               this.scanOrigin.getZ() + zOffset
            );
            long positionKey = this.scanCursor.asLong();
            if (!this.coveredPositions.contains(positionKey)) {
               HoleInfo hole = classifyHole(this.scanCursor, null);
               if (hole != null) {
                  this.pendingHoles.add(hole);
                  this.addCoveredPositions(hole.bounds());
               }
            }
            processed++;
         }

         if (this.scanIndex >= TOTAL_SCAN_POSITIONS) {
            this.holes = List.copyOf(this.pendingHoles);
            this.startScan(this.scanLevel, this.scanPlayer, this.scanPlayer.blockPosition());
         }

      } finally {
         if (measuring) {
            dev.schizo.client.profiling.ScanWorkTracker.GLOBAL.record(
               dev.schizo.client.profiling.ScanWorkTracker.Source.HOLES, System.nanoTime() - scanStart, processed);
         }
      }
   }

   private void addCoveredPositions(AABB bounds) {
      for (int x = (int)bounds.minX; x < (int)bounds.maxX; x++) {
         for (int z = (int)bounds.minZ; z < (int)bounds.maxZ; z++) {
            int y = (int)bounds.minY;
            this.coveredPositions.add(BlockPos.asLong(x, y, z));
            this.coveredPositions.add(BlockPos.asLong(x, y + 1, z));
            this.coveredPositions.add(BlockPos.asLong(x, y + 2, z));
         }
      }
   }

   private void resetScanState(boolean clearPublishedHoles) {
      this.scanLevel = null;
      this.scanPlayer = null;
      this.scanOrigin = null;
      this.scanIndex = 0;
      this.pendingHoles.clear();
      this.coveredPositions.clear();
      this.inHole = false;
      if (clearPublishedHoles) {
         this.holes = List.of();
      }
   }

   private static AABB createHoleBounds(BlockPos position, Direction face) {
      if (face == null) return new AABB(position);
      AABB box = new AABB(position);
      Vec3i xVector = face.getUnitVec3i();
      double x = xVector.getX();
      Vec3i zVector = face.getUnitVec3i();
      double z = zVector.getZ();
      return box.expandTowards(x, 0.0, z);
   }
}
