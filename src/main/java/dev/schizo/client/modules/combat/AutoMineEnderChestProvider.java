/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.inventory.AutoMineProviderBase;
import dev.schizo.client.utils.inventory.AutoMineCandidates;
import dev.schizo.client.settings.Setting;

import java.util.Iterator;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.Vec3;

public final class AutoMineEnderChestProvider extends AutoMineProviderBase {
   public AutoMineEnderChestProvider(AutoMineModule module) {
      super(module);
   }

   @Override
   public void collectCandidates(AutoMineCandidates candidates) {
      BlockPos selectedPosition = null;

      Iterator<BlockEntity> blockEntities = ClientContext.worldStateManager.getBlockEntities().iterator();

      while (blockEntities.hasNext()) {
         BlockEntity blockEntity = blockEntities.next();

         Vec3 eyeSnapshot = ClientAccess.schizoMinecraft.player.getEyePosition();

         BlockPos blockPosition = blockEntity.getBlockPos();
         Vec3 blockCenter = blockPosition.getCenter();
         double distance = eyeSnapshot.distanceTo(blockCenter);
         Setting<Float> rangeSetting = this.autoMine.rangeSetting;

         Float range = rangeSetting.getValue();
         if (!(distance > range.floatValue()) && blockEntity instanceof EnderChestBlockEntity) {
            BlockPos validityPosition = blockEntity.getBlockPos();

            if (this.canMinePosition(validityPosition)) {
               if (selectedPosition != null) {
                  BlockPos miningPositionSnapshot = blockEntity.getBlockPos();
                  if (!this.isAlreadyMining(miningPositionSnapshot)) {
                     continue;
                  }
               }

               selectedPosition = blockEntity.getBlockPos();
            }
         }
      }

      BlockPos selectedSnapshot = selectedPosition;
      if (selectedSnapshot != null) {
         Consumer<AutoMineCandidates> update = output -> output.setPrimaryPosition(selectedSnapshot);
         candidates.offer(5, update);
      }
   }

   @Override
   public boolean isEnabled() {
      Setting<Boolean> enabledSetting = this.autoMine.enderChestsSetting;

      return enabledSetting.getValue();
   }
}
