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
import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.render.MeshBuffer;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.utils.combat.BoundingBoxUtils;
import dev.schizo.client.render.MeshDrawContext;
import dev.schizo.client.render.ClientRenderTypes;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.math.MathUtils;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11C;

public final class MarkerTracerRenderer extends TracerRenderer {

   public MarkerTracerRenderer(TracersModule module) {
      super(module);
   }

   private void drawMarker(
      PoseStack poseStack, float targetYaw, float distance, float screenHeight, float screenWidth, float[] cameraAngles, Color color
   ) {
      float yawDifference = targetYaw - cameraAngles[0];
      float relativeYaw = Mth.wrapDegrees(yawDifference);
      double capturedRelativeYaw = relativeYaw;
      double angleRadians = Math.toRadians(capturedRelativeYaw);
      float capturedCameraPitch = cameraAngles[1];
      float negatedPitchLimit = -MathConstants.NINETY_DEGREES;
      float maximumPitch = MathConstants.NINETY_DEGREES;
      float clampedPitch = Mth.clamp(capturedCameraPitch, negatedPitchLimit, maximumPitch);
      float pitchFraction = Math.abs(clampedPitch) / MathConstants.NINETY_DEGREES;
      double centerX = screenWidth * MathConstants.HALF;
      float markerX = (float)(centerX + Math.sin(angleRadians) * distance);
      double centerY = screenHeight * MathConstants.HALF;
      double cosine = Math.cos(angleRadians);

      float markerY = (float)(centerY - cosine * (distance * Math.max(pitchFraction, 0.4F)));
      poseStack.pushPose();
      poseStack.translate(markerX, markerY, 0.0F);
      Axis markerAxis = Axis.ZP;
      float markerRotation = relativeYaw - MathConstants.NINETY_DEGREES;
      Quaternionf rotation = markerAxis.rotationDegrees(markerRotation);
      poseStack.mulPose(rotation);

      int packedColor = color.hashCode();
      this.drawArrow(poseStack, -4.0F, -4.0F, 8.0F, packedColor);
      poseStack.popPose();
   }

   private void drawArrow(
      PoseStack poseStack, float x, float y, float size, int packedColor
   ) {
      Pose pose = poseStack.last();
      Matrix4f matrix = pose.pose();

      float vertex1X = x + 0.1F * size;
      float vertex1Y = y + 0.1F * size;
      float vertex2X = x + 0.1F * size;
      float vertex2Y = y + 0.3F * size;
      float vertex3X = x + 0.1F * size;
      float vertex3Y = y + 0.7F * size;
      float vertex4X = x + 0.1F * size;
      float vertex4Y = y + 0.9F * size;
      float vertex5X = x + 0.4F * size;
      float vertex5Y = y + 0.5F * size;
      float vertex6X = x + 0.25F * size;
      float vertex6Y = y + 0.5F * size;
      addVertex(matrix, vertex1X, vertex1Y, packedColor);
      addVertex(matrix, vertex2X, vertex2Y, packedColor);
      addVertex(matrix, vertex6X, vertex6Y, packedColor);
      addVertex(matrix, vertex6X, vertex6Y, packedColor);
      addVertex(matrix, vertex5X, vertex5Y, packedColor);
      addVertex(matrix, vertex1X, vertex1Y, packedColor);
      addVertex(matrix, vertex3X, vertex3Y, packedColor);
      addVertex(matrix, vertex4X, vertex4Y, packedColor);
      addVertex(matrix, vertex5X, vertex5Y, packedColor);
      addVertex(matrix, vertex5X, vertex5Y, packedColor);
      addVertex(matrix, vertex6X, vertex6Y, packedColor);
      addVertex(matrix, vertex3X, vertex3Y, packedColor);
   }

   private static void addVertex(
      Matrix4f matrix, float x, float y, int packedColor
   ) {
      MeshBuffer buffer = MeshDrawContext.getBuffer();
      VertexConsumer vertex = buffer.addVertex(matrix, x, y, 0.0F);
      vertex.setColor(packedColor);
   }

   @Override
   public void render(PoseStack poseStack, float tickDelta) {

      float[] cameraAngles = getCameraAngles();
      Entity cameraEntity = ClientAccess.schizoMinecraft.getCameraEntity();
      Vec3 localPosition = ClientAccess.schizoMinecraft.player.getPosition(tickDelta);
      if (cameraEntity != null) {
         MeshDrawContext.getBuffer().begin(
            Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR
         );
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         Iterator players = level.players().iterator();

         while (players.hasNext()) {
            AbstractClientPlayer player = (AbstractClientPlayer)players.next();
            Vec3 cameraPosition = cameraEntity.getPosition(tickDelta);
            Vec3 playerPosition = player.getPosition(tickDelta);

            float targetYaw = RotationManager.calculateRotations(
               cameraPosition, playerPosition
            )[0];

            if (!this.module.shouldSkip(player) && !player.equals(cameraEntity)) {
               AABB bounds = BoundingBoxUtils.getRenderBounds(
                  player, tickDelta
               );

               float opacity = 1.0F;
               Setting<Boolean> ignoreVisibleSetting = this.module.ignoreVisibleSetting;
               Boolean ignoreVisible = ignoreVisibleSetting.getValue();
               if (ignoreVisible) {
                  float cameraYaw = cameraAngles[0];
                  float yawDifference = Mth.degreesDifferenceAbs(targetYaw, cameraYaw);
                  cameraPosition = BoundingBoxUtils.clampToBounds(localPosition, bounds);
                  float boundsDistance = (float)cameraPosition.distanceTo(localPosition);
                  OptionInstance firstFovSetting = ClientAccess.schizoMinecraft.options.fov();
                  float fovValue = ((Integer)firstFovSetting.get()).intValue();
                  OptionInstance secondFovSetting = ClientAccess.schizoMinecraft.options.fov();
                  int secondFovValue = ((Integer)secondFovSetting.get());
                  float fovEnd = secondFovValue + 20;
                  opacity *= MathUtils.rangeProgress(
                     yawDifference, fovValue, fovEnd
                  );

                  opacity *= MathUtils.rangeProgress(
                     boundsDistance, 0.0F, 0.1F
                  );
               }

               double capturedOpacity = opacity;

               if (!(capturedOpacity < 0.1D)) {
                  Vec3 colorPosition = player.position();
                  Color color = ColorUtils.multiplyAlpha(
                     this.module.getColor(player, colorPosition), opacity
                  );
                  Setting<Integer> capturedShadowSetting = this.module.shadowSetting;

                  if (!capturedShadowSetting.isAtMinimum()) {
                     poseStack.pushPose();
                     float shadowValue = (this.module.shadowSetting.getValue()).intValue();

                     float shadowOffset = shadowValue * 0.5F;
                     poseStack.translate(shadowOffset, shadowOffset, 0.0F);
                     float configuredShadowRadius = (this.module.distanceSetting.getValue());
                     float scaledShadowHeight = (ClientAccess.schizoMinecraft.getWindow()).getGuiScaledHeight();
                     Minecraft shadowWidthClient = ClientAccess.schizoMinecraft;
                     Window shadowWidthWindow = shadowWidthClient.getWindow();
                     float scaledShadowWidth = shadowWidthWindow.getGuiScaledWidth();

                     Color shadowColor = ColorUtils.darken(
                        color, 0.25F
                     );
                     this.drawMarker(
                        poseStack, targetYaw, configuredShadowRadius, scaledShadowHeight, scaledShadowWidth, cameraAngles, shadowColor
                     );
                     poseStack.popPose();
                  }

                  Setting<Float> distanceSetting = this.module.distanceSetting;
                  Float distance = distanceSetting.getValue();
                  float configuredRadius = distance;
                  Minecraft heightClient = ClientAccess.schizoMinecraft;
                  Window heightWindow = heightClient.getWindow();
                  float scaledHeight = heightWindow.getGuiScaledHeight();
                  Minecraft widthClient = ClientAccess.schizoMinecraft;
                  Window widthWindow = widthClient.getWindow();
                  float scaledWidth = widthWindow.getGuiScaledWidth();
                  this.drawMarker(
                     poseStack, targetYaw, configuredRadius, scaledHeight, scaledWidth, cameraAngles, color
                  );
               }
            }
         }

         GL11C.glEnable(2881);
         MeshDrawContext.getBuffer().draw(ClientRenderTypes.guiTriangles());

         GL11C.glDisable(2881);
      }
   }

   private static float[] getCameraAngles() {
      Entity cameraEntity = ClientAccess.schizoMinecraft.getCameraEntity();
      if (!cameraEntity.equals(ClientAccess.schizoMinecraft.player)) {

         float[] entityAngles = new float[2];
         float yawTickDelta = RenderUtils.getPartialTick();
         entityAngles[0] = cameraEntity.getYRot(yawTickDelta);
         float pitchTickDelta = RenderUtils.getPartialTick();
         entityAngles[1] = cameraEntity.getXRot(pitchTickDelta);
         return entityAngles;
      } else {

         float[] playerAngles = new float[2];
         float interpolationTickDelta = RenderUtils.getPartialTick();
         float previousYawBob = ClientAccess.schizoMinecraft.player.yBobO;
         float currentYawBob = ClientAccess.schizoMinecraft.player.yBob;
         playerAngles[0] = Mth.lerp(interpolationTickDelta, previousYawBob, currentYawBob);
         interpolationTickDelta = RenderUtils.getPartialTick();
         float previousPitchBob = ClientAccess.schizoMinecraft.player.xBobO;
         float currentPitchBob = ClientAccess.schizoMinecraft.player.xBob;
         playerAngles[1] = Mth.lerp(interpolationTickDelta, previousPitchBob, currentPitchBob);
         return playerAngles;
      }
   }
}
