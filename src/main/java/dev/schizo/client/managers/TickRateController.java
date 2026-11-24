/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.modules.Module;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.Mth;

public class TickRateController implements ClientAccess {
   private final Requests requests = new Requests();

   public TickRateController() {
      ClientAccess.eventBus.subscribe(this);
   }

   @EventHandler
   private void onModuleToggle(ModuleToggleEvent event) {
      Module module = event.getModule();
      if (!module.isToggled()) {
         Module moduleToRelease = event.getModule();
         this.releaseMultiplier(moduleToRelease);
      }
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.requests.clear();
   }

   public void requestMultiplier(Module module, float requestedMultiplier) {
      this.requests.request(module, requestedMultiplier);
   }

   public float getMultiplier() {
      return this.requests.getMultiplier();
   }

   public void releaseMultiplier(Module module) {
      this.requests.release(module);
   }

   /**
    * Keeps the most recently requested active multiplier while allowing older
    * requests to resume when a newer owner releases its request.
    */
   static final class Requests {
      private final Map<Object, Float> activeRequests = new LinkedHashMap<>();
      private float multiplier = 1.0F;

      synchronized void request(Object owner, float requestedMultiplier) {
         this.activeRequests.remove(owner);
         this.activeRequests.put(owner, requestedMultiplier);
         this.multiplier = requestedMultiplier;
      }

      synchronized void release(Object owner) {
         this.activeRequests.remove(owner);
         this.multiplier = 1.0F;

         for (Float requestedMultiplier : this.activeRequests.values()) {
            this.multiplier = requestedMultiplier;
         }
      }

      synchronized void clear() {
         this.activeRequests.clear();
         this.multiplier = 1.0F;
      }

      synchronized float getMultiplier() {
         return this.multiplier;
      }
   }

   public static class PlayerTicks extends TickRateController {
      private float fractionalTicks;

      @EventHandler(priority = -200)
      private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
         float rate = this.getMultiplier();
         if (rate <= 1.0F) {
            return;
         }

         this.fractionalTicks += rate - (int)rate;
         for (int extraTicks = 0;
            extraTicks < Math.floor(rate) + Math.floor(this.fractionalTicks) - 1.0D;
            extraTicks++
         ) {
            PlayerTickExecutor.runExtraTick();
         }

         float fractionSnapshot = this.fractionalTicks;
         if (fractionSnapshot >= 1.0F) {
            float fractionToReduce = this.fractionalTicks;
            float reducedFraction = fractionToReduce - 1.0F;
            float maximumFraction = 1.0F;
            this.fractionalTicks = Mth.clamp(reducedFraction, 0.0F, maximumFraction);
         }
      }
   }
}
