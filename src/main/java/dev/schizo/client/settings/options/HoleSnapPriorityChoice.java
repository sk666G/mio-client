/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.world.HoleInfo;
import dev.schizo.client.utils.math.Vec3Utils;
import dev.schizo.client.settings.NamedEntry;

import java.util.Comparator;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public enum HoleSnapPriorityChoice implements ClientAccess, NamedEntry {
   DISTANCE("Distance", Comparator.comparing((HoleInfo hole) -> {
      BlockPos block = hole.pos();
      Vec3 center = block.getCenter();
      LocalPlayer capturedPlayer = ClientAccess.schizoMinecraft.player;
      Vec3 playerPosition = capturedPlayer.position();
      return Vec3Utils.horizontalDistance(center, playerPosition);
   })),
   CROSSHAIR("Crosshair", Comparator.comparing((HoleInfo hole) -> {
      GameRenderer gameRenderer = ClientAccess.schizoMinecraft.gameRenderer;
      Camera camera = gameRenderer.getMainCamera();
      float cameraYaw = camera.yaw();

      BlockPos block = hole.pos();
      Vec3 center = block.getCenter();

      float targetYaw = RotationManager.getRotationsToPosition(center)[0];
      float yawDifference = Mth.degreesDifferenceAbs(cameraYaw, targetYaw);
      return yawDifference;
   }));

   private final String displayName;
   private final Comparator<HoleInfo> comparator;

   @Override
   public String getName() {
      return this.displayName;
   }

   HoleSnapPriorityChoice(String displayName, Comparator<HoleInfo> comparator) {
      this.displayName = displayName;
      this.comparator = comparator;
   }

   public Comparator<HoleInfo> getComparator() {
      return this.comparator;
   }
}
