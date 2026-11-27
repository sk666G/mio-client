/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.PostProcessShaderPass;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerModelModule extends HudWidgetModule {

   public PlayerModelModule() {
      super("PlayerModel");
      ResizableHudWidgetRenderer widgetController = new ResizableHudWidgetRenderer(this);
      widgetController.enableResizing(this);
      this.setRenderer(widgetController);
   }

   @Override
   public float[] calculateDimensions() {
      return this.dimensions(50.0F, 80.0F);
   }

   @Override
   public void renderContent(PoseStack poses) {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         return;
      }
      Camera camera = ClientAccess.schizoMinecraft.gameRenderer.getMainCamera();
      if (camera.position() != null) {
         Quaternionf quaternion = new Quaternionf();

         float rotationZ = 3.1415927F;
         Quaternionf rotation = quaternion.rotateZ(rotationZ);
         Vector3f modelTranslation = new Vector3f(
            0.0F, player.getBbHeight() / 2.0F, 0.0F
         );
         float x = 25.0F;
         float y = 40.0F;
         float scaleArgument = 35.0F;
         renderEntityPreview(poses, x, y, scaleArgument, modelTranslation, rotation, player);
      }
   }

   public static void renderEntityPreview(
      PoseStack poses, float x, float y, float scale, Vector3f modelTranslation, Quaternionf rotation, LivingEntity entity
   ) {
      RenderUtils.clearColor(
         PostProcessShaderPass.postTarget(), Color.black
      );
      RenderUtils.clearDepth(
         PostProcessShaderPass.postTarget()
      );
      Lighting lighting = ClientAccess.schizoMinecraft.gameRenderer.getLighting();
      RenderTargetUtils.overrideRenderTargets(
         PostProcessShaderPass.postTarget()
      );
      try {
         poses.pushPose();
         try {
            poses.translate(x, y, 50.0D);
            float scaleZ = -scale;
            poses.scale(scale, scale, scaleZ);

            float modelZ = modelTranslation.z;
            float modelY = modelTranslation.y;
            float modelX = modelTranslation.x;
            poses.translate(modelX, modelY, modelZ);
            poses.mulPose(rotation);
            lighting.setupFor(Entry.ENTITY_IN_UI);
            try {
               SubmitNodeStorage submitNodes = ClientAccess.schizoMinecraft.gameRenderer.getSubmitNodeStorage();
               CameraRenderState cameraState = ClientAccess.schizoMinecraft.gameRenderer.getLevelRenderState().cameraRenderState;
               Minecraft clientForEntityRenderer = Minecraft.getInstance();
               EntityRenderDispatcher entityRenderer = clientForEntityRenderer.getEntityRenderDispatcher();
               float partialTick = RenderUtils.getPartialTick();
               EntityRenderState entityState = entityRenderer.extractEntity(entity, partialTick);
               entityState.shadowPieces.clear();
               entityRenderer.submit(entityState, cameraState, 0.0, 0.0, 0.0, poses, submitNodes);
               RenderUtils.INSTANCE.flushEntities();
            } finally {
               lighting.setupFor(Entry.ITEMS_FLAT);
            }
         } finally {
            poses.popPose();
         }
      } finally {
         RenderTargetUtils.restoreRenderTargets();
      }
      RenderUtils.clearDepth(
         PostProcessShaderPass.postTarget()
      );
      RenderUtils.blitToMain(
         PostProcessShaderPass.postTarget()
      );
   }

}
