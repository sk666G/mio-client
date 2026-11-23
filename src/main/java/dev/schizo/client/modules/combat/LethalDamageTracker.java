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
import dev.schizo.client.utils.combat.DamageUtils;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.ElapsedTimer;
import java.util.Iterator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LethalDamageTracker implements ClientAccess {
   private final ElapsedTimer lethalCrystalTimer = new ElapsedTimer();
   private final LethalCheckPolicy policy;

   public LethalDamageTracker(LethalCheckPolicy checkPolicy) {
      this.policy = checkPolicy;
   }

   public void update() {
      if (hasLethalCrystal()) {
         this.lethalCrystalTimer.reset();
      }
   }

   public boolean isLethalCheckEnabled() {
      return this.policy.isLethalCheckEnabled();
   }

   public boolean isCrystalLethal(Vec3 crystalPosition) {
      double squaredDistance = crystalPosition.distanceToSqr(ClientAccess.schizoMinecraft.player.position());
      if (squaredDistance > 144.0D) {
         return false;
      }

      for (int predictionStep = 4; predictionStep >= 0; predictionStep--) {
         AABB predictedPlayerBox = ClientContext.movementPredictionManager.getPredictedBoundingBox(ClientAccess.schizoMinecraft.player, predictionStep);
         LocalPlayer playerForDamageCheck = ClientAccess.schizoMinecraft.player;

         double predictedDamage = DamageUtils.getCrystalDamage(crystalPosition, playerForDamageCheck, predictedPlayerBox);
         if (predictedDamage >= EntityUtils.getPlayerHealthWithAbsorption()) {
            return true;
         }
      }

      return false;
   }

   private boolean isNearbyCrystal(Entity entity) {
      if (entity instanceof EndCrystal) {
         double squaredDistance = ClientAccess.schizoMinecraft.player.distanceToSqr(entity);
         return squaredDistance <= 144.0D;
      }

      return false;
   }

   public boolean shouldHoldTotem() {
      return isLethalCheckEnabled() && !this.lethalCrystalTimer.hasElapsed(750L);
   }

   public boolean hasLethalCrystal() {
      synchronized (ClientAccess.schizoMinecraft.level.entitiesForRendering()) {
         Iterator<Entity> entityIterator = ClientAccess.schizoMinecraft.level.entitiesForRendering().iterator();

         while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();

            if (isNearbyCrystal(entity)) {
               if (isCrystalLethal(entity.position())) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public interface LethalCheckPolicy {
      boolean isLethalCheckEnabled();
   }
}
