/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.render.GameOptionsOverrides;
import dev.schizo.client.settings.Setting;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public class ZoomOptionsUpdater implements ClientAccess {
   private final ZoomModule zoomModule;

   public ZoomOptionsUpdater(ZoomModule zoomModule) {
      this.zoomModule = zoomModule;
   }

   static boolean resolveSmoothCameraValue(
      boolean toggled,
      boolean captured,
      boolean savedValue,
      boolean configuredValue,
      boolean currentValue
   ) {
      return toggled ? configuredValue : currentValue;
   }

   static boolean shouldRestoreSmoothCamera(
      boolean overrideApplied,
      boolean currentValue,
      boolean lastAppliedValue
   ) {
      return overrideApplied && currentValue == lastAppliedValue;
   }

   @EventHandler
   private void onMinecraftRunTickEvent(MinecraftRunTickEvent event) {
      ZoomModule fovModule = this.zoomModule;
      int fov = fovModule.calculateFov();
      OptionInstance fovOption = ClientAccess.schizoMinecraft.options.fov();
      Integer fovValue = fov;
      GameOptionsOverrides.forceSetOptionValue(fovOption, fovValue);
      Options cameraOptions = ClientAccess.schizoMinecraft.options;
      ZoomModule toggleModule = this.zoomModule;
      Setting<Boolean> smoothCameraSetting = this.zoomModule.smoothCameraSetting;
      boolean toggled = toggleModule.isToggled();
      boolean resolvedSmoothCamera = resolveSmoothCameraValue(
         toggled,
         this.zoomModule.smoothCameraCaptured,
         this.zoomModule.savedSmoothCamera,
         smoothCameraSetting.getValue(),
         cameraOptions.smoothCamera
      );
      if (toggled) {
         cameraOptions.smoothCamera = resolvedSmoothCamera;
         this.zoomModule.lastAppliedSmoothCamera = resolvedSmoothCamera;
         this.zoomModule.smoothCameraOverrideApplied = true;
      }
      if (fov >= this.zoomModule.baselineFov) {
         ZoomModule currentToggleModule = this.zoomModule;
         if (!currentToggleModule.isToggled()) {
            EventBus bus = ClientAccess.eventBus;
            bus.unsubscribe(this);
            this.zoomModule.fovUpdateActive = false;
            this.zoomModule.smoothCameraCaptured = false;
            return;
         }
      }
      this.zoomModule.fovUpdateActive = true;
   }

}
