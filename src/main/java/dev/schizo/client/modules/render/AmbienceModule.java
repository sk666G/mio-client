/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;


import java.util.function.Predicate;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.render.ShaderPass;
import dev.schizo.client.managers.ShaderRegistry;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.GameOptionsOverrides;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.schizo.mixin.DimensionTypeAccessor;
import java.awt.Color;
import java.util.Calendar;
import java.util.function.Supplier;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome.Precipitation;

public class AmbienceModule extends Module {
   public final Setting<BrightnessChoice> brightnessSetting;
   public final Setting<Color> colorSetting;
   public final Setting<Boolean> occlusionSetting;
   public final Setting<Color> occlusionColorSetting;
   public final Setting<Boolean> worldTimeSetting;
   final Setting<Boolean> syncSetting;
   private final Setting<Float> timeSetting;
   public final Setting<Boolean> worldWeatherSetting;
   public final Setting<WeatherChoice> weatherSetting;
   public final Setting<Boolean> weatherAlwaysSetting;
   public final Setting<Integer> opacitySetting;
   public final Setting<Boolean> fieldDepthSetting;
   public final Setting<Float> strengthSetting;
   private RenderTarget depthRenderTarget;
   private AmbienceModule.BrightnessChoice previousBrightness;
   private double savedGamma;
   private boolean gammaOverrideApplied;
   private MobEffectInstance appliedNightVisionEffect;
   private boolean weatherOverrideSuppressed;
   private ServerTimeSnapshot serverTimeSnapshot;

   @Override
   public void onEnable() {
      this.gammaOverrideApplied = false;
      this.appliedNightVisionEffect = null;
      Options options = ClientAccess.schizoMinecraft.options;
      OptionInstance<Double> gammaOption = options.gamma();
      this.savedGamma = gammaOption.get();
      if (ClientAccess.schizoMinecraft.level != null) {
         this.serverTimeSnapshot = new ServerTimeSnapshot(
            ClientAccess.schizoMinecraft.level.getGameTime(), ClientAccess.schizoMinecraft.level.getDayTime()
         );
      } else {
         this.serverTimeSnapshot = null;
      }
   }

   @Override
   public void onToggle() {
      if (ClientAccess.schizoMinecraft.levelRenderer != null) {
         Setting<BrightnessChoice> brightnessOption = this.brightnessSetting;
         if (brightnessOption.getValue() == AmbienceModule.BrightnessChoice.SCREEN) {
            LevelRenderer levelRenderer = ClientAccess.schizoMinecraft.levelRenderer;
            levelRenderer.allChanged();
         }
      }
   }

   @Override
   public void onDisable() {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (minecraft == null) {
         this.appliedNightVisionEffect = null;
         return;
      }
      if (minecraft.player != null) {
         this.removeOwnedNightVision(minecraft.player);
      } else {
         this.appliedNightVisionEffect = null;
      }

      if (this.serverTimeSnapshot != null && minecraft.level != null) {
         minecraft.level.setTimeFromServer(
            this.serverTimeSnapshot.gameTime(), this.serverTimeSnapshot.dayTime(), true
         );
      }

      this.restoreGammaIfOwned(minecraft);
   }

   public void updateServerTimeSnapshot(long gameTime, long dayTime) {
      this.serverTimeSnapshot = new ServerTimeSnapshot(gameTime, dayTime);
   }

   public long getWorldTime() {
      Setting<Boolean> syncOption = this.syncSetting;
      if (syncOption.getValue()) {
         Calendar calendar = Calendar.getInstance();

         float timeTicks = calendar.get(11);

         float minutes = calendar.get(12);

         timeTicks += minutes / 6e+01F;

         timeTicks *= 1e+03F;

         return (long)(timeTicks + 1.8e+04F);
      } else {
         float configuredTimeTicks = this.timeSetting.getValue();

         configuredTimeTicks *= 1e+03F;

         return (long)(configuredTimeTicks + 1.8e+04F);
      }
   }

   public RenderTarget getDepthRenderTarget() {
      return this.depthRenderTarget;
   }

   public boolean isWeatherOverrideSuppressed() {
      return this.weatherOverrideSuppressed;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      if (this.isWorldUnavailable()) {
         return;
      }
      Setting<Boolean> worldTimeOption = this.worldTimeSetting;
      if (worldTimeOption.getValue()) {

         long worldTime = this.getWorldTime();
         DimensionTypeAccessor dimensionType = (DimensionTypeAccessor)(Object)ClientAccess.schizoMinecraft.level.dimensionType();
         ClientAccess.schizoMinecraft.level.setTimeFromServer(worldTime, worldTime, false);
         if (dimensionType != null) {
            dimensionType.schizo$setFixedTime(false);
         }
      }

      Setting<BrightnessChoice> gammaBrightnessOption = this.brightnessSetting;
      if (gammaBrightnessOption.getValue() == AmbienceModule.BrightnessChoice.GAMMA) {
         Options options = ClientAccess.schizoMinecraft.options;
         OptionInstance<Double> gammaOption = options.gamma();
         if (!this.gammaOverrideApplied) {
            this.savedGamma = gammaOption.get();
            this.gammaOverrideApplied = true;
         }
         GameOptionsOverrides.forceSetOptionValue(gammaOption, 1000.0D);
      } else {
         Setting<BrightnessChoice> potionBrightnessOption = this.brightnessSetting;
         if (potionBrightnessOption.getValue() == AmbienceModule.BrightnessChoice.POTION) {
            LocalPlayer playerToCheck = ClientAccess.schizoMinecraft.player;
            Holder effectToCheck = MobEffects.NIGHT_VISION;
            if (!playerToCheck.hasEffect(effectToCheck)) {
               LocalPlayer effectPlayerCapture = ClientAccess.schizoMinecraft.player;
               MobEffectInstance nightVisionEffect = new MobEffectInstance(
                  MobEffects.NIGHT_VISION, -1, getNightVisionAmplifier()
               );
               effectPlayerCapture.addEffect(nightVisionEffect);
               MobEffectInstance appliedEffect = effectPlayerCapture.getEffect(MobEffects.NIGHT_VISION);
               this.appliedNightVisionEffect = appliedEffect != null
                  && appliedEffect.getAmplifier() == getNightVisionAmplifier()
                  ? appliedEffect
                  : null;
            }
         }
      }
   }

   private void restoreGammaIfOwned(Minecraft minecraft) {
      boolean owned = this.gammaOverrideApplied;
      this.gammaOverrideApplied = false;
      if (!owned || minecraft == null || minecraft.options == null) {
         return;
      }

      OptionInstance<Double> gammaOption = minecraft.options.gamma();
      double currentGamma = gammaOption.get();
      if (shouldRestoreGamma(owned, currentGamma, 1000.0D)) {
         GameOptionsOverrides.forceSetOptionValue(gammaOption, this.savedGamma);
      }
   }

   static boolean shouldRestoreGamma(
      boolean overrideApplied,
      double currentGamma,
      double lastAppliedGamma
   ) {
      return overrideApplied && Double.compare(currentGamma, lastAppliedGamma) == 0;
   }

   private void removeOwnedNightVision(LocalPlayer player) {
      MobEffectInstance ownedEffect = this.appliedNightVisionEffect;
      this.appliedNightVisionEffect = null;
      if (ownedEffect == null) {
         return;
      }

      MobEffectInstance currentEffect = player.getEffect(MobEffects.NIGHT_VISION);
      if (currentEffect == ownedEffect
         && currentEffect.getAmplifier() == getNightVisionAmplifier()) {
         player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   private static int getNightVisionAmplifier() {
      return 68;
   }

   @EventHandler(priority = 10)
   public void onBeforeDepthClear(WorldRenderEvent.BeforeDepthClear event) {
      Setting<Boolean> depthOption = this.fieldDepthSetting;
      if (depthOption.getValue()) {
         this.depthRenderTarget = ClientAccess.schizoMinecraft.getMainRenderTarget();
         RenderUtils renderUtils = RenderUtils.INSTANCE;
         renderUtils.fillRect(
            event.getPoseStack(), 0.0F, 0.0F, 0.0F, 0.0F, -1
         );
         ShaderRegistry registry = ClientContext.shaderRegistry;
         String shaderName = "dof";
         ShaderPass depthPass = registry.getPass(shaderName);
         Runnable depthDraw = () -> {};
         depthPass.render(depthDraw);
      }
   }

   public Object withWeatherOverrideSuppressed(Supplier supplier) {
      boolean previous = this.weatherOverrideSuppressed;
      this.weatherOverrideSuppressed = true;
      try {
         return supplier.get();
      } finally {
         this.weatherOverrideSuppressed = previous;
      }
   }

   public AmbienceModule() {
      super(
         "Ambience",
         "Changes several ambient things.",
         ModuleCategory.RENDER,
         new String[]{"fullbright"}
      );
      this.brightnessSetting = this.add(new EnumSetting<>("Brightness", AmbienceModule.BrightnessChoice.GAMMA));
      Predicate<Object> screenColorVisibility = ignored -> this.brightnessSetting.getValue() == AmbienceModule.BrightnessChoice.SCREEN;
      this.colorSetting = this.add(new ColorSetting("Color", new Color(-1, true), screenColorVisibility));
      Predicate<Object> occlusionVisibility = ignored -> (this.brightnessSetting.getValue()).supportsOcclusion();
      this.occlusionSetting = this.add(new BooleanSetting("Occlusion", false, occlusionVisibility).markVisibilityDependency());
      this.occlusionColorSetting = this.add(new ColorSetting("Color", new Color(-1, true)).dependsOn(this.occlusionSetting));
      this.worldTimeSetting = this.add(new BooleanSetting("WorldTime", false).markVisibilityDependency());
      this.syncSetting = this.add(new BooleanSetting("Sync", false).dependsOn(this.worldTimeSetting));
      Predicate<Object> timeVisibility = ignored -> this.worldTimeSetting.isExpanded() && !this.syncSetting.getValue();
      this.timeSetting = this.add(new NumberSetting<>("Time", 0.0F, 0.0F, 24.0F, timeVisibility).withUnit("h"));
      this.worldWeatherSetting = this.add(new BooleanSetting("WorldWeather", false).markVisibilityDependency());
      this.weatherSetting = this.add(new EnumSetting<>("Weather", AmbienceModule.WeatherChoice.CLEAR).dependsOn(this.worldWeatherSetting));
      Predicate<Object> alwaysVisibility = ignored -> (this.weatherSetting.getValue()).supportsPrecipitation();
      this.weatherAlwaysSetting = this.add(new BooleanSetting("Always", false, alwaysVisibility).dependsOn(this.worldWeatherSetting));
      this.opacitySetting = this.add(new NumberSetting<>("Opacity", 100, 0, 100).withUnit("%").dependsOn(this.worldWeatherSetting));
      this.fieldDepthSetting = this.add(new BooleanSetting("FieldDepth", false).markVisibilityDependency());
      this.strengthSetting = this.add(new NumberSetting<>("Strength", 10.0F, 1.0F, 20.0F).dependsOn(this.fieldDepthSetting));
      Setting<BrightnessChoice> brightnessForCallback = this.brightnessSetting;
      Runnable brightnessChangeCallback = () -> {
         Setting<BrightnessChoice> changedBrightnessOption = this.brightnessSetting;
         AmbienceModule.BrightnessChoice changedBrightness = changedBrightnessOption.getValue();
         if (!this.isToggled()) {
            this.previousBrightness = changedBrightness;
            return;
         }

         AmbienceModule.BrightnessChoice previousBrightnessCapture = this.previousBrightness;
         if (previousBrightnessCapture == AmbienceModule.BrightnessChoice.GAMMA
            && changedBrightness != AmbienceModule.BrightnessChoice.GAMMA) {
            this.restoreGammaIfOwned(ClientAccess.schizoMinecraft);
         }

         this.previousBrightness = changedBrightness;
         if (previousBrightnessCapture == AmbienceModule.BrightnessChoice.POTION
            && changedBrightness != AmbienceModule.BrightnessChoice.POTION
            && !this.isWorldUnavailable()) {
            this.removeOwnedNightVision(ClientAccess.schizoMinecraft.player);
         }
      };
      brightnessForCallback.setChangeCallback(brightnessChangeCallback);
      Setting<Boolean> worldTimeForCallback = this.worldTimeSetting;
      Runnable worldTimeChangeCallback = () -> {
         Setting<Boolean> worldTimeOption = this.worldTimeSetting;
         if (worldTimeOption.getValue()) {
            return;
         }
         Minecraft minecraft = ClientAccess.schizoMinecraft;
         if (this.serverTimeSnapshot != null && minecraft != null && minecraft.level != null) {
            minecraft.level.setTimeFromServer(
               this.serverTimeSnapshot.gameTime(), this.serverTimeSnapshot.dayTime(), false
            );
            this.serverTimeSnapshot = null;
         }
      };
      worldTimeForCallback.setChangeCallback(worldTimeChangeCallback);
      this.previousBrightness = this.brightnessSetting.getValue();
      this.occlusionColorSetting.setConfigName("OcclusionColor");

      this.setDrawn(false);
   }

   private record ServerTimeSnapshot(long gameTime, long dayTime) {
   }

   public enum BrightnessChoice implements NamedEntry {
      SCREEN("Screen"),
      GAMMA("Gamma"),
      POTION("Potion"),
      NONE("None");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      BrightnessChoice(String displayName) {
         this.displayName = displayName;
      }

      public boolean supportsOcclusion() {
         return this == NONE || this == POTION;
      }
   }

   public enum WeatherChoice implements NamedEntry {
      VANILLA("Vanilla", null),
      CLEAR("Clear", Precipitation.NONE),
      SNOW("Snow", Precipitation.SNOW),
      RAIN("Rain", Precipitation.RAIN);

      private final String displayName;
      private final Precipitation precipitation;

      public Precipitation getPrecipitation() {
         return this.precipitation;
      }

      public boolean supportsPrecipitation() {
         return this.getPrecipitation() != null && this.getPrecipitation() != Precipitation.NONE;
      }

      @Override
      public String getName() {
         return this.displayName;
      }

      WeatherChoice(String displayName, Precipitation precipitation) {
         this.displayName = displayName;
         this.precipitation = precipitation;
      }
   }
}
