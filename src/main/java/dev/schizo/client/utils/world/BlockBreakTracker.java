/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.world;

import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.managers.WorldStateManager;
import dev.schizo.client.utils.inventory.EnchantmentUtils;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.SpeedMineRebreakChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBreakTracker implements ClientAccess {
   private static SpeedMineModule speedMine;
   private static ItemStack miningPickaxe;
   private final List<BlockBreakEntry> entries = Collections.synchronizedList(new ArrayList<>());
   private volatile long sessionGeneration;

   private static ItemStack getMiningPickaxe() {
      if (miningPickaxe == null) {
         miningPickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
         ItemStack pickaxe = miningPickaxe;
         ResourceKey efficiency = Enchantments.EFFICIENCY;
         Holder enchantment = EnchantmentUtils.getHolder(efficiency);

         pickaxe.enchant(enchantment, 5);
      }

      return miningPickaxe;
   }

   public List<BlockBreakEntry> getEntries() {
      return this.entries;
   }

   private void onBlockRemoved(BlockPos position) {
      Iterator<BlockBreakEntry> iterator = this.entries.iterator();
      while (iterator.hasNext()) {
         BlockBreakEntry entry = iterator.next();
         if (position.equals(entry.getPosition())) {
            Setting<SpeedMineRebreakChoice> rebreakSetting = speedMine.rebreakSetting;
            if (rebreakSetting.getValue() != SpeedMineRebreakChoice.INSTANT) {
               entry.setProgress(0.0F);
               entry.setAirTicks(0);
            }
            entry.markBroken();
            entry.getProgressTimer().reset();
         }
      }
   }

   private void dispatchSessionUpdate(long expectedGeneration, Runnable action) {
      ClientAccess.schizoMinecraft.executeIfPossible(() -> {
         if (this.sessionGeneration == expectedGeneration) {
            action.run();
         }
      });
   }

   private void resetSessionState() {
      this.sessionGeneration++;
      this.entries.clear();
   }

   long sessionGeneration() {
      return this.sessionGeneration;
   }

   public BlockBreakTracker() {
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   static {

      speedMine = ClientContext.getModule(SpeedMineModule.class);
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.entries.removeIf(BlockBreakEntry::isExpired);
      Iterator<BlockBreakEntry> iterator = this.entries.iterator();

      while (iterator.hasNext()) {
         BlockBreakEntry entry = iterator.next();
         ClientLevel levelSnapshot = ClientAccess.schizoMinecraft.level;

         BlockPos statePosition = entry.getPosition();

         BlockState blockState = levelSnapshot.getBlockState(statePosition);

         ItemStack pickaxeSnapshot = getMiningPickaxe();
         float baseProgress = (float)WorldUtils.getBreakProgress(pickaxeSnapshot, blockState, true);
         ServerStateManager serverStateSnapshot = ClientContext.serverStateManager;

         float progressDelta = baseProgress * serverStateSnapshot.getTickRateScale();
         SpeedMineModule speedMineSnapshot = speedMine;
         if (speedMineSnapshot.isToggled() && !entry.isExtraBreak()) {
            Setting<Float> damageSetting = speedMine.damageSetting;
            Float damageScale = damageSetting.getValue();
            progressDelta /= damageScale;
         }

         BlockPos miningPositionSnapshot = entry.getPosition();

         if (WorldUtils.canMineBlock(miningPositionSnapshot)) {
            int airTicksSnapshot = entry.getAirTicks();
            progressDelta *= Math.max(airTicksSnapshot, 1);
            entry.setAirTicks(0);
         } else {
            progressDelta = 0.0F;

            int nextAirTicks = entry.getAirTicks() + 1;

            entry.setAirTicks(nextAirTicks);

            entry.resetInactivityTimer();
         }

         float previousProgress = entry.getProgress();
         float nextProgress = previousProgress + progressDelta;

         entry.setProgress(nextProgress);
         if (Math.min((double)nextProgress, 1.0D) > previousProgress) {
            ElapsedTimer progressTimerSnapshot = entry.getProgressTimer();

            progressTimerSnapshot.reset();
         }

         if (!(nextProgress >= 0.8D)) {
            continue;
         }
         if (entry.isBroken()) {
            continue;
         }
         ElapsedTimer rollbackTimerSnapshot = entry.getProgressTimer();

         if (rollbackTimerSnapshot.hasElapsed(150L)) {
            continue;
         }
         WorldStateManager worldStateSnapshot = ClientContext.worldStateManager;

         BlockPos rollbackPositionSnapshot = entry.getPosition();

         worldStateSnapshot.saveBlockForRollback(rollbackPositionSnapshot);
      }
   }

   private void trackBlockBreaking(BlockPos position, int entityId) {
      boolean alreadyTracked = false;
      boolean hasExtraBreak = false;
      Iterator<BlockBreakEntry> iterator = this.entries.iterator();

      while (iterator.hasNext()) {
         BlockBreakEntry entry = iterator.next();

         if (entry.getEntityId() == entityId) {
            if (entry.getPosition().equals(position)) {
               alreadyTracked = true;
            }

            if (entry.isExtraBreak()) {
               hasExtraBreak = true;
            }
         }
      }

      if (alreadyTracked) {
         return;
      }
      boolean extraBreak = false;
      if (!hasExtraBreak) {
         Setting<Boolean> extraBreakSetting = speedMine.extraBreakSetting;
         Boolean extraBreakEnabled = extraBreakSetting.getValue();
         extraBreak = extraBreakEnabled;
      }

      this.entries.removeIf(existingEntry -> {
         if (existingEntry.isExtraBreak()) {
            return false;
         }
         return existingEntry.getPosition().equals(position) || existingEntry.getEntityId() == entityId;
      });

      if (WorldUtils.canMineBlock(position)) {
         this.entries.add(new BlockBreakEntry(entityId, position, extraBreak));
      }
   }

   @EventHandler
   void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      this.resetSessionState();
   }

   @EventHandler
   void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.resetSessionState();
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      long expectedGeneration = this.sessionGeneration;
      if (event.getPacket() instanceof ClientboundBlockDestructionPacket destructionPacket) {
         int progress = destructionPacket.getProgress();
         int breakerId = destructionPacket.getId();
         BlockPos position = destructionPacket.getPos().immutable();
         this.dispatchSessionUpdate(expectedGeneration, () -> {
            if (progress >= 0 && progress <= 10) {
               this.trackBlockBreaking(position, breakerId);
            }
         });
      }

      if (event.getPacket() instanceof ClientboundBlockUpdatePacket updatePacket
         && updatePacket.getBlockState().isAir()) {
         BlockPos removedPosition = updatePacket.getPos().immutable();
         this.dispatchSessionUpdate(expectedGeneration, () -> this.onBlockRemoved(removedPosition));
      }

      if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
         ArrayList<BlockPos> removedPositions = new ArrayList<>();
         sectionPacket.runUpdates((position, updatedState) -> {
            if (updatedState.isAir()) {
               removedPositions.add(position.immutable());
            }
         });
         if (!removedPositions.isEmpty()) {
            List<BlockPos> removedSnapshot = List.copyOf(removedPositions);
            this.dispatchSessionUpdate(expectedGeneration, () -> {
               for (BlockPos position : removedSnapshot) {
                  this.onBlockRemoved(position);
               }
            });
         }
      }
   }

   public float getBreakProgress(BlockPos position) {
      if (!WorldUtils.canMineBlock(position)) {
         return 0.0F;
      }

      return this.entries.stream()
         .filter(entry -> {
            return entry.getPosition().equals(position);
         })
         .map(BlockBreakEntry::getProgress)
         .findAny().orElse(0.0F);
   }

   public static void bindModules(ModuleRegistry modules) {
      speedMine = (SpeedMineModule)modules.getModule(
         SpeedMineModule.class
      );
   }
}
