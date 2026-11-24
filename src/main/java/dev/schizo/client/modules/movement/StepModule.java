/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;


import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.StepHeightEvent;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.managers.HoleManager;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

public class StepModule extends Module {
   private static HoleSnapModule holeSnap;
   final Setting<ModeChoice> modeSetting;
   private final Setting<Float> heightSetting;
   private final Setting<Boolean> safeDisableSetting;
   final Setting<Boolean> useTimerSetting;
   private final Setting<Float> timerSpeedSetting;
   private boolean usingStepTimer;
   private int stepCount;

   @Override
   public String getInfo() {
      return FontRenderer.capitalize(this.modeSetting.getValue());
   }

   @Override
   public void onDisable() {
      this.releaseStepTimer();
      this.stepCount = 0;
   }

   private void releaseStepTimer() {
      ClientContext.tickRateController.releaseMultiplier(this);
      this.usingStepTimer = false;
   }

   static boolean shouldRequestStepTimer(boolean normalMode, boolean timerEnabled, boolean inCobweb) {
      return normalMode && timerEnabled && !inCobweb;
   }

   @EventHandler(priority = -100)
   private void onLocalPlayerPreTickEvent(LocalPlayerPreTickEvent event) {
      if (this.stepCount <= 0) {
         return;
      }
      HoleManager holes = ClientContext.holeManager;
      BlockPos position = PlayerStateUtils.getLocalRoundedBlockPosition();
      if (!holes.intersectsHole(position)) {
         return;
      }
      if (this.safeDisableSetting.getValue()) {
         this.disable();
      }
   }

   @EventHandler
   private void onStepHeight(StepHeightEvent event) {
      if (holeSnap.shouldPauseStep()) {
         this.releaseStepTimer();
         return;
      }
      if (!ClientAccess.schizoMinecraft.player.onGround() || PlayerStateUtils.getTrackedFallDistance() > 0.0F
         || !ClientAccess.schizoMinecraft.player.verticalCollision) {
         this.releaseStepTimer();
         return;
      }
      if (EventStage.Pre == event.getStage()) {
         Setting<Float> height = this.heightSetting;
         float requestedHeight = height.getValue();
         event.setHeight(requestedHeight);
      }

      if (event.getStage() != EventStage.Post) {
         return;
      }

      double actualHeight = event.getHeight();
      if (actualHeight <= 0.6D) {
         this.releaseStepTimer();
         return;
      }

      if (this.modeSetting.getValue() == StepModule.ModeChoice.VANILLA) {
         this.releaseStepTimer();
         this.stepCount++;
         return;
      }

      ArrayList<Double> offsets = new ArrayList<>();
      boolean requestTimer = shouldRequestStepTimer(
         this.modeSetting.getValue() == StepModule.ModeChoice.NORMAL,
         this.useTimerSetting.getValue(),
         PlayerStateUtils.isInCobweb(ClientAccess.schizoMinecraft.player)
      );
      if (requestTimer) {
         float requestedSpeed = this.timerSpeedSetting.getValue();
         ClientContext.tickRateController.requestMultiplier(this, requestedSpeed);
         this.usingStepTimer = true;
      } else {
         this.releaseStepTimer();
      }

      this.stepCount++;
      float height = event.getHeight();
      if (height <= 1.0F) {
         offsets.addAll(List.of(0.42D * height, 0.75D * height));
      } else if (height <= 1.5D) {
         offsets.addAll(List.of(0.42D, 0.75D, 1.0D, 1.16D, 1.23D, 1.2D));
      } else if (height <= this.heightSetting.getValue()) {
         offsets.addAll(List.of(0.42D, 0.78D, 0.63D, 0.51D, 0.9D, 1.21D, 1.45D, 1.43D));
      }

      if (offsets.isEmpty()) {
         return;
      }
      for (Double offset : offsets) {
         double x = ClientAccess.schizoMinecraft.player.getX();
         double baseY = ClientAccess.schizoMinecraft.player.getY();
         double y = baseY + offset;
         double z = ClientAccess.schizoMinecraft.player.getZ();
         boolean onGround = ClientAccess.schizoMinecraft.player.onGround();
         PacketSender.sendPosition(x, y, z, onGround);
      }
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.usingStepTimer = false;
   }

   public boolean isUsingTimer() {
      return this.usingStepTimer;
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      holeSnap = (HoleSnapModule)registry.getModule(HoleSnapModule.class);
   }

   public StepModule() {
      super("Step", "Allows you to step up blocks without jumping.", ModuleCategory.MOVEMENT, new String[0]);
      this.modeSetting = this.add(new EnumSetting<>("Mode", StepModule.ModeChoice.VANILLA));
      this.heightSetting = this.add(new NumberSetting<>("Height", 2.0F, 1.0F, 4.0F).withUnit("m"));
      this.safeDisableSetting = this.add(new BooleanSetting("SafeDisable", false));
      Predicate<Object> timerVisibility = ignored -> this.modeSetting.getValue() == ModeChoice.NORMAL;
      this.useTimerSetting = this.add(new BooleanSetting("UseTimer", true, timerVisibility).markVisibilityDependency());
      Predicate<Object> timerSpeedVisibility = ignored -> this.modeSetting.getValue() == ModeChoice.NORMAL && this.useTimerSetting.isExpanded();
      this.timerSpeedSetting = this.add(new NumberSetting<>("TimerSpeed", 0.3F, 0.01F, 1.0F, timerSpeedVisibility));
      this.stepCount = 0;
      this.modeSetting.setChangeCallback(this::releaseStepTimer);
   }

   public enum ModeChoice implements NamedEntry {
      VANILLA("Vanilla"),
      NORMAL("Normal");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      ModeChoice(String displayName) {
         this.displayName = displayName;
      }
   }
}
