/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import dev.schizo.client.diagnostics.RuntimeDiagnostics;
import dev.schizo.client.utils.IncrementalWorkBudget;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.render.WorldBoxRenderer;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.world.ChunkScanner;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TunnelsModule extends Module {
   private final Setting<Boolean> verticalsSetting;
   private final Setting<Float> lineWidthSetting;
   private final Setting<Float> heightSetting;
   private final Setting<Integer> minLengthSetting;
   private final Setting<Color> fillSetting;
   private final Setting<Color> outlineSetting;
   private static final MutableBlockPos scanPosition;
   private final ElapsedTimer mergeTimer;
   private volatile Queue<Tunnel> pendingTunnels;
   private volatile List<Tunnel> tunnels;
   private final AtomicBoolean mergeInProgress;
   private static final int CHUNK_SCAN_BUDGET_PER_TICK = 2048;
   private final ChunkScanner chunkScanner;
   private final Queue<ChunkPos> pendingChunkScans;
   private final Set<Long> queuedChunkScans;
   private TunnelsChunkScanCursor activeChunkScan;
   private ChunkPos activeChunkPosition;
   private ClientLevel scanLevel;
   private volatile long runtimeGeneration;

   private boolean isNonSolid(BlockPos position) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState state = level.getBlockState(position);
      return !state.isSolid();
   }

   private boolean isHorizontalTunnelCell(BlockPos position) {
      if (!this.isNonSolid(position.above())
         || this.isNonSolid(position.below())
         || this.isNonSolid(position.above().above())) {
         return false;
      }

      if (this.isNonSolid(position.north())
         && this.isNonSolid(position.south())
         && this.isNonSolid(position.above().north())
         && this.isNonSolid(position.above().south())) {
         return !this.isNonSolid(position.east())
            && !this.isNonSolid(position.west())
            && !this.isNonSolid(position.above().east())
            && !this.isNonSolid(position.above().west());
      }

      return this.isNonSolid(position.east())
         && this.isNonSolid(position.west())
         && this.isNonSolid(position.above().east())
         && this.isNonSolid(position.above().west())
         && !this.isNonSolid(position.north())
         && !this.isNonSolid(position.south())
         && !this.isNonSolid(position.above().north())
         && !this.isNonSolid(position.above().south());
   }

   static {
      scanPosition = new MutableBlockPos();
   }

   @Override
   public void onEnable() {
      this.resetChunkScanState(true);
      this.scanLevel = ClientAccess.schizoMinecraft.level;
      ChunkScanner capturedScanner = this.chunkScanner;
      capturedScanner.start();
      if (!this.isWorldUnavailable()) {
         capturedScanner.scanLoadedChunks();
      }
   }

   @Override
   public void onDisable() {
      this.resetChunkScanState(true);
      ChunkScanner scanner = this.chunkScanner;
      scanner.stop();
   }

   private boolean isVerticalTunnelCell(BlockPos position) {
      Boolean scanVerticals = this.verticalsSetting.getValue();
      if (!scanVerticals) {
         return false;
      }

      Plane horizontalPlane = Plane.HORIZONTAL;
      Iterator<Direction> directions = horizontalPlane.iterator();
      while (directions.hasNext()) {
         Direction horizontalDirection = directions.next();
         BlockPos sidePosition = position.relative(horizontalDirection);
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         BlockState sideState = level.getBlockState(sidePosition);
         if (!sideState.isSolid()) {
            return false;
         }

         Block bambooBlock = Blocks.BAMBOO_BLOCK;
         if (sideState.is(bambooBlock)) {
            return false;
         }

         Block basaltBlock = Blocks.BASALT;
         if (sideState.is(basaltBlock)) {
            return false;
         }
      }

      Plane verticalPlane = Plane.VERTICAL;
      directions = verticalPlane.iterator();
      while (directions.hasNext()) {
         Direction verticalDirection = directions.next();
         BlockPos firstVerticalPosition = position.relative(verticalDirection);
         if (!this.isNonSolid(firstVerticalPosition)) {
            return false;
         }
         BlockPos secondVerticalPosition = firstVerticalPosition.relative(verticalDirection);
         if (!this.isNonSolid(secondVerticalPosition)) {
            return false;
         }
      }
      return true;
   }

   private boolean isTunnelCenterInChunk(
      TunnelsModule.Tunnel tunnel, ChunkPos chunkPosition
   ) {

      int maxX = tunnel.maxX;
      int minX = tunnel.minX;
      float centerX = Mth.lerpInt(0.5F, minX, maxX);

      int maxZ = tunnel.maxZ;
      int minZ = tunnel.minZ;
      float centerZ = Mth.lerpInt(0.5F, minZ, maxZ);
      return centerX >= chunkPosition.getMinBlockX()
         && centerX <= chunkPosition.getMaxBlockX()
         && centerZ >= chunkPosition.getMinBlockZ()
         && centerZ <= chunkPosition.getMaxBlockZ();
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      if (!this.isWorldUnavailable()) {
         this.syncScanLevel();
         this.scanChunkSlice();
         AtomicBoolean mergeFlag = this.mergeInProgress;
         if (!mergeFlag.get()) {
            ElapsedTimer capturedTimer = this.mergeTimer;

            ElapsedTimer timerForElapsedCheck = capturedTimer;
            if (timerForElapsedCheck.hasElapsed(75L)) {
               capturedTimer = this.mergeTimer;

               capturedTimer.reset();
               boolean busy = true;
               AtomicBoolean busyFlag = this.mergeInProgress;
               busyFlag.set(busy);
               List<Tunnel> currentTunnels = this.tunnels;
               ArrayList<Tunnel> snapshot = new ArrayList<>(currentTunnels.size());
               for (Tunnel tunnel : currentTunnels) {
                  snapshot.add(tunnel.copy());
               }
               ClientLevel expectedLevel = ClientAccess.schizoMinecraft.level;
               LocalPlayer expectedPlayer = ClientAccess.schizoMinecraft.player;
               Vec3 eyePosition = expectedPlayer.getEyePosition();
               long expectedGeneration = this.runtimeGeneration;
               Queue<Tunnel> pendingSnapshot = this.pendingTunnels;
               ClientAccess.executorService.submit(
                  (Runnable)() -> this.mergePendingTunnels(
                     snapshot,
                     pendingSnapshot,
                     eyePosition,
                     expectedLevel,
                     expectedPlayer,
                     expectedGeneration
                  )
               );
            }
         }
      }
   }

   @EventHandler
   private void onRenderWorld(WorldRenderEvent.Main event) {
      if (!this.isWorldUnavailable()) {
         List<Tunnel> visibleTunnels = this.tunnels;
         Iterator<Tunnel> iterator = visibleTunnels.iterator();

         while (iterator.hasNext()) {
            TunnelsModule.Tunnel tunnel = iterator.next();
            AABB bounds = tunnel.getBounds();
            double horizontalLength = Math.max(bounds.getXsize(), bounds.getZsize());
            double length = Math.max(horizontalLength, bounds.getYsize());
            Integer minimumLength = this.minLengthSetting.getValue();
            if (!(length < minimumLength.intValue())) {
               float renderHeight = (this.heightSetting.getValue());
               if (tunnel.vertical) {
                  renderHeight = (float)bounds.getYsize();
               }

               double renderTop = bounds.minY + renderHeight;
               AABB renderBounds = bounds.setMaxY(renderTop);

               if (RotationManager.isVisible(renderBounds)) {
                  PoseStack fillPoses = event.getPoseStack();
                  Setting<Color> fillOption = this.fillSetting;
                  WorldBoxRenderer.fillBox(
                     fillPoses, renderBounds, fillOption.getValue()
                  );
                  PoseStack outlinePoses = event.getPoseStack();
                  Color outlineColor = this.outlineSetting.getValue();
                  Float lineWidth = this.lineWidthSetting.getValue();
                  WorldBoxRenderer.outlineBox(outlinePoses, renderBounds, outlineColor, lineWidth);
               }
            }
         }
      }
   }

   private void enqueueChunkScan(ChunkPos chunkPosition, LevelChunk unusedChunk) {
      ChunkPos copiedPosition = new ChunkPos(chunkPosition.x, chunkPosition.z);
      long key = copiedPosition.toLong();
      if (this.queuedChunkScans.add(key)) {
         this.pendingChunkScans.add(copiedPosition);
      }
   }

   private void syncScanLevel() {
      ClientLevel currentLevel = ClientAccess.schizoMinecraft.level;
      if (this.scanLevel != currentLevel) {
         this.resetChunkScanState(true);
         this.scanLevel = currentLevel;
         if (currentLevel != null) {
            this.chunkScanner.scanLoadedChunks();
         }
      }
   }

   private void scanChunkSlice() {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      if (level == null) {
         return;
      }

      int processed = 0;
      while (IncrementalWorkBudget.canContinue(processed, CHUNK_SCAN_BUDGET_PER_TICK)) {
         if (this.activeChunkScan == null) {
            ChunkPos nextChunk = this.pendingChunkScans.poll();
            if (nextChunk == null) {
               return;
            }
            this.activeChunkPosition = nextChunk;
            this.activeChunkScan = new TunnelsChunkScanCursor(
               nextChunk.getMinBlockX(),
               nextChunk.getMinBlockZ(),
               level.getMinY() + 1,
               (int)WorldUtils.getTopY() - 2
            );
         }

         TunnelsChunkScanCursor.Cell cell = this.activeChunkScan.next();
         if (cell == null) {
            this.queuedChunkScans.remove(this.activeChunkPosition.toLong());
            this.activeChunkScan = null;
            this.activeChunkPosition = null;
            continue;
         }

         int x = cell.x();
         int y = cell.y();
         int z = cell.z();
         this.scanPosition.set(x, y, z);
         if (this.isNonSolid(this.scanPosition)) {
            if (this.isVerticalTunnelCell(this.scanPosition)) {
               this.pendingTunnels.add(
                  new Tunnel(x, y, z, x + 1, y + 1, z + 1, true)
               );
            } else if (this.isHorizontalTunnelCell(this.scanPosition)) {
               this.pendingTunnels.add(
                  new Tunnel(x, y, z, x + 1, y + 1, z + 1, false)
               );
            }
         }
         processed++;
      }
   }

   private void removeChunkState(ChunkPos chunkPosition) {
      long key = chunkPosition.toLong();
      this.queuedChunkScans.remove(key);
      this.pendingChunkScans.removeIf(queued -> queued.toLong() == key);
      if (this.activeChunkPosition != null && this.activeChunkPosition.toLong() == key) {
         this.activeChunkScan = null;
         this.activeChunkPosition = null;
      }

      List<Tunnel> current = this.tunnels;
      ArrayList<Tunnel> retained = new ArrayList<>(current.size());
      for (Tunnel tunnel : current) {
         if (!this.isTunnelCenterInChunk(tunnel, chunkPosition)) {
            retained.add(tunnel);
         }
      }
      this.tunnels = retained;
   }

   private void resetChunkScanState(boolean clearTunnels) {
      this.runtimeGeneration++;
      this.pendingChunkScans.clear();
      this.queuedChunkScans.clear();
      this.activeChunkScan = null;
      this.activeChunkPosition = null;
      this.pendingTunnels = new ConcurrentLinkedQueue<>();
      this.mergeInProgress.set(false);
      if (clearTunnels) {
         this.tunnels = Collections.emptyList();
      }
   }

   @NotNull
   private static Set<Tunnel> collectMergedTunnels(List<Tunnel> tunnels) {
      HashSet<Tunnel> merged = new HashSet<>();

      for (int index = 0; index < tunnels.size(); index++) {
         TunnelsModule.Tunnel tunnel = tunnels.get(index);
         if (!merged.contains(tunnel)) {
            for (int candidateIndex = index + 1; candidateIndex < tunnels.size(); candidateIndex++) {
               TunnelsModule.Tunnel candidate = tunnels.get(candidateIndex);
               if (!merged.contains(candidate)) {
                  if (tunnel.canMergeWith(candidate)) {
                     tunnel.merge(candidate);
                     merged.add(candidate);
                  }
               }
            }
         }
      }

      return merged;
   }

   private void mergePendingTunnels(
      List<Tunnel> snapshot,
      Queue<Tunnel> pendingSnapshot,
      Vec3 eyePosition,
      ClientLevel expectedLevel,
      LocalPlayer expectedPlayer,
      long expectedGeneration
   ) {
      try {
         boolean changed = false;

         TunnelsModule.Tunnel pending;
         while ((pending = pendingSnapshot.poll()) != null) {
            boolean mergedPending = false;
            Iterator<Tunnel> iterator = snapshot.iterator();

            while (iterator.hasNext()) {
               TunnelsModule.Tunnel existing = iterator.next();
               if (existing.canMergeWith(pending)) {
                  existing.merge(pending);
                  mergedPending = true;
                  changed = true;
                  break;
               }
            }

            if (!mergedPending) {
               snapshot.add(pending);
               changed = true;
            }
         }

         if (changed) {

            Set<Tunnel> merged = collectMergedTunnels(snapshot);
            snapshot.removeAll(merged);
         }

         snapshot.removeIf((Predicate<TunnelsModule.Tunnel>)tunnel -> {
            double distanceSquared = tunnel.distanceToCenterSquared(eyePosition);

            return distanceSquared > 65536.0D;
         });
         BlockableEventLoop<?> client = ClientAccess.schizoMinecraft;
         client.executeIfPossible(() -> {
            if (this.isToggled()
               && this.runtimeGeneration == expectedGeneration
               && ClientAccess.schizoMinecraft.level == expectedLevel
               && ClientAccess.schizoMinecraft.player == expectedPlayer) {
               this.tunnels = snapshot;
            }
         });
      } catch (RuntimeException mergeFailure) {
         RuntimeDiagnostics.record(
            RuntimeDiagnostics.Area.RENDER,
            "Tunnel scan merge",
            mergeFailure
         );
      } finally {
         if (this.runtimeGeneration == expectedGeneration) {
            this.mergeInProgress.set(false);
         }
      }
   }

   public TunnelsModule() {
      super("Tunnels", "Highlights dug-out tunnels.", ModuleCategory.RENDER, new String[0]);
      this.verticalsSetting = this.add(new BooleanSetting(new String("Verticals"), true));
      this.lineWidthSetting = this.add(new NumberSetting<>(new String("LineWidth"), 1.0F, 0.1F, 3.0F));
      this.heightSetting = this.add(new NumberSetting<>(new String("Height"), 0.1F, -1.0F, 2.0F).withUnit(new String("m")));
      String minimumLengthKey = new String("MinLength");
      this.minLengthSetting = this.add(new NumberSetting<>(minimumLengthKey, 4, 1, 16)
         .withUnit(new String("m")));
      this.fillSetting = this.add(new ColorSetting(new String("Fill"), new Color(1358888960, true)));
      this.outlineSetting = this.add(new ColorSetting(new String("Outline"), new Color(-65536, true)));
      this.mergeTimer = new ElapsedTimer();
      this.pendingTunnels = new ConcurrentLinkedQueue<>();
      this.pendingChunkScans = new ConcurrentLinkedQueue<>();
      this.queuedChunkScans = ConcurrentHashMap.newKeySet();
      this.tunnels = Collections.emptyList();
      this.mergeInProgress = new AtomicBoolean(false);
      this.chunkScanner = new ChunkScanner(
         0,
         (java.util.function.BiConsumer<net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.LevelChunk>)this::enqueueChunkScan,
         (java.util.function.BiConsumer<net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.LevelChunk>)((unloadedChunkPosition, unusedChunk) -> {
            BlockableEventLoop<?> client = ClientAccess.schizoMinecraft;
            ChunkPos copiedPosition = new ChunkPos(unloadedChunkPosition.x, unloadedChunkPosition.z);
            client.executeIfPossible(() -> this.removeChunkState(copiedPosition));
         }),
         null
      );
   }

   private static class Tunnel {
      public int minX;
      public int minY;
      public int minZ;
      public int maxX;
      public int maxY;
      public int maxZ;
      public final boolean vertical;
      private AABB cachedBounds = null;

      public Tunnel(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean vertical) {
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.maxX = maxX;
         this.maxY = maxY;
         this.maxZ = maxZ;
         this.vertical = vertical;
      }

      private Tunnel copy() {
         return new Tunnel(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, this.vertical);
      }

      public AABB getBounds() {
         if (this.cachedBounds == null) {
            this.cachedBounds = new AABB(
               this.minX,
               this.minY,
               this.minZ,
               this.maxX,
               this.maxY,
               this.maxZ
            );
         }

         return this.cachedBounds;
      }

      public double distanceToCenterSquared(Vec3 position) {
         double coordinateMinimum = this.minX;
         double coordinateSpan = this.maxX
            - this.minX;

         double centerX = coordinateMinimum + coordinateSpan * 0.5D;
         coordinateMinimum = this.minY;
         coordinateSpan = this.maxY
            - this.minY;

         double centerY = coordinateMinimum + coordinateSpan * 0.5D;
         coordinateMinimum = this.minZ;
         coordinateSpan = this.maxZ
            - this.minZ;

         double centerZ = coordinateMinimum + coordinateSpan * 0.5D;
         return position.distanceToSqr(centerX, centerY, centerZ);
      }

      public boolean canMergeWith(TunnelsModule.Tunnel other) {
         if (this.vertical != other.vertical) {
            return false;
         }

         if (this.vertical) {
            if (this.minX != other.minX || this.maxX != other.maxX) {
               return false;
            }
            return this.minZ == other.minZ
               && this.maxZ == other.maxZ
               && (this.minY == other.maxY || this.maxY == other.minY);
         }

         if (this.minY != other.minY || this.maxY != other.maxY) {
            return false;
         }
         boolean touchesAlongX = (this.minX == other.maxX || this.maxX == other.minX)
            && this.minZ == other.minZ
            && this.maxZ == other.maxZ;
         return touchesAlongX
            || (this.minZ == other.maxZ || this.maxZ == other.minZ)
               && this.minX == other.minX
               && this.maxX == other.maxX;
      }

      public void merge(TunnelsModule.Tunnel other) {
         int otherMinX = other.minX;
         int currentMinX = this.minX;
         this.minX = Math.min(currentMinX, otherMinX);
         int otherMinY = other.minY;
         int currentMinY = this.minY;
         this.minY = Math.min(currentMinY, otherMinY);
         int otherMinZ = other.minZ;
         int currentMinZ = this.minZ;
         this.minZ = Math.min(currentMinZ, otherMinZ);
         int otherMaxX = other.maxX;
         int currentMaxX = this.maxX;
         this.maxX = Math.max(currentMaxX, otherMaxX);
         int otherMaxY = other.maxY;
         int currentMaxY = this.maxY;
         this.maxY = Math.max(currentMaxY, otherMaxY);
         int otherMaxZ = other.maxZ;
         int currentMaxZ = this.maxZ;
         this.maxZ = Math.max(currentMaxZ, otherMaxZ);
         this.cachedBounds = null;
      }

   }
}
