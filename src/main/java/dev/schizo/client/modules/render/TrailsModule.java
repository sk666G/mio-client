/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.render.LineMeshRenderer;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.phys.Vec3;

public class TrailsModule extends Module {
   private static final int MAX_POINTS_PER_TRAIL = 8192;
   private static final int TRAIL_PRUNE_BATCH = 2048;
   private static final int MAX_TRAIL_OWNERS = 64;
   private final LinkedHashMap<Integer, List<TrailPoint>> trailsByEntityId =
      new LinkedHashMap<>(16, 0.75F, true);
   private final Setting<Boolean> renderSetting;
   private final Setting<Boolean> bloomSetting;
   private final Setting<Float> delaySetting;
   private final Setting<Float> lineWidthSetting;
   final Setting<Boolean> fadeSetting;
   private final Setting<Float> fadeDelaySetting;
   private final Setting<Float> fadeDurationSetting;
   final Setting<Boolean> targetsSetting;
   private final Setting<Boolean> selfSetting;
   private final Setting<Boolean> playersSetting;
   private final Setting<Boolean> pearlsSetting;
   private final Setting<Boolean> arrowsSetting;
   private final Setting<Boolean> expSetting;
   final Setting<Color> colorSetting;
   private final Setting<Double> thresholdSetting;
   private final ElapsedTimer sampleTimer;
   private ClientLevel trailLevel;
   private LocalPlayer trailPlayer;
   private int lastPruneTick;
   private boolean prunedThisSession;

   private long getFadeDelayMillis() {
      float delayValue = (this.fadeDelaySetting.getValue());

      return (long)(delayValue * 1e+03F);
   }

   @Override
   public void onToggle() {
      this.clearTrails();
   }

   @EventHandler
   private void onDisconnect(MinecraftDisconnectEvent event) {
      this.clearTrails();
   }

   private void clearTrails() {
      this.trailsByEntityId.clear();
      this.trailLevel = null;
      this.trailPlayer = null;
      this.prunedThisSession = false;
   }

   private void syncTrailOwner() {
      if (this.trailLevel != ClientAccess.schizoMinecraft.level || this.trailPlayer != ClientAccess.schizoMinecraft.player) {
         this.clearTrails();
         this.trailLevel = ClientAccess.schizoMinecraft.level;
         this.trailPlayer = ClientAccess.schizoMinecraft.player;
      }
   }

   @EventHandler(priority = -100)
   private void onBeforeBufferSource(WorldRenderEvent.BeforeBufferSource event) {
      this.syncTrailOwner();
      if (!this.isWorldUnavailable()) {
         LocalPlayer player = ClientAccess.schizoMinecraft.player;
         if (player.isDeadOrDying()) {
            this.trailsByEntityId.clear();
         } else {
            Float delaySeconds = this.delaySetting.getValue();
            if (delaySeconds != 0.0F) {
               ElapsedTimer timerBeforeCheck = this.sampleTimer;
               double delayValue = (this.delaySetting.getValue()).floatValue();
               TimeUnit delayUnit = TimeUnit.SECONDS;
               if (!timerBeforeCheck.hasElapsed(delayValue, delayUnit)) {
                  return;
               }
            }

            this.sampleTimer.reset();
            int playerTick = ClientAccess.schizoMinecraft.player.tickCount;

            if (this.fadeSetting.getValue() && (!this.prunedThisSession || playerTick - this.lastPruneTick >= 4)) {
               this.prunedThisSession = true;
               this.lastPruneTick = playerTick;
               long now = System.nanoTime();
               long lifetime = TimeUnit.MILLISECONDS.toNanos(this.getFadeDelayMillis() + this.getFadeDurationMillis());
               this.trailsByEntityId.values().removeIf(points -> {
                  points.removeIf(point -> now - point.createdAtNanos > lifetime);
                  return points.isEmpty();
               });
            }

            ClientLevel level = ClientAccess.schizoMinecraft.level;
            for (Entity entity : level.entitiesForRendering()) {

               if (this.shouldTrackEntity(entity)) {
                  TrailsModule.TrailPoint point = new TrailsModule.TrailPoint(entity.getPosition(RenderUtils.getPartialTick()));
                  int newEntityId = entity.getId();
                  List<TrailPoint> entityTrail = this.getOrCreateTrail(newEntityId);
                  if (!this.fadeSetting.getValue() && entityTrail.size() >= MAX_POINTS_PER_TRAIL) {
                     entityTrail.subList(0, TRAIL_PRUNE_BATCH).clear();
                  }
                  entityTrail.add(point);
               }
            }
         }
      }
   }

   private List<TrailPoint> getOrCreateTrail(int entityId) {
      List<TrailPoint> existing = this.trailsByEntityId.get(entityId);
      if (existing != null) {
         return existing;
      }

      if (this.trailsByEntityId.size() >= MAX_TRAIL_OWNERS) {
         var eldest = this.trailsByEntityId.entrySet().iterator();
         if (eldest.hasNext()) {
            eldest.next();
            eldest.remove();
         }
      }

      List<TrailPoint> created = new ArrayList<>();
      this.trailsByEntityId.put(entityId, created);
      return created;
   }

   private void renderTrail(PoseStack poseStack, List<TrailPoint> points, float lineWidth) {
      LineMeshRenderer.begin(poseStack, lineWidth);
      Color baseColor = this.colorSetting.getValue();
      float[] baseHsb = Color.RGBtoHSB(
         baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null
      );
      ColorSetting colorMode = (ColorSetting)this.colorSetting;
      TrailRenderSettings renderSettings = new TrailRenderSettings(
         baseColor,
         colorMode.isRainbow(),
         this.thresholdSetting.getValue(),
         this.fadeSetting.getValue(),
         this.getFadeDelayMillis(),
         this.getFadeDurationMillis(),
         System.nanoTime()
      );

      TrailPoint previous = points.getFirst();
      int previousColor = getPointColor(baseHsb, previous, renderSettings).hashCode();
      for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
         TrailPoint point = points.get(pointIndex);
         int pointColor = getPointColor(baseHsb, point, renderSettings).hashCode();
         LineMeshRenderer.addVertex(previous.position.x, previous.position.y, previous.position.z,
            previousColor, true, false);
         LineMeshRenderer.addVertex(point.position.x, point.position.y, point.position.z,
            pointColor, true, false);
         previous = point;
         previousColor = pointColor;
      }
      LineMeshRenderer.draw();
   }

   private static Color getPointColor(
      float[] baseHsb, TrailsModule.TrailPoint point, TrailRenderSettings settings
   ) {
      double huePeriod = point.createdAtMillis / (settings.thresholdFactor() * 20.0D);
      float hue = (float)(
         Math.ceil(huePeriod)
            % MathConstants.FULL_TURN_DEGREES
            / MathConstants.FULL_TURN_DEGREES
      );
      long pointAgeMillis = TimeUnit.NANOSECONDS.toMillis(
         settings.nowNanos() - point.createdAtNanos
      );

      float ageAfterFadeDelay = (float)(pointAgeMillis - settings.fadeDelayMillis());
      long fadeDuration = settings.fadeDurationMillis();
      float fadeFraction = fadeDuration == 0L
         ? 0.0F
         : 1.0F - ageAfterFadeDelay / fadeDuration;
      float rawAlpha = fadeFraction * settings.baseColor().getAlpha();
      float alphaMultiplier = Mth.clamp(rawAlpha, 0.0F, 255.0F) / 255.0F;

      Color color;
      if (settings.rainbow()) {
         color = Color.getHSBColor(hue, baseHsb[1], baseHsb[2]);
      } else {
         color = settings.baseColor();
      }

      if (settings.fadeEnabled() && pointAgeMillis >= settings.fadeDelayMillis()) {
         return ColorUtils.multiplyAlpha(color, alphaMultiplier);
      }
      return ColorUtils.multiplyAlpha(color, 1.0F);
   }

   private record TrailRenderSettings(
      Color baseColor,
      boolean rainbow,
      double thresholdFactor,
      boolean fadeEnabled,
      long fadeDelayMillis,
      long fadeDurationMillis,
      long nowNanos
   ) {
   }

   @EventHandler
   private void onWorldRender(WorldRenderEvent.Main event) {
      this.syncTrailOwner();
      if (!this.isWorldUnavailable()) {
         Boolean renderEnabled = this.renderSetting.getValue();
         if (renderEnabled) {
            RenderTarget mainTarget = ClientAccess.schizoMinecraft.getMainRenderTarget();
            RenderTarget maskTarget = MaskShaderRenderer.maskTarget();
            Setting<Color> maskColor = this.colorSetting;
            RenderUtils.clearColor(
               maskTarget, maskColor.getValue()
            );
            RenderUtils.clearDepth(
               MaskShaderRenderer.maskTarget()
            );
            RenderTargetUtils.setMainRenderTarget(
               MaskShaderRenderer.maskTarget()
            );
            try {
               float width = this.lineWidthSetting.getValue();
               for (List<TrailPoint> points : this.trailsByEntityId.values()) {
                  if (points.size() > 2) {
                     this.renderTrail(event.getPoseStack(), points, width);
                  }
               }
            } finally {
               RenderTargetUtils.setMainRenderTarget(mainTarget);
            }
            RenderUtils.blitToMain(
               MaskShaderRenderer.maskTarget()
            );
            Boolean bloomEnabled = this.bloomSetting.getValue();
            if (!bloomEnabled) {
               return;
            }

            Float bloomWidth = this.lineWidthSetting.getValue();
            float effectWidth = 6.0F + bloomWidth;

            MaskShaderRenderer.setParameters(
               effectWidth, 1.5F, 1.0F,2.0F);

            MaskShaderRenderer.renderEffect("bloom_mask");
            return;
         }
      }
   }

   private boolean shouldTrackEntity(Entity entity) {
      if (entity == ClientAccess.schizoMinecraft.player) {
         Boolean includeSelf = this.selfSetting.getValue();
         if (includeSelf) {
            return true;
         }
      }

      if (entity instanceof Player) {
         Boolean includePlayers = this.playersSetting.getValue();
         if (includePlayers && entity != ClientAccess.schizoMinecraft.player) {
            return true;
         }
      }

      if (entity instanceof ThrownEnderpearl) {
         Boolean includePearls = this.pearlsSetting.getValue();
         if (includePearls) {
            return true;
         }
      }

      if (entity instanceof ThrownExperienceBottle) {
         Boolean includeExperienceBottles = this.expSetting.getValue();
         if (includeExperienceBottles) {
            return true;
         }
      }

      if (entity instanceof Arrow) {
         Boolean includeArrows = this.arrowsSetting.getValue();
         if (includeArrows) {
            return true;
         }
      }

      return false;
   }

   private long getFadeDurationMillis() {
      float durationValue = (this.fadeDurationSetting.getValue());

      return (long)(durationValue * 1e+03F);
   }

   public TrailsModule() {
      super("Trails", "Draws trails behind certain entities.", ModuleCategory.RENDER, new String[0]);
      this.renderSetting = this.add(new BooleanSetting(new String("Render"), true));
      this.bloomSetting = this.add(new BooleanSetting(new String("Bloom"), false));
      this.delaySetting = this.add(new NumberSetting<>(new String("Delay"), 0.0F, 0.0F, 5.0F).withUnit(new String("s")));
      this.lineWidthSetting = this.add(new NumberSetting<>(new String("LineWidth"), 1.0F, 0.1F, 5.0F));
      this.fadeSetting = this.add(new BooleanSetting(new String("Fade"), false).markVisibilityDependency());
      Predicate<Object> fadeDelayVisible = ignored -> this.fadeSetting.isExpanded();
      this.fadeDelaySetting = this.add(new NumberSetting<>(new String("FadeDelay"), 1.0F, 0.0F, 10.0F, fadeDelayVisible).withUnit(new String("s")));
      Predicate<Object> fadeDurationVisible = ignored -> this.fadeSetting.isExpanded();
      this.fadeDurationSetting = this.add(new NumberSetting<>(new String("FadeDuration"), 0.5F, 0.0F, 10.0F, fadeDurationVisible).withUnit(new String("s")));
      this.targetsSetting = this.add(new BooleanSetting(new String("Targets"), false).markAsGroup().markVisibilityDependency());
      Predicate<Object> selfVisible = ignored -> this.targetsSetting.isExpanded();
      this.selfSetting = this.add(new BooleanSetting(new String("Self"), true, selfVisible));
      Predicate<Object> playersVisible = ignored -> this.targetsSetting.isExpanded();
      this.playersSetting = this.add(new BooleanSetting(new String("Players"), false, playersVisible));
      Predicate<Object> pearlsVisible = ignored -> this.targetsSetting.isExpanded();
      this.pearlsSetting = this.add(new BooleanSetting(new String("Pearls"), false, pearlsVisible));
      Predicate<Object> arrowsVisible = ignored -> this.targetsSetting.isExpanded();
      this.arrowsSetting = this.add(new BooleanSetting(new String("Arrows"), false, arrowsVisible));
      Predicate<Object> expVisible = ignored -> this.targetsSetting.isExpanded();
      this.expSetting = this.add(new BooleanSetting(new String("Exp"), false, expVisible));
      this.colorSetting = this.add(new ColorSetting(new String("Color"), new Color(-1312974337, true)));
      Predicate<Object> thresholdVisible = ignored -> ((ColorSetting)this.colorSetting).isRainbow();
      this.thresholdSetting = this.add(new NumberSetting<>(new String("Threshold"), 1.0, 0.1, 3.0, thresholdVisible));
      this.sampleTimer = new ElapsedTimer();

      this.setDrawn(false);
   }

   private static class TrailPoint {
      private final long createdAtMillis;
      private final long createdAtNanos;
      private final Vec3 position;

      private TrailPoint(Vec3 position) {
         this.createdAtMillis = System.currentTimeMillis();
         this.createdAtNanos = System.nanoTime();
         this.position = position;
      }

   }
}
