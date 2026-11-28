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
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ScreenChangeEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.events.MouseHandlerOnScrollEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.InterpolatedFloat;
import dev.schizo.client.utils.movement.EntityMotionAccess;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.GameOptionsOverrides;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

public class FreeLookModule extends Module {

   private static FreecamModule freecamModule;
   private final InterpolatedFloat cameraDistanceAnimation;
   private final Vector2f cameraRotation;
   private CameraType cameraType;
   private LocalPlayer cameraOwner;
   private float cameraDistance;
   public final Setting<Boolean> scrollSetting;

   @Override
   public void onEnable() {
      if (this.isWorldUnavailable()) {
         this.disable();
         return;
      }

      this.cameraDistance = 1e+02F;
      this.cameraDistanceAnimation.setValue(0.0F);
      Options options = ClientAccess.schizoMinecraft.options;
      CameraType selectedCameraType;
      if (options.getCameraType() == CameraType.FIRST_PERSON) {
         selectedCameraType = CameraType.THIRD_PERSON_BACK;
      } else {
         selectedCameraType = ClientAccess.schizoMinecraft.options.getCameraType();
      }
      this.cameraType = selectedCameraType;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      this.cameraOwner = player;
      GameOptionsOverrides.registerCameraOverride(this);
      Vector2f rotation = this.cameraRotation;
      float yaw = player.getYRot();
      float pitch = player.getXRot();
      rotation.set(yaw, pitch);
      if (ClientAccess.schizoMinecraft.player instanceof EntityMotionAccess cameraPlayer) {
         float cameraYaw = this.cameraRotation.x;
         cameraPlayer.setCameraYaw(cameraYaw);
         float cameraPitch = this.cameraRotation.y;
         cameraPlayer.setCameraPitch(cameraPitch);
      }
   }

   @Override
   public void onDisable() {
      GameOptionsOverrides.unregisterCameraOverride(this);
      LocalPlayer owner = this.cameraOwner;
      this.cameraOwner = null;
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (minecraft == null) {
         return;
      }
      LocalPlayer player = minecraft.player;
      if (!shouldRestorePlayerRotation(owner, player)) {
         return;
      }

      float yaw = this.cameraRotation.x();
      float pitch = this.cameraRotation.y();
      player.setYRot(yaw);
      player.yBobO = player.yBob = player.yRotO = yaw;
      player.setXRot(pitch);
      player.xBobO = player.xBob = player.xRotO = pitch;

      ClientLevel level = minecraft.level;
      if (level == null || minecraft.gameRenderer == null) {
         return;
      }
      Camera camera = minecraft.gameRenderer.getMainCamera();
      camera.setup(level, player, false, false, 1.0F);
   }

   static boolean shouldRestorePlayerRotation(Object capturedPlayer, Object currentPlayer) {
      return ownsCurrentPlayer(capturedPlayer, currentPlayer);
   }

   static boolean ownsCurrentPlayer(Object capturedPlayer, Object currentPlayer) {
      return capturedPlayer != null && capturedPlayer == currentPlayer;
   }

   public float getCameraDistanceOffset() {
      float offset = this.cameraDistanceAnimation.getValue();
      return offset / 1e+01F;
   }

   public boolean isScrollEnabled() {
      return this.isToggled() && this.scrollSetting.getValue();
   }

   @EventHandler(priority = -200)
   private void onLocalPlayerPreTick(LocalPlayerPreTickEvent event) {
      LocalPlayer currentPlayer = ClientAccess.schizoMinecraft.player;
      if (!ownsCurrentPlayer(this.cameraOwner, currentPlayer)) {
         this.disable();
         return;
      }
      FreecamModule freecam = freecamModule;
      if (freecam.isToggled()) {
         return;
      }
      CameraType selectedCameraType = this.cameraType;
      GameOptionsOverrides.applyCameraOverride(this, selectedCameraType);
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      LocalPlayer pickPlayer = ClientAccess.schizoMinecraft.player;
      double range = ClientAccess.schizoMinecraft.player.blockInteractionRange();
      float partialTick = RenderUtils.getPartialTick();
      minecraft.hitResult = pickPlayer.pick(range, partialTick, false);
   }

   @EventHandler
   private void onMouseHandlerOnScrollEvent(MouseHandlerOnScrollEvent event) {
      Boolean enabled = this.scrollSetting.getValue();
      if (enabled) {
         float previousDistance = this.cameraDistance;
         float scrollDelta = (float)event.getVerticalOffset() * this.cameraDistance;
         this.cameraDistance = previousDistance - scrollDelta * 0.08F;
         float distance = this.cameraDistance;
         this.cameraDistance = Mth.clamp(distance, 1.0F, 2e+02F);
         event.cancel();
      }
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.disable();
   }

   @EventHandler
   private void onMinecraftSetScreenEvent(
      ScreenChangeEvent.Before event
   ) {
      if (event.getNewScreen() instanceof LevelLoadingScreen) {
         this.disable();
      }
   }

   @EventHandler(priority = 200)
   private void onMinecraftRunTickEvent(MinecraftRunTickEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      InterpolatedFloat targetAnimation = this.cameraDistanceAnimation;
      float distance = this.cameraDistance;
      float target = distance - 1e+02F;
      targetAnimation.setTarget(target);
      this.cameraDistanceAnimation.updateAndGet();
   }

   public Vector2f getCameraRotation() {
      return this.cameraRotation;
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      freecamModule = (FreecamModule)registry.getModule(
         FreecamModule.class
      );
   }

   public FreeLookModule() {
      super("FreeLook", "Allows you to rotate your 3rd person camera without rotating your player.", ModuleCategory.RENDER, new String[0]);
      this.cameraDistanceAnimation = new InterpolatedFloat(2.0F, true);
      this.cameraRotation = new Vector2f();
      Boolean scrollDefault = false;
      String scrollName = new String("Scroll");
      BooleanSetting scroll = new BooleanSetting(scrollName, scrollDefault);
      this.scrollSetting = this.add(scroll);
   }

}
