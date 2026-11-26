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
import dev.schizo.client.events.ClientChunkCacheReplaceWithPacketDataEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkScanner implements ClientAccess {
   private final int workerCount;
   private final BiConsumer chunkLoadCallback;
   private final BiConsumer chunkUnloadCallback;
   private final BiConsumer blockUpdateCallback;
   private volatile ExecutorService executor;
   private volatile boolean running;

   public synchronized void start() {
      if (this.running) {
         return;
      }
      this.running = true;
      if (this.workerCount > 0) {
         this.executor = createExecutor(this.workerCount);
      }
      ClientAccess.eventBus.subscribe(this);
   }

   public void scanLoadedChunks() {
      if (this.chunkLoadCallback != null) {

         Iterator chunks = WorldUtils.getLoadedChunks().iterator();

         while (chunks.hasNext()) {
            LevelChunk chunk = (LevelChunk)chunks.next();
            this.dispatch(() -> {
               this.chunkLoadCallback.accept(chunk.getPos(), chunk);
            });
         }
      }
   }

   public synchronized void restartExecutor() {
      ExecutorService previous = this.executor;
      if (previous != null) {
         previous.shutdownNow();
      }
      this.executor = this.running && this.workerCount > 0
         ? createExecutor(this.workerCount)
         : null;
   }

   public ChunkScanner(int workerCount, BiConsumer chunkLoadCallback, BiConsumer chunkUnloadCallback, BiConsumer blockUpdateCallback) {
      this.workerCount = workerCount;
      this.chunkLoadCallback = chunkLoadCallback;
      this.chunkUnloadCallback = chunkUnloadCallback;
      this.blockUpdateCallback = blockUpdateCallback;
   }

   public ChunkScanner(int workerCount, ChunkScanner.Listener listener) {
      this(
         workerCount,
         (java.util.function.BiConsumer<net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.LevelChunk>)listener::onChunkLoaded,
         (java.util.function.BiConsumer<net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.LevelChunk>)listener::onChunkUnloaded,
         (java.util.function.BiConsumer<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>)listener::onBlockUpdated
      );
   }

   public ExecutorService getExecutor() {
      return this.executor;
   }

   @EventHandler
   private void onClientChunkCacheReplaceWithPacketDataEvent(
      ClientChunkCacheReplaceWithPacketDataEvent event
   ) {
      if (this.chunkLoadCallback != null) {
         this.dispatch(
            () -> {
               BiConsumer loadCallback = this.chunkLoadCallback;
               LevelChunk chunkForPosition = event.getChunk();
               ChunkPos chunkPosition = chunkForPosition.getPos();
               loadCallback.accept(chunkPosition, event.getChunk());
            }
         );
      }
   }

   @EventHandler
   public void onPacketReceive(
      PacketEvent.Receive event
   ) {
      net.minecraft.world.level.ChunkPos extractedChunkPosition;
      if (ClientAccess.schizoMinecraft.level != null) {
         if (this.blockUpdateCallback != null) {
            if (event.getPacket() instanceof ClientboundBlockUpdatePacket blockPacket) {
               BlockPos blockPosition = blockPacket.getPos().immutable();
               BlockState blockState = blockPacket.getBlockState();
               this.dispatchPacketCallback(() -> {
                  BiConsumer updateCallback = this.blockUpdateCallback;
                  updateCallback.accept(blockPosition, blockState);
               });
               return;
            }
         }

         if (this.blockUpdateCallback != null) {
            if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
               this.dispatchPacketCallback(() -> sectionPacket.runUpdates((position, state) -> {
                  BiConsumer updateCallback = this.blockUpdateCallback;
                  updateCallback.accept(position.immutable(), state);
               }));
               return;
            }
         }

         if (this.chunkUnloadCallback != null) {
            if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket forgetPacket) {

               try {
                  extractedChunkPosition = forgetPacket.pos();
               } catch (Throwable extractionFailure) {
                  throw new MatchException(extractionFailure.toString(), extractionFailure);
               }

               ChunkPos copiedChunkPosition = new ChunkPos(extractedChunkPosition.x, extractedChunkPosition.z);
               ClientAccess.schizoMinecraft.executeIfPossible(() -> {
                  if (!this.running || ClientAccess.schizoMinecraft.level == null) {
                     return;
                  }
                  int chunkZ = copiedChunkPosition.z;
                  int chunkX = copiedChunkPosition.x;
                  ClientLevel level = ClientAccess.schizoMinecraft.level;
                  LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                  this.dispatch(() -> this.chunkUnloadCallback.accept(copiedChunkPosition, chunk));
               });
            }
         }
      }
   }

   private void dispatchPacketCallback(Runnable action) {
      if (!this.running) {
         return;
      }
      if (this.workerCount == 0) {
         ClientAccess.schizoMinecraft.executeIfPossible(() -> this.dispatch(action));
      } else {
         this.dispatch(action);
      }
   }

   private void dispatch(Runnable action) {
      if (!this.running) {
         return;
      }
      if (this.workerCount == 0) {
         action.run();
         return;
      }

      ExecutorService target = this.executor;
      if (target == null || target.isShutdown()) {
         return;
      }
      try {
         target.execute(action);
      } catch (RejectedExecutionException ignoredDuringShutdown) {
         // A concurrent stop/restart owns cancellation of work that has not started yet.
      }
   }

   public synchronized void stop() {
      if (!this.running) {
         return;
      }
      this.running = false;
      ClientAccess.eventBus.unsubscribe(this);
      ExecutorService previous = this.executor;
      this.executor = null;
      if (previous != null) {
         previous.shutdownNow();
      }
   }

   private static ExecutorService createExecutor(int requestedWorkers) {
      return Executors.newFixedThreadPool(requestedWorkers);
   }

   public interface Listener {
      void onChunkLoaded(ChunkPos chunkPosition, LevelChunk chunk);

      void onChunkUnloaded(ChunkPos chunkPosition, LevelChunk chunk);

      void onBlockUpdated(BlockPos position, BlockState state);
   }
}
