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
import dev.schizo.client.utils.world.HoleInfo;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.SpeedMineRebreakChoice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlainHoleFillPolicy implements ClientAccess {
   private static SpeedMineModule speedMineModule;
   protected final HoleFillModule holeFillModule;

   public PlainHoleFillPolicy(HoleFillModule holeFillModule) {
      this.holeFillModule = holeFillModule;
   }
   public boolean shouldFill(HoleInfo hole) {
      if (speedMineModule.isToggled()) {
         Setting<SpeedMineRebreakChoice> rebreakSettingRef = speedMineModule.rebreakSetting;

         if (rebreakSettingRef.getValue() == SpeedMineRebreakChoice.INSTANT) {
            if (speedMineModule.getMiningPos() != null) {
               BlockPos miningPos = speedMineModule.getMiningPos();
               Vec3 miningCenter = miningPos.getCenter();
               AABB miningBoundsSnapshot = hole.bounds();
               if (miningBoundsSnapshot.contains(miningCenter)) {
                  return false;
               }
            }
         }
      }

      AABB widthBounds = hole.bounds();
      if (widthBounds.getXsize() != 1.0D || hole.bounds().getZsize() != 1.0D) {
         if (!this.holeFillModule.doublesSetting.getValue()) {
            return false;
         }
      }

      Vec3 eyeSnapshot = ClientAccess.schizoMinecraft.player.getEyePosition();
      BlockPos rangePosition = hole.pos();
      Vec3 holeCenter = rangePosition.getCenter();
      double distance = eyeSnapshot.distanceTo(holeCenter);

      Float configuredRange = this.holeFillModule.rangeSetting.getValue();
      if (distance <= configuredRange.floatValue()) {
         double eyeY = ClientAccess.schizoMinecraft.player.getEyePosition().y;
         BlockPos verticalPosition = hole.pos();
         double verticalDifference = eyeY - verticalPosition.getCenter().y;
         double absoluteVerticalDifference = Math.abs(verticalDifference);

         Float configuredVerticalRange = this.holeFillModule.verticalRangeSetting.getValue();
         return absoluteVerticalDifference <= configuredVerticalRange.floatValue();
      }
      return false;
   }

   public static void connectSpeedMineModule(ModuleRegistry registry) {
      speedMineModule = (SpeedMineModule)registry.getModule(
         SpeedMineModule.class
      );
   }
}
