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
import dev.schizo.client.commands.FakeplayerCommand;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.HudRenderEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.utils.network.InteractPacketUtils;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.managers.SoundManager;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.VertexGeometry;
import dev.schizo.client.utils.collections.NamedIdentifier;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.IdentifierSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.schizo.mixin.ServerboundInteractPacketAccessor;
import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HitmarkerModule extends Module {
   private static CrosshairModule crosshair;
   final Setting<Boolean> drawSetting;
   private final Setting<Float> lengthSetting;
   private final Setting<Float> timeSetting;
   private final Setting<Float> fadeTimeSetting;
   private final Setting<Color> colorSetting;
   final Setting<Boolean> soundSetting;
   private final Setting<NamedIdentifier> soundTypeSetting;
   private final Setting<Float> volumeSetting;
   final Setting<Boolean> targetsSetting;
   private final Setting<Boolean> animalsSetting;
   private final Setting<Boolean> hostilesSetting;
   private final Setting<Boolean> playersSetting;
   private final Setting<Boolean> crystalsSetting;
   public long lastHitTime;
   private long lastHitNanos = -1L;
   public boolean soundPending;

   public boolean isEligibleTarget(Entity entity) {
      Boolean animalsEnabled = this.animalsSetting.getValue();
      if (animalsEnabled && entity instanceof AgeableMob) {
         return true;
      }
      Boolean hostilesEnabled = this.hostilesSetting.getValue();
      if (hostilesEnabled && entity instanceof Enemy) {
         return true;
      }
      Boolean playersEnabled = this.playersSetting.getValue();
      if (playersEnabled && entity instanceof Player) {
         return true;
      }
      return (this.crystalsSetting.getValue()) && entity instanceof EndCrystal;
   }

   private float getMarkerOpacity() {
      float secondsCapture = (this.timeSetting.getValue());

      long durationMillis = (long)(secondsCapture * 1e+03F);
      secondsCapture = (this.fadeTimeSetting.getValue());

      long fadeMillis = (long)(secondsCapture * 1e+03F);
      long elapsedMillis = this.elapsedHitMillis();
      if (elapsedMillis < durationMillis - fadeMillis) {

         return 1.0F;
      } else {

         return 1.0F - (float)(elapsedMillis - (durationMillis - fadeMillis)) / (float)fadeMillis;
      }
   }

   private long elapsedHitMillis() {
      if (this.lastHitNanos < 0L) {
         return Long.MAX_VALUE;
      }
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.lastHitNanos);
   }
   @Override
   public void onToggle() {
      this.resetTransientState();
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.resetTransientState();
   }

   private void resetTransientState() {
      this.lastHitTime = -1L;
      this.lastHitNanos = -1L;
      this.soundPending = false;
   }

   @EventHandler
   private void onHudRender(HudRenderEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      if (this.soundPending) {
         this.soundPending = false;
         Boolean soundEnabled = this.soundSetting.getValue();
         if (soundEnabled) {
            SoundManager soundManagerCapture = ClientContext.soundManager;
            NamedIdentifier soundIdentifierCapture = this.soundTypeSetting.getValue();
            float volume = (this.volumeSetting.getValue());
            soundManagerCapture.playSound(soundIdentifierCapture, volume);
         }
      }
      if (ClientAccess.schizoMinecraft.options.hideGui) {
         return;
      }
      GameRenderer gameRenderer = ClientAccess.schizoMinecraft.gameRenderer;
      Camera camera = gameRenderer.getMainCamera();
      if (camera.isDetached()) {
         return;
      }
      if (this.lastHitNanos < 0L) {
         return;
      }

      long elapsedMillis = this.elapsedHitMillis();
      float durationCapture = (this.timeSetting.getValue());

      long durationMillis = (long)(durationCapture * 1e+03F);
      if (elapsedMillis > durationMillis) {

         this.lastHitTime = -1L;
         this.lastHitNanos = -1L;
         return;
      }

      float opacity = this.getMarkerOpacity();
      if (!(opacity > 0.0F)) {
         return;
      }
      Boolean drawEnabled = this.drawSetting.getValue();
      if (!drawEnabled) {
         return;
      }
      float scaledWidth = (ClientAccess.schizoMinecraft.getWindow()).getGuiScaledWidth();

      float centerX = scaledWidth / 2.0F;
      float scaledHeight = (ClientAccess.schizoMinecraft.getWindow()).getGuiScaledHeight();

      float halfHeight = scaledHeight / 2.0F;

      float centerY = halfHeight - 0.5F;
      CrosshairModule crosshairForOffset = crosshair;
      boolean customCrosshair = crosshairForOffset != null && crosshairForOffset.isToggled();
      if (!customCrosshair) {

         centerX -= 0.5F;
      } else {

         centerY += 0.5F;
      }

      Float lengthValue = this.lengthSetting.getValue();
      float outerOffset = 2.0F + lengthValue;
      Setting<Color> colorOption = this.colorSetting;
      Color markerColor = ColorUtils.multiplyAlpha(
         colorOption.getValue(), opacity
      );
      VertexConsumer buffer = GuiDrawUtils.getLineVertices();
      PoseStack markerPoseStack = event.getPoseStack();
      markerPoseStack.pushPose();
      try {
         if (customCrosshair) {
            markerPoseStack.translate(-0.6D, 0.0D, 0.0D);
         }

      PoseStack topLeftStackCapture = markerPoseStack;
      float topLeftOuterX = centerX - outerOffset;
      float lineYCapture = centerY - outerOffset;

      float lineXCapture = centerX - 2.0F;

      float topLeftEndY = centerY - 2.0F;
      drawMarkerLine(
         topLeftStackCapture, buffer, markerColor, topLeftOuterX, lineYCapture, lineXCapture, topLeftEndY
      );
      PoseStack bottomLeftStackCapture = markerPoseStack;
      float bottomLeftOuterX = centerX - outerOffset;
      lineYCapture = centerY + outerOffset;

      lineXCapture = centerX - 2.0F;

      float bottomLeftEndY = centerY + 2.0F;
      drawMarkerLine(
         bottomLeftStackCapture, buffer, markerColor, bottomLeftOuterX, lineYCapture, lineXCapture, bottomLeftEndY
      );
      PoseStack topRightStackCapture = markerPoseStack;

      float topRightInnerX = centerX + 2.0F;

      lineYCapture = centerY - 2.0F;
      lineXCapture = centerX + outerOffset;
      float topRightEndY = centerY - outerOffset;
      drawMarkerLine(
         topRightStackCapture, buffer, markerColor, topRightInnerX, lineYCapture, lineXCapture, topRightEndY
      );
      PoseStack bottomRightStackCapture = markerPoseStack;

      float bottomRightInnerX = centerX + 2.0F;

      lineYCapture = centerY + 2.0F;
      lineXCapture = centerX + outerOffset;
      float bottomRightEndY = centerY + outerOffset;
      drawMarkerLine(
         bottomRightStackCapture, buffer, markerColor, bottomRightInnerX, lineYCapture, lineXCapture, bottomRightEndY
      );
      } finally {
         markerPoseStack.popPose();
      }

   }

   private static void drawMarkerLine(
      PoseStack poseStack, VertexConsumer buffer, Color color, float startX, float startY, float endX, float endY
   ) {
      VertexGeometry.appendLine(buffer, poseStack.last(), startX, startY, 0.0F, endX, endY, 0.0F, 1.0F, color.hashCode());
   }

   @EventHandler(priority = -100)
   private void onConnectionSendPacketEvent(
      PacketEvent.Send event
   ) {
      if (this.isWorldUnavailable()) {
         return;
      }
      if (!(event.getPacket() instanceof ServerboundInteractPacket packet)) {
         return;
      }
      if (InteractPacketUtils.getAction(packet) != InteractPacketUtils.Action.ATTACK) {
         return;
      }
      ClientLevel levelCapture = ClientAccess.schizoMinecraft.level;
      int entityId = ((ServerboundInteractPacketAccessor)packet).getEntityId();
      Entity target = levelCapture.getEntity(entityId);
      long elapsedMillis = this.elapsedHitMillis();

      if (elapsedMillis <= 100L) {
         return;
      }

      boolean simulatedPlayer = InteractPacketUtils.getEntity(packet) instanceof FakeplayerCommand.SimulatedPlayer;
      if (event.isCancelled() && !simulatedPlayer) {
         return;
      }

      if (target == null) {
         return;
      }
      if (target.level() == null || target == ClientAccess.schizoMinecraft.player) {
         return;
      }
      Level targetLevel = target.level();
      if (!targetLevel.isClientSide()) {
         return;
      }
      if (this.isEligibleTarget(target)) {
         this.lastHitTime = System.currentTimeMillis();
         this.lastHitNanos = System.nanoTime();
         this.soundPending = true;
      }
   }

   public static void bindCrosshair(ModuleRegistry registry) {
      crosshair = (CrosshairModule)registry.getModule(
         CrosshairModule.class
      );
   }

   public HitmarkerModule() {
      super("Hitmarker", "Marks your screen when you hit entities.", ModuleCategory.RENDER, new String[0]);
      this.drawSetting = this.add(new BooleanSetting("Draw", true).markVisibilityDependency());
      Predicate<Object> lengthVisibility = ignored -> this.drawSetting.isExpanded();
      this.lengthSetting = this.add(new NumberSetting<>("Length", 3.0F, 0.1F, 10.0F, lengthVisibility));
      Predicate<Object> timeVisibility = ignored -> this.drawSetting.isExpanded();
      this.timeSetting = this.add(new NumberSetting<>("Time", 1.0F, 0.5F, 3.0F, timeVisibility).withUnit("s"));
      Predicate<Object> fadeTimeVisibility = ignored -> this.drawSetting.isExpanded();
      this.fadeTimeSetting = this.add(new NumberSetting<>("FadeTime", 0.5F, 0.5F, 3.0F, fadeTimeVisibility).withUnit("s"));
      Predicate<Object> colorVisibility = ignored -> this.drawSetting.isExpanded();
      this.colorSetting = this.add(new ColorSetting("Color", new Color(-1581409793, true), colorVisibility));
      this.soundSetting = this.add(new BooleanSetting("Sound", false).markVisibilityDependency());
      this.soundTypeSetting = this.add(new IdentifierSetting("Sound", BuiltinSound.NEVERLOSE.getIdentifier()).dependsOn(this.soundSetting));
      Predicate<Object> volumeVisibility = ignored -> this.soundSetting.isExpanded();
      this.volumeSetting = this.add(new NumberSetting<>("Volume", 0.5F, 0.0F, 1.0F, volumeVisibility));
      this.targetsSetting = this.add(new BooleanSetting("Targets", false).markAsGroup().markVisibilityDependency());
      Predicate<Object> animalsVisibility = ignored -> this.targetsSetting.isExpanded();
      this.animalsSetting = this.add(new BooleanSetting("Animals", false, animalsVisibility));
      Predicate<Object> hostilesVisibility = ignored -> this.targetsSetting.isExpanded();
      this.hostilesSetting = this.add(new BooleanSetting("Hostiles", false, hostilesVisibility));
      Predicate<Object> playersVisibility = ignored -> this.targetsSetting.isExpanded();
      this.playersSetting = this.add(new BooleanSetting("Players", true, playersVisibility));
      Predicate<Object> crystalsVisibility = ignored -> this.targetsSetting.isExpanded();
      this.crystalsSetting = this.add(new BooleanSetting("Crystals", false, crystalsVisibility));

      this.lastHitTime = -1L;
      this.soundPending = false;

      this.setDrawn(false);
      this.soundTypeSetting.setConfigName("Type");
   }

}
