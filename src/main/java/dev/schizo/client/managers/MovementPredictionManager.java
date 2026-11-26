/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.movement.PlayerMotionPredictor;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.ClientLevelRemoveEntityEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.MinecraftRunTickEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MovementPredictionManager implements ClientAccess {
   private final Map<Integer, PlayerMotionPredictor> playerPredictions = Collections.synchronizedMap(new HashMap<>());
   private final ElapsedTimer predictionTimer = new ElapsedTimer();
   private Vec3 previousPlayerPosition;
   private LocalPlayer trackedLocalPlayer;

   public Vec3 getAdjustedLocalMotion() {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player == null
         || player != this.trackedLocalPlayer
         || this.previousPlayerPosition == null) {
         return Vec3.ZERO;
      }
      Vec3 motion = player.position().subtract(this.previousPlayerPosition);
      if (player.isSprinting() && !player.isFallFlying()) {
         motion = motion.multiply((double)0.9F, 1.0D, (double)0.9F);
      }
      return motion;
   }

   public Vec3 getPredictedEyePosition() {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      return player == null
         ? Vec3.ZERO
         : player.getEyePosition().add(this.getAdjustedLocalMotion());
   }

   public MovementPredictionManager() {
      ClientAccess.eventBus.subscribe(this);
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player != this.trackedLocalPlayer) {
         this.trackedLocalPlayer = player;
         this.playerPredictions.clear();
      }
      this.previousPlayerPosition = new Vec3(
         player.xo,
         player.yo,
         player.zo
      );
   }

   @EventHandler
   private void onMinecraftRunTickEvent(MinecraftRunTickEvent event) {
      if (this.isWorldUnavailable() || !this.predictionTimer.hasElapsedAndReset(25L)) {
         return;
      }

      for (AbstractClientPlayer player : ClientAccess.schizoMinecraft.level.players()) {
         this.playerPredictions.computeIfAbsent(player.getId(), ignored -> new PlayerMotionPredictor(player))
            .updatePredictions();
      }
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.playerPredictions.clear();
      this.previousPlayerPosition = null;
      this.trackedLocalPlayer = null;
      this.predictionTimer.clear();
   }

   public synchronized AABB getPredictedBoundingBox(Player player, int tickIndex) {
      PlayerMotionPredictor prediction = this.playerPredictions.get(player.getId());
      if (prediction == null) {
         return player.getBoundingBox();
      }
      List<AABB> positions = prediction.getPredictedBoxes();
      return positions.isEmpty() ? player.getBoundingBox() : positions.get(Math.min(tickIndex, positions.size() - 1));
   }

   @EventHandler
   private void onClientLevelRemoveEntityEvent(ClientLevelRemoveEntityEvent event) {
      synchronized (this.playerPredictions) {
         this.playerPredictions.remove(event.getId());
      }
   }

}
