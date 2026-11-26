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
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.render.WorldBoxRenderer;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PhaseESPModule extends Module {
   private final Setting<Boolean> crawlingSetting;
   private final Setting<Boolean> inBlocksSetting;
   private final Setting<Boolean> insideSetting;
   private final Setting<Color> safeSetting;
   private final Setting<Color> semiSafeSetting;
   private final Setting<Color> unsafeSetting;
   private final Setting<Boolean> outlineSetting;
   private final Setting<Float> alphaSetting;
   private final Map<BlockPos, BlockAnimation> blockAnimations;
   private ClientLevel animationLevel;

   private boolean isNotBedrock(BlockPos position) {

      return BlockInteractionUtils.getBlock(position)
         != Blocks.BEDROCK;
   }

   private boolean shouldHighlightPosition(BlockPos position) {
      Boolean allowInBlocks = this.inBlocksSetting.getValue();
      if (allowInBlocks) {
         Boolean allowInside = this.insideSetting.getValue();
         if (allowInside) {
            return true;
         }
      }

      AABB blockBounds = new AABB(position);
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState shapeState = level.getBlockState(position);
      ClientLevel shapeLevel = ClientAccess.schizoMinecraft.level;
      VoxelShape collisionShape = shapeState.getCollisionShape(shapeLevel, position);
      if (!collisionShape.isEmpty()) {
         AABB localBounds = collisionShape.bounds();
         blockBounds = localBounds.move(position);
      }

      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      AABB playerBounds = player.getBoundingBox();

      AABB insetBounds = blockBounds.inflate(-0.05D);
      return !playerBounds.intersects(insetBounds);
   }

   private boolean isPhaseCandidate(BlockPos position) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState state = level.getBlockState(position);
      Block block = state.getBlock();
      float explosionResistance = block.getExplosionResistance();

      if (explosionResistance >= 600.0F) {

         if (BlockInteractionUtils.isCollidable(position)) {
            if (!state.canBeReplaced()) {
               return true;
            }
         }
      }

      return false;
   }

   private PhaseESPModule.Safety classifySafetyForRender(BlockPos position) {
      return this.classifySafety(position);
   }

   private PhaseESPModule.Safety classifySafety(BlockPos position) {

      Block block = WorldUtils.getBlock(position);
      float explosionResistance = block.getExplosionResistance();

      boolean resistantBlock = explosionResistance >= 600.0F;
      BlockPos collisionFloorPosition = position.below();

      if (BlockInteractionUtils.isCollidable(collisionFloorPosition) && resistantBlock) {
         BlockPos resistanceFloorPosition = position.below();

         Block floorBlock = WorldUtils.getBlock(resistanceFloorPosition);
         explosionResistance = floorBlock.getExplosionResistance();

         if (!(explosionResistance < 600.0F)) {

            if (!this.isNotBedrock(position)) {
               BlockPos bedrockFloorPosition = position.below();

               if (!this.isNotBedrock(bedrockFloorPosition)) {
                  return PhaseESPModule.Safety.SAFE;
               }
            }

            return PhaseESPModule.Safety.SEMI;
         }
      }

      return PhaseESPModule.Safety.UNSAFE;
   }

   private void refreshPosition(BlockPos position, long nowNanos) {
      BlockAnimation animation = this.blockAnimations.get(position);
      if (animation == null) {
         animation = BlockAnimation.create(nowNanos);
      } else {
         animation = animation.refresh(nowNanos);
      }
      this.blockAnimations.put(position, animation);
   }

   private void refreshAnimationLevel(ClientLevel level) {
      if (this.animationLevel != level) {
         this.blockAnimations.clear();
         this.animationLevel = level;
      }
   }

   private AABB getBlockBounds(BlockPos position) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      BlockState shapeState = level.getBlockState(position);
      ClientLevel shapeLevel = ClientAccess.schizoMinecraft.level;
      VoxelShape shape = shapeState.getShape(shapeLevel, position);
      if (shape.isEmpty()) {
         shape = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
      }

      AABB localBounds = shape.bounds();
      return localBounds.move(position);
   }

   private static boolean isPlayerInCollidableBlock() {
      BlockPos playerPosition = PlayerStateUtils.getLocalRoundedBlockPosition();
      return BlockInteractionUtils.isCollidable(playerPosition);
   }

   private void removeExpiredPositions(long nowNanos) {
      this.blockAnimations.entrySet().removeIf(
         entry -> entry.getValue().isExpired(nowNanos)
      );
   }

   @EventHandler
   private void onWorldRender(WorldRenderEvent.Main event) {
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      if (level == null || ClientAccess.schizoMinecraft.player == null) {
         this.blockAnimations.clear();
         this.animationLevel = null;
         return;
      }
      this.refreshAnimationLevel(level);
      long nowNanos = System.nanoTime();
      this.removeExpiredPositions(nowNanos);
      double playerMinY = ClientAccess.schizoMinecraft.player.getBoundingBox().minY;
      float roundedPlayerY = (float)Math.round(playerMinY);
      AABB playerBounds = ClientAccess.schizoMinecraft.player.getBoundingBox();
      AABB minimumYBounds = playerBounds.setMinY(roundedPlayerY);
      AABB flatBounds = minimumYBounds.setMaxY(roundedPlayerY);
      AABB searchBounds = flatBounds.inflate(0.01D, 0.0D, 0.01D);

      boolean crawlingBlocked = PlayerStateUtils.isLocalHeightBelowOne()
         && !(this.crawlingSetting.getValue());
      boolean inBlockBlocked = isPlayerInCollidableBlock()
         && !(this.inBlocksSetting.getValue());
      if (!inBlockBlocked && !crawlingBlocked) {
         BlockPos.betweenClosedStream(searchBounds)
            .map(BlockPos::immutable)
            .filter(this::isPhaseCandidate)
            .filter(this::shouldHighlightPosition)
            .forEach(position -> this.refreshPosition(position, nowNanos));
      }

      for (Entry<BlockPos, BlockAnimation> entry : this.blockAnimations.entrySet()) {
         BlockPos position = entry.getKey();
         PhaseESPModule.BlockAnimation animation = entry.getValue();

         PhaseESPModule.Safety safety = classifySafetyForRender(position);
         Color fillColor = safety.getFadedColor(this, animation, nowNanos);

         AABB blockBounds = this.getBlockBounds(position);
         PoseStack fillPoses = event.getPoseStack();
         BlockPos fillPosition = position;
         double fillMaxY = fillPosition.getY();
         WorldBoxRenderer.fillBox(
            fillPoses, blockBounds.setMaxY(fillMaxY), fillColor
         );
         Boolean drawOutline = this.outlineSetting.getValue();
         if (drawOutline) {
            float animationAlpha = animation.getAlpha(nowNanos);
            Float configuredAlpha = this.alphaSetting.getValue();
            float outlineAlpha = animationAlpha * configuredAlpha;
            PoseStack outlinePoses = event.getPoseStack();
            BlockPos outlinePosition = position;
            double outlineMaxY = outlinePosition.getY();
            AABB outlineBounds = blockBounds.setMaxY(outlineMaxY);
            Color outlineColor = ColorUtils.withAlpha(fillColor, outlineAlpha);

            WorldBoxRenderer.outlineBox(
               outlinePoses, outlineBounds, outlineColor, 1.0F);
         }
      }
   }

   @Override
   public void onDisable() {
      this.blockAnimations.clear();
      this.animationLevel = null;
   }

   public PhaseESPModule() {
      super("PhaseESP", "Highlights safe blocks to phase into.", ModuleCategory.RENDER, new String[0]);
      this.crawlingSetting = this.add(new BooleanSetting(new String("Crawling"), false));
      this.inBlocksSetting = this.add(new BooleanSetting(new String("InBlocks"), false));
      this.insideSetting = this.add(new BooleanSetting(new String("Inside"), false).dependsOn(this.inBlocksSetting));
      this.safeSetting = this.add(new ColorSetting(new String("Safe"), new Color(540373280, true)));
      this.semiSafeSetting = this.add(new ColorSetting(new String("SemiSafe"), new Color(637525248, true)));
      this.unsafeSetting = this.add(new ColorSetting(new String("Unsafe"), new Color(634257408, true)));
      this.outlineSetting = this.add(new BooleanSetting(new String("Outline"), false).markVisibilityDependency());
      this.alphaSetting = this.add(new NumberSetting<>(new String("Alpha"), 1.0F, 0.0F, 1.0F).dependsOn(this.outlineSetting));
      this.blockAnimations = new HashMap<>();
   }

   enum Safety {
      SAFE,
      SEMI,
      UNSAFE;

      Color getFadedColor(
         PhaseESPModule module, PhaseESPModule.BlockAnimation animation, long nowNanos
      ) {
         Setting<Color> colorSetting = switch (this) {
            case SAFE -> module.safeSetting;
            case SEMI -> module.semiSafeSetting;
            case UNSAFE -> module.unsafeSetting;
         };
         Color baseColor = colorSetting.getValue();
         float baseAlpha = baseColor.getAlpha();
         return ColorUtils.withAlpha(
            baseColor, (int)(baseAlpha * animation.getAlpha(nowNanos))
         );
      }

   }

   static record BlockAnimation(
      long createdAtNanos,
      long expiresAtNanos
   ) {
      static final long FADE_NANOS = 75_000_000L;
      static final long RETENTION_NANOS = 200_000_000L;

      static BlockAnimation create(long nowNanos) {
         return new BlockAnimation(nowNanos, nowNanos + RETENTION_NANOS);
      }

      BlockAnimation refresh(long nowNanos) {
         return new BlockAnimation(this.createdAtNanos, nowNanos + RETENTION_NANOS);
      }

      boolean isExpired(long nowNanos) {
         return nowNanos > this.expiresAtNanos;
      }

      float getAlpha(long nowNanos) {
         long ageNanos = Math.max(0L, nowNanos - this.createdAtNanos);
         if (ageNanos <= FADE_NANOS) {
            return (float)ageNanos / (float)FADE_NANOS;
         }

         long remainingNanos = this.expiresAtNanos - nowNanos;
         if (remainingNanos <= FADE_NANOS) {
            return Mth.clamp(
               (float)remainingNanos / (float)FADE_NANOS,
               0.0F,
               1.0F
            );
         }

         return 1.0F;
      }
   }

}
