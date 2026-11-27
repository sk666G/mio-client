package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class ZoomLifecycleTest {
   @Test
   void smoothCameraPolicyPreservesSavedValueDuringTailAnimation() {
      assertTrue(ZoomOptionsUpdater.resolveSmoothCameraValue(false, true, true, false, false));
      assertFalse(ZoomOptionsUpdater.resolveSmoothCameraValue(false, true, false, true, true));
   }

   @Test
   void smoothCameraPolicyUsesConfiguredValueWhileEnabled() {
      assertTrue(ZoomOptionsUpdater.resolveSmoothCameraValue(true, true, false, true, false));
      assertFalse(ZoomOptionsUpdater.resolveSmoothCameraValue(true, true, true, false, true));
   }

   @Test
   void cleanupBeforeCaptureDoesNotOverwriteCurrentSmoothCameraState() {
      if (ClientAccess.schizoMinecraft.options == null) {
         ZoomModule module = new ZoomModule();
         assertDoesNotThrow(module::onDisable);
         return;
      }

      boolean original = ClientAccess.schizoMinecraft.options.smoothCamera;
      ClientAccess.schizoMinecraft.options.smoothCamera = true;
      try {
         ZoomModule module = new ZoomModule();
         assertDoesNotThrow(module::onDisable);
         assertTrue(ClientAccess.schizoMinecraft.options.smoothCamera);
      } finally {
         ClientAccess.schizoMinecraft.options.smoothCamera = original;
      }
   }
}
