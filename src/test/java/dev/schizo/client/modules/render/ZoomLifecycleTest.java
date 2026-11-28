package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class ZoomLifecycleTest {
   @Test
   void smoothCameraTailPreservesCurrentValueAfterOwnershipRelease() {
      assertFalse(ZoomOptionsUpdater.resolveSmoothCameraValue(false, true, true, false, false));
      assertTrue(ZoomOptionsUpdater.resolveSmoothCameraValue(false, true, false, true, true));
   }

   @Test
   void smoothCameraPolicyUsesConfiguredValueWhileEnabled() {
      assertTrue(ZoomOptionsUpdater.resolveSmoothCameraValue(true, true, false, true, false));
      assertFalse(ZoomOptionsUpdater.resolveSmoothCameraValue(true, true, true, false, true));
   }


   @Test
   void smoothCameraRestoreRequiresZoomToStillOwnCurrentValue() {
      assertTrue(ZoomOptionsUpdater.shouldRestoreSmoothCamera(true, true, true));
      assertTrue(ZoomOptionsUpdater.shouldRestoreSmoothCamera(true, false, false));
      assertFalse(ZoomOptionsUpdater.shouldRestoreSmoothCamera(false, true, true));
      assertFalse(ZoomOptionsUpdater.shouldRestoreSmoothCamera(true, false, true));
      assertFalse(ZoomOptionsUpdater.shouldRestoreSmoothCamera(true, true, false));
   }



   @Test
   void fovRestoreRequiresZoomToStillOwnCurrentValue() {
      assertTrue(ZoomOptionsUpdater.shouldRestoreFov(true, 30, 30));
      assertFalse(ZoomOptionsUpdater.shouldRestoreFov(false, 30, 30));
      assertFalse(ZoomOptionsUpdater.shouldRestoreFov(true, 90, 30));
   }

   @Test
   void fovTailReleasesWhenNewerStateReplacesZoomValue() {
      assertTrue(ZoomOptionsUpdater.shouldContinueFovOverride(true, true, 90, 30));
      assertFalse(ZoomOptionsUpdater.shouldContinueFovOverride(false, false, 90, 30));
      assertTrue(ZoomOptionsUpdater.shouldContinueFovOverride(false, true, 30, 30));
      assertFalse(ZoomOptionsUpdater.shouldContinueFovOverride(false, true, 90, 30));
   }

   @Test
   void cleanupBeforeCaptureDoesNotOverwriteCurrentSmoothCameraState() {
      if (ClientAccess.schizoMinecraft == null || ClientAccess.schizoMinecraft.options == null) {
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
