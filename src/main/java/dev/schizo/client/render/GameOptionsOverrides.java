/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.access.OptionValueAccess;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.settings.NamedEntry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public final class GameOptionsOverrides implements ClientAccess {
   private static final Set<NamedEntry> CAMERA_OVERRIDE_OWNERS = newCameraOwnerSet();
   private static CameraType savedCameraType;
   private static CameraType lastAppliedCameraType;

   private GameOptionsOverrides() {
   }

   public static void forceSetOptionValue(OptionInstance option, Object value) {
      OptionValueAccess optionValue = (OptionValueAccess)(Object)option;
      optionValue.forceSetValue(value);
   }

   public static void registerCameraOverride(NamedEntry owner) {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (minecraft == null || minecraft.options == null) {
         return;
      }
      if (CAMERA_OVERRIDE_OWNERS.isEmpty()) {
         savedCameraType = minecraft.options.getCameraType();
      }

      CAMERA_OVERRIDE_OWNERS.add(owner);
   }

   static Set<NamedEntry> newCameraOwnerSet() {
      return Collections.newSetFromMap(new IdentityHashMap<>());
   }

   public static void applyCameraOverride(NamedEntry owner, CameraType cameraType) {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (minecraft == null || minecraft.options == null || cameraType == null) {
         return;
      }
      registerCameraOverride(owner);
      if (!CAMERA_OVERRIDE_OWNERS.contains(owner)) {
         return;
      }
      minecraft.options.setCameraType(cameraType);
      lastAppliedCameraType = cameraType;
   }

   static boolean shouldRestoreCameraType(
      CameraType lastAppliedType,
      CameraType currentType
   ) {
      return lastAppliedType != null && lastAppliedType == currentType;
   }

   public static void unregisterCameraOverride(NamedEntry owner) {
      CAMERA_OVERRIDE_OWNERS.remove(owner);
      if (CAMERA_OVERRIDE_OWNERS.isEmpty()) {
         Minecraft minecraft = ClientAccess.schizoMinecraft;
         if (savedCameraType != null
            && minecraft != null
            && minecraft.options != null
            && shouldRestoreCameraType(
               lastAppliedCameraType, minecraft.options.getCameraType()
            )) {
            Options gameOptions = minecraft.options;
            CameraType cameraTypeToRestore = savedCameraType;
            gameOptions.setCameraType(cameraTypeToRestore);
         }
         savedCameraType = null;
         lastAppliedCameraType = null;
      }
   }
}
