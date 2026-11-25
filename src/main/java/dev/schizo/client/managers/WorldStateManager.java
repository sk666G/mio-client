/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.combat.PendingPlacement;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.world.BlockStateSnapshot;
import dev.schizo.client.utils.world.WorldUtils;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.BlockCollisionShapeEvent;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.SelectedSlotChangeEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.settings.options.DimensionType;
import dev.schizo.mixin.BlockBehaviourAccessor;
import dev.schizo.mixin.ClientLevelAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WorldStateManager implements ClientAccess {
   private static final AntiCheatModule antiCheatModule;

   private final List<BlockEntity> blockEntities = Collections.synchronizedList(new ArrayList<>());

   private final Map<BlockPos, PendingPlacement> pendingPlacements = Collections.synchronizedMap(new HashMap<>());

   private final Set<BlockPos> confirmedPlacements = Collections.synchronizedSet(new HashSet<>());

   private final Set<BlockPos> currentTickPlacements = Collections.synchronizedSet(new HashSet<>());

   private final Map<BlockPos, BlockStateSnapshot> rollbackBlocks = Collections.synchronizedMap(new HashMap<>());

   private final Map<BlockPos, GlassUpdateWindow> glassUpdateWindows = new ConcurrentHashMap<>();
   @Deprecated

   private BlockPos unconfirmedBreak = null;

   private final ElapsedTimer unconfirmedBreakTimer = new ElapsedTimer();

   private BlockPos extraBreak;

   private final ElapsedTimer extraBreakTimer = new ElapsedTimer();

   private DimensionType dimension = DimensionType.OVERWORLD;

   private ItemStack mainHandSnapshot;

   private ItemStack offHandSnapshot;

   private int chunkRadius;

   private volatile long packetGeneration;

   public DimensionType getDimension() {
      return this.dimension;
   }

   public int nextPredictionSequence() {
      BlockStatePredictionHandler handler = ((ClientLevelAccessor)ClientAccess.schizoMinecraft.level)
         .getPendingUpdateManager();
      BlockStatePredictionHandler prediction = handler.startPredicting();
      prediction.close();
      return prediction.currentSequence();
   }

   public int getChunkRadius() {
      return this.chunkRadius;
   }

   public List<BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public Map<BlockPos, PendingPlacement> getPendingPlacements() {
      return this.pendingPlacements;
   }

   public BlockPos getUnconfirmedBreak() {
      return this.unconfirmedBreak;
   }

   public BlockPos getExtraBreak() {
      return this.extraBreak;
   }

   private void refreshHandSnapshots() {
      this.mainHandSnapshot = ClientAccess.schizoMinecraft.player.getMainHandItem().copy();
      this.offHandSnapshot = ClientAccess.schizoMinecraft.player.getOffhandItem().copy();
   }

   public boolean hasActiveGlassUpdate(BlockPos position) {
      GlassUpdateWindow window = this.glassUpdateWindows.get(position);
      return window != null && window.isActive();
   }

   public boolean hasPendingPlacement(BlockPos position) {
      return this.pendingPlacements.containsKey(position);
   }

   public WorldStateManager() {
      ClientAccess.eventBus.subscribe(this);
   }

   static {
      antiCheatModule = (AntiCheatModule)ClientContext.getModule(AntiCheatModule.class);
   }

   public boolean isPlacementOccupied(BlockPos position) {
      if (this.hasActiveGlassUpdate(position) && RotationManager.isMovementSyncEnabled()) {
         return false;
      }
      if ((antiCheatModule.assumeBlockBreakingSetting.getValue()) && this.rollbackBlocks.containsKey(position)) {
         return false;
      }
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState state = level.getBlockState(position);
      return !state.canBeReplaced()
         || this.currentTickPlacements.contains(position);
   }

   public void destroyBlockPredicted(BlockPos position) {
      this.saveBlockForRollback(position);
      MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
      gameMode.destroyBlock(position);
   }

   public void markUnconfirmedBreak(BlockPos position) {
      this.unconfirmedBreak = position;
      this.unconfirmedBreakTimer.reset();
   }

   public void markExtraBreak(BlockPos position) {
      this.extraBreak = position;
      this.extraBreakTimer.reset();
   }

   @EventHandler(priority = -200)
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.currentTickPlacements.clear();
      if (this.unconfirmedBreakTimer.hasElapsed(150L)) {
         this.unconfirmedBreak = null;
      }
      if (this.extraBreakTimer.hasElapsed(150L)) {
         this.extraBreak = null;
      }
      this.confirmedPlacements.removeIf(position -> {
         LocalPlayer player = ClientAccess.schizoMinecraft.player;
         BlockPos playerPosition = player.blockPosition();
         return !position.closerThan(playerPosition, 128.0D);
      });
      synchronized (this.pendingPlacements) {
         List<BlockPos> expiredPlacements = new ArrayList<>();
         Iterator<Entry<BlockPos, PendingPlacement>> placementsIterator = this.pendingPlacements.entrySet().iterator();
         while (placementsIterator.hasNext()) {
            Entry<BlockPos, PendingPlacement> placementEntry = placementsIterator.next();
            long now = System.currentTimeMillis();
            long createdAtMillis = placementEntry.getValue().createdAtMillis();
            if (now > createdAtMillis + 250L) {
               expiredPlacements.add(placementEntry.getKey());
            }
         }
         for (BlockPos expiredPosition : expiredPlacements) {
            this.pendingPlacements.remove(expiredPosition);
         }
      }
      synchronized (this.getBlockEntities()) {
         this.blockEntities.clear();
         this.blockEntities.addAll(WorldUtils.getLoadedBlockEntities());
         this.blockEntities.removeIf(Objects::isNull);
      }
      synchronized (this.rollbackBlocks) {
         this.rollbackBlocks.forEach((position, snapshot) -> {
            if (snapshot.hasExpired()) {
               ClientLevel level = ClientAccess.schizoMinecraft.level;
               BlockPos savedPosition = snapshot.position();
               BlockState savedState = snapshot.state();
               level.setBlockAndUpdate(savedPosition, savedState);
            }
         });
         this.rollbackBlocks.entrySet().removeIf(entry -> {
            BlockStateSnapshot snapshot = entry.getValue();
            return snapshot.hasExpired();
         });
      }
      this.glassUpdateWindows.values().removeIf(GlassUpdateWindow::hasExpired);
   }

   @EventHandler
   private void onConnectionSendPacketEvent(
      PacketEvent.Send event
   ) {
      if (!(event.getPacket() instanceof ServerboundUseItemOnPacket packet)) {
         return;
      }
      RotationManager rotationManager = ClientContext.rotationManager;
      if (rotationManager.isExecutingAction()) {
         return;
      }
      InteractionHand hand = packet.getHand();
      ItemStack stack = this.resolveHandStack(hand);
      if (!(stack.getItem() instanceof BlockItem item)) {
         return;
      }
      BlockHitResult hit = packet.getHitResult();
      BlockPos placementPosition = hit.getBlockPos();
      Direction face = hit.getDirection();
      BlockPos adjacentPosition = placementPosition.relative(face);
      Block block = item.getBlock();
      boolean replacesClickedBlock = true;
      if (!WorldUtils.isReplaceable(placementPosition)) {
         placementPosition = adjacentPosition;
         replacesClickedBlock = false;
      }
      if (!WorldUtils.isReplaceable(placementPosition)) {
         if (replacesClickedBlock) {
            return;
         }
         if (block != WorldUtils.getBlock(placementPosition)) {
            return;
         }
      }
      this.pendingPlacements.put(placementPosition, new PendingPlacement(System.currentTimeMillis(), block));
      if (!replacesClickedBlock) {
         this.currentTickPlacements.add(placementPosition);
      }
   }

   @EventHandler
   private void onHandSnapshotEvent(SelectedSlotChangeEvent event) {
      this.refreshHandSnapshots();
   }

   @EventHandler
   private void onCollisionShapeEvent(BlockCollisionShapeEvent event) {
      if (event.getPos() == null || ClientAccess.schizoMinecraft.player == null) {
         return;
      }
      Map<BlockPos, PendingPlacement> placements = this.pendingPlacements;
      if (placements.containsKey(event.getPos())) {
         placements = this.pendingPlacements;
         PendingPlacement placement = placements.get(event.getPos());
         if (placement == null) {
            return;
         }
         if (placement.block() == null) {
            return;
         }
         AABB blockBounds = new AABB(event.getPos());
         LocalPlayer player = ClientAccess.schizoMinecraft.player;
         if (player.getBoundingBox().intersects(blockBounds)) {
            return;
         }
         if (!((BlockBehaviourAccessor)placement.block())
            .isCollidable()) {
            return;
         }
         ClientLevel entityLevel = ClientAccess.schizoMinecraft.level;
         Class<Entity> entityClass = Entity.class;
         Predicate<Entity> collisionEntityFilter = entity -> !(entity instanceof ThrownExperienceBottle)
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof Arrow)
            && !(entity instanceof ThrownEnderpearl);
         if (!entityLevel.getEntitiesOfClass(entityClass, blockBounds, collisionEntityFilter).isEmpty()) {
            return;
         }
         Block block = placement.block();
         BlockState defaultState = block.defaultBlockState();
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         BlockPos shapePosition = event.getPos();
         VoxelShape shape = defaultState.getShape(level, shapePosition);
         event.setShape(shape);
      }
      if (RotationManager.isMovementSyncEnabled()) {
         if (ClientContext.serverStateManager.getSetbackTimer().hasElapsed(300L)) {
            if (event.getPos().equals(this.unconfirmedBreak)) {
               VoxelShape emptyShape = Shapes.empty();
               event.setShape(emptyShape);
            }
         }
      }
   }

   @EventHandler
   private void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      this.resetSessionState();
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.resetSessionState();
   }

   void resetSessionState() {
      this.packetGeneration++;
      this.pendingPlacements.clear();
      this.confirmedPlacements.clear();
      this.blockEntities.clear();
      this.rollbackBlocks.clear();
      this.currentTickPlacements.clear();
      this.glassUpdateWindows.clear();
      this.unconfirmedBreak = null;
      this.extraBreak = null;
      this.unconfirmedBreakTimer.clear();
      this.extraBreakTimer.clear();
      this.dimension = DimensionType.OVERWORLD;
      this.mainHandSnapshot = null;
      this.offHandSnapshot = null;
      this.chunkRadius = 0;
   }

   @EventHandler(priority = 200)
   private void onLocalPlayerPreTick(LocalPlayerPreTickEvent event) {
      this.currentTickPlacements.clear();
      this.dimension = WorldUtils.getDimension();
      this.refreshHandSnapshots();
   }

   private void trackGlassUpdate(BlockPos position, BlockState state) {
      if (!BlockInteractionUtils.isGlass(state)) {
         return;
      }
      long now = System.currentTimeMillis();
      int latencyDurationMillis = ClientContext.serverStateManager.getLatencyTicks() * (50) + (50);
      long activeDurationMillis = Math.min(250, latencyDurationMillis);
      Map<BlockPos, GlassUpdateWindow> windows = this.glassUpdateWindows;
      BlockPos immutableWindowPosition = position.immutable();
      long activeUntilMillis = now + activeDurationMillis;
      windows.putIfAbsent(immutableWindowPosition, new WorldStateManager.GlassUpdateWindow(activeUntilMillis, now + 10000L));
   }

   private ItemStack resolveHandStack(InteractionHand hand) {
      ItemStack previous = hand == InteractionHand.MAIN_HAND ? this.mainHandSnapshot : this.offHandSnapshot;
      this.refreshHandSnapshots();
      ItemStack updated = hand == InteractionHand.MAIN_HAND ? this.mainHandSnapshot : this.offHandSnapshot;
      return updated.isEmpty() ? previous : updated;
   }

   public boolean hasKnownPlacement(BlockPos position) {
      return this.confirmedPlacements.contains(position)
         || this.pendingPlacements.containsKey(position);
   }

   private void dispatchPacketUpdate(long expectedGeneration, Runnable action) {
      ClientAccess.schizoMinecraft.executeIfPossible(() -> {
         if (this.packetGeneration == expectedGeneration) {
            action.run();
         }
      });
   }

   private void applyBlockUpdate(BlockPos position, BlockState state) {
      this.rollbackBlocks.remove(position);
      if (state.isAir()) {
         this.confirmedPlacements.remove(position);
         this.pendingPlacements.remove(position);
         this.glassUpdateWindows.remove(position);
      } else if (this.pendingPlacements.remove(position) != null) {
         this.confirmedPlacements.add(position);
      }
      this.trackGlassUpdate(position, state);
   }

   private void applySectionUpdate(BlockPos position, BlockState state) {
      this.rollbackBlocks.remove(position);
      if (state.isAir()) {
         this.glassUpdateWindows.remove(position);
      } else {
         this.trackGlassUpdate(position, state);
      }
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      long expectedGeneration = this.packetGeneration;
      if (event.getPacket() instanceof ClientboundBlockUpdatePacket blockUpdate) {
         BlockPos position = blockUpdate.getPos().immutable();
         BlockState state = blockUpdate.getBlockState();
         this.dispatchPacketUpdate(expectedGeneration, () -> this.applyBlockUpdate(position, state));
      }

      if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
         ArrayList<PacketBlockUpdate> updates = new ArrayList<>();
         sectionUpdate.runUpdates((position, state) ->
            updates.add(new PacketBlockUpdate(position.immutable(), state))
         );
         if (!updates.isEmpty()) {
            List<PacketBlockUpdate> updateSnapshot = List.copyOf(updates);
            this.dispatchPacketUpdate(expectedGeneration, () -> {
               for (PacketBlockUpdate update : updateSnapshot) {
                  this.applySectionUpdate(update.position(), update.state());
               }
            });
         }
      }

      if (event.getPacket() instanceof ClientboundLoginPacket login) {
         int radius = login.chunkRadius();
         this.dispatchPacketUpdate(expectedGeneration, () -> this.chunkRadius = radius);
      }

      if (event.getPacket() instanceof ClientboundSetChunkCacheRadiusPacket radiusUpdate) {
         int radius = radiusUpdate.getRadius();
         this.dispatchPacketUpdate(expectedGeneration, () -> this.chunkRadius = radius);
      }
   }

   public void saveBlockForRollback(BlockPos position) {
      if (!this.rollbackBlocks.containsKey(position)) {
         this.rollbackBlocks.put(position, new BlockStateSnapshot(
            BlockStateSnapshot.captureState(position), position, System.currentTimeMillis()
         ));
      }
   }

   private record PacketBlockUpdate(BlockPos position, BlockState state) {
   }

   private record GlassUpdateWindow(
      long activeUntilMillis,
      long expiresAtMillis
   ) {
      public boolean isActive() {
         return System.currentTimeMillis() < this.activeUntilMillis;
      }
      public boolean hasExpired() {
         return System.currentTimeMillis() >= this.expiresAtMillis;
      }
   }
}
