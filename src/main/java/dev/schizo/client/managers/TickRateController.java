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
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.modules.Module;
import net.minecraft.util.Mth;

public class TickRateController implements ClientAccess {
   private final OwnedFloatRequests<Module> multiplierRequests = new OwnedFloatRequests<>(1.0F);

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

   public void requestMultiplier(Module module, float requestedMultiplier) {
      this.multiplierRequests.request(module, requestedMultiplier);
   }

   public float getMultiplier() {
      return this.multiplierRequests.get();
   }

   public void releaseMultiplier(Module module) {
      this.multiplierRequests.release(module);
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.multiplierRequests.clear();
   }

   public static class PlayerTicks extends TickRateController {
      private float fractionalTicks;

      @EventHandler
      private void onMinecraftDisconnectPlayerTicks(MinecraftDisconnectEvent event) {
         this.fractionalTicks = 0.0F;
      }

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
