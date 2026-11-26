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
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.gui.ScissorStack;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.exploit.DetectedChunk;
import dev.schizo.client.modules.exploit.ChunkKind;
import dev.schizo.client.modules.exploit.NewChunksModule;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class MapModule extends HudWidgetModule {
   private static NewChunksModule newChunks() {
      return ClientContext.getModule(NewChunksModule.class);
   }
   private static final Identifier NAVIGATION_TEXTURE;
   private final Setting<PointerMode> pointerModeSetting;
   private final Setting<Float> widthSetting;
   private final Setting<Float> heightSetting;
   private final Setting<Color> pointerColorSetting;
   private final Setting<Color> backgroundSetting;
   private final Setting<Color> outlineSetting;

   static {

      String texturePath = "textures/nav.png";
      String textureNamespace = "schizo";
      NAVIGATION_TEXTURE = Identifier.fromNamespaceAndPath(textureNamespace, texturePath);
   }

   @Override
   public float[] calculateDimensions() {

      Setting<Float> widthOption = this.widthSetting;
      Float width = widthOption.getValue();
      Setting<Float> heightOption = this.heightSetting;
      Float height = heightOption.getValue();
      return this.dimensions(width, height);
   }

   private int getChunkRadius() {
      double mapWidth = this.widthSetting.getValue();
      double mapHeight = this.heightSetting.getValue();
      return (int)Math.ceil(Math.hypot(mapWidth, mapHeight) / 16.0D);
   }

   private static boolean isChunkWithinRange(ChunkPos chunk, ChunkPos playerChunk, int chunkRadius) {
      return chunk.distanceSquared(playerChunk) <= chunkRadius * chunkRadius;
   }

   @Override
   public void renderContent(PoseStack poses) {
      if (dev.schizo.client.utils.CoordinatePrivacy.isEnabled()) return;
      float[] widgetSize = this.renderer
         .getDimensions();
      if (widgetSize != null) {
         int[] renderColorValues = RenderUtils.getRenderColor();

         int previousRenderState = renderColorValues[3];

         RenderUtils.setColorAlpha(255);
         try {
            GuiDrawUtils.flush();
         } finally {
            RenderUtils.setColorAlpha(previousRenderState);
         }
         RenderUtils backgroundRenderer = RenderUtils.INSTANCE;
         Setting<Float> backgroundWidthOption = this.widthSetting;
         Float backgroundWidth = backgroundWidthOption.getValue();
         float rectangleWidth = backgroundWidth;
         Setting<Float> backgroundHeightOption = this.heightSetting;
         Float backgroundHeight = backgroundHeightOption.getValue();
         float rectangleHeight = backgroundHeight;
         Setting<Color> backgroundColorOption = this.backgroundSetting;
         backgroundRenderer.fillRect(
            poses, 0.0F, 0.0F, rectangleWidth, rectangleHeight, backgroundColorOption.getValue()
         );
         RenderUtils outlineRenderer = RenderUtils.INSTANCE;

         float outlineX = -1.0F;

         float outlineY = -1.0F;
         Setting<Float> outlineWidthOption = this.widthSetting;
         Float outlineWidth = outlineWidthOption.getValue();
         rectangleWidth = outlineWidth;
         Setting<Float> outlineHeightOption = this.heightSetting;
         Float outlineHeight = outlineHeightOption.getValue();
         rectangleHeight = outlineHeight;
         Setting<Color> outlineColorOption = this.outlineSetting;
         outlineRenderer.drawRectOutline(
            poses, outlineX, outlineY, rectangleWidth, rectangleHeight, outlineColorOption.getValue()
         );
         Setting<Float> centerWidthOption = this.widthSetting;
         Float centerWidth = centerWidthOption.getValue();
         float mapWidth = centerWidth;

         float centerX = mapWidth / 2.0F;
         Setting<Float> centerHeightOption = this.heightSetting;
         Float centerHeight = centerHeightOption.getValue();
         float mapHeight = centerHeight;

         float centerY = mapHeight / 2.0F;
         poses.pushPose();
         try {
            float mapDepth = 0.0F;
            poses.translate(centerX, centerY, mapDepth);

            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            if (player != null) {
               NewChunksModule newChunksModule = newChunks();
               ChunkPos playerChunk = player.chunkPosition();
               int chunkRadius = this.getChunkRadius();
               Vec3 cameraPosition = ClientAccess.schizoMinecraft.gameRenderer.getMainCamera().position();

               Iterator<BlockPos> dungeons = newChunksModule.getDetectedDungeons().iterator();
               while (dungeons.hasNext()) {
                  ChunkPos dungeonChunk = new ChunkPos(dungeons.next());
                  if (isChunkWithinRange(dungeonChunk, playerChunk, chunkRadius)) {
                     Color dungeonColor = ColorUtils.withAlpha(
                        newChunksModule.dungeonFillSetting.getValue(), 0.4F
                     );
                     this.renderChunk(poses, cameraPosition, dungeonChunk, dungeonColor);
                  }
               }

               Iterator<DetectedChunk> chunks = newChunksModule.getVisibleChunks().iterator();
               while (chunks.hasNext()) {
                  DetectedChunk visibleChunk = chunks.next();
                  ChunkPos chunkPosition = visibleChunk.chunkPosition();
                  if (!isChunkWithinRange(chunkPosition, playerChunk, chunkRadius)) {
                     continue;
                  }

                  Color fillColor = visibleChunk.kind().getFillColor(newChunksModule);
                  if (fillColor != null) {
                     Color chunkColor = ColorUtils.withAlpha(fillColor, 0.25F);
                     this.renderChunk(poses, cameraPosition, chunkPosition, chunkColor);
                  }
               }
            }
         } finally {
            poses.popPose();
         }
         ScissorStack.push(
            (int)this.renderer
               .getX(),
            (int)this.renderer
               .getY(),
            (int)widgetSize[0],
            (int)widgetSize[1]
         );
         try {
            GuiDrawUtils.flush();
         } finally {
            ScissorStack.pop();
         }
         this.renderPointer(poses, centerX, centerY);
      }
   }

   private void renderChunk(
      PoseStack poses, Vec3 cameraPosition, ChunkPos chunk, Color color
   ) {
      double cameraCoordinate = cameraPosition.x;
      float cameraOffsetX = (float)(cameraCoordinate - chunk.getMinBlockX());

      float scaledOffsetX = cameraOffsetX / 2.0F;

      float offsetX = scaledOffsetX - 8.0F;
      cameraCoordinate = cameraPosition.z;
      float cameraOffsetZ = (float)(cameraCoordinate - chunk.getMinBlockZ());

      float scaledOffsetZ = cameraOffsetZ / 2.0F;

      float offsetZ = scaledOffsetZ - 8.0F;
      poses.pushPose();
      try {
         float markerDepth = 0.0F;
         poses.translate(offsetX, offsetZ, markerDepth);

         float markerWidth = 8.0F;

         GuiDrawUtils.fillRect(
            poses, 0.0F, 0.0F, markerWidth, 8.0F, color
         );
      } finally {
         poses.popPose();
      }
   }

   private void renderPointer(PoseStack poses, float centerX, float centerY) {
      poses.pushPose();
      try {
         float pointerDepth = 0.0F;
         poses.translate(centerX, centerY, pointerDepth);
      Setting<PointerMode> dotModeOption = this.pointerModeSetting;
      if (dotModeOption.getValue() == MapModule.PointerMode.DOT) {
         RenderUtils dotRenderer = RenderUtils.INSTANCE;

         float dotX = -1.0F;

         float dotY = -1.0F;

         float dotWidth = 1.0F;

         float dotHeight = 1.0F;
         Setting<Color> dotColorOption = this.pointerColorSetting;
         dotRenderer.fillRect(
            poses, dotX, dotY, dotWidth, dotHeight, dotColorOption.getValue()
         );
      } else {
         Setting<PointerMode> arrowModeOption = this.pointerModeSetting;
         if (arrowModeOption.getValue() == MapModule.PointerMode.ARROW) {

            float arrowScaleX = 0.05F;

            float arrowScaleY = 0.05F;

            float arrowScaleZ = 1.0F;
            poses.scale(arrowScaleX, arrowScaleY, arrowScaleZ);
            Axis rotationAxis = Axis.ZP;

            float rotationOffset = 45.0F;
            GameRenderer gameRenderer = ClientAccess.schizoMinecraft.gameRenderer;
            Camera camera = gameRenderer.getMainCamera();
            float rotationDegrees = rotationOffset + camera.yRot();
            Quaternionf arrowRotation = rotationAxis.rotationDegrees(rotationDegrees);
            poses.mulPose((Quaternionfc)arrowRotation);
            int[] renderStateValues = RenderUtils.getRenderColor();

            float renderAlpha = renderStateValues[3];
            Setting<Color> arrowColorOption = this.pointerColorSetting;
            int pointerArgb = ColorUtils.toArgb(
               arrowColorOption.getValue(), renderAlpha
            );
            Identifier texture = NAVIGATION_TEXTURE;

            GuiDrawUtils.drawTexture(
               poses,
               texture,
               -128,
               -128,
               0.0F,
               0.0F,
               256,
               256,
               256,
               256,
               pointerArgb
            );
            RenderBuffers renderBuffers = ClientAccess.schizoMinecraft.renderBuffers();
            BufferSource bufferSource = renderBuffers.bufferSource();
            bufferSource.endBatch();
         }
      }
      } finally {
         poses.popPose();
      }
   }

   public MapModule() {
      super("Map", new String[]{"minimap"});

      this.pointerModeSetting = this.add(new EnumSetting<>("Pointer", MapModule.PointerMode.ARROW));

      this.widthSetting = this.add(new NumberSetting<>(
         "Width",
         76.0F,
         50.0F,
         500.0F
      ));
      this.heightSetting = this.add(new NumberSetting<>(
         "Height",
         76.0F,
         50.0F,
         250.0F
      ));
      this.pointerColorSetting = this.add(new ColorSetting("Pointer", Color.white));
      this.backgroundSetting = this.add(new ColorSetting(
         "Background",
         new Color(
            10,
            10,
            10,
            50
         )
      ));
      this.outlineSetting = this.add(new ColorSetting(
         "Outline",
         new Color(
            10,
            10,
            10,
            100
         )
      ));
      this.pointerColorSetting.setConfigName("PointerColor");
      this.pointerColorSetting.forceOpaqueColor();
      ChatFormatting descriptionColor = ChatFormatting.YELLOW;
      String descriptionPrefix = String.valueOf(descriptionColor);
      String descriptionText = descriptionPrefix + "Primarily used for NewChunks";

      this.setDescription(descriptionText);
      ResizableHudWidgetRenderer widget = new ResizableHudWidgetRenderer(this);
      widget.enableResizing(this);
      this.setRenderer(widget);
   }

   private enum PointerMode implements NamedEntry {
      NONE("None"),
      ARROW("Arrow"),
      DOT("Dot");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      PointerMode(String displayName) {
         this.displayName = displayName;
      }
   }
}
