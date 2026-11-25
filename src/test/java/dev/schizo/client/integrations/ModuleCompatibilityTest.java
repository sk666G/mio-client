package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.client.CapesModule;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.BaritoneModule;
import dev.schizo.client.modules.client.NotificationsModule;
import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.combat.AntiPhaseModule;
import dev.schizo.client.modules.combat.CriticalsModule;
import dev.schizo.client.modules.exploit.AntiHungerModule;
import dev.schizo.client.modules.exploit.PhaseModule;
import dev.schizo.client.modules.exploit.PingSpoofModule;
import dev.schizo.client.modules.exploit.PortalGodModeModule;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.hud.ArmorModule;
import dev.schizo.client.modules.hud.ChatModule;
import dev.schizo.client.modules.hud.CryptoModule;
import dev.schizo.client.modules.hud.DirectionModule;
import dev.schizo.client.modules.hud.EffectsModule;
import dev.schizo.client.modules.hud.EntityListModule;
import dev.schizo.client.modules.hud.FrameTimeGraphModule;
import dev.schizo.client.modules.hud.GraphModule;
import dev.schizo.client.modules.hud.IndicatorModule;
import dev.schizo.client.modules.hud.InventoryModule;
import dev.schizo.client.modules.hud.LagOMeterModule;
import dev.schizo.client.modules.hud.MapModule;
import dev.schizo.client.modules.hud.MetricsModule;
import dev.schizo.client.modules.hud.ModuleListModule;
import dev.schizo.client.modules.hud.MusicModule;
import dev.schizo.client.modules.hud.PlayerModelModule;
import dev.schizo.client.modules.hud.PositionModule;
import dev.schizo.client.modules.hud.TextRadarModule;
import dev.schizo.client.modules.hud.TotemsModule;
import dev.schizo.client.modules.hud.WatermarkModule;
import dev.schizo.client.modules.hud.WelcomerModule;
import dev.schizo.client.modules.misc.*;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.modules.movement.FakeLagModule;
import dev.schizo.client.modules.movement.FastClimbModule;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.modules.movement.WarpModule;
import dev.schizo.client.modules.player.FastPlaceModule;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.modules.render.AmbienceModule;
import dev.schizo.client.modules.render.AnimationsModule;
import dev.schizo.client.modules.render.ChamsModule;
import dev.schizo.client.modules.render.ESPModule;
import dev.schizo.client.modules.render.HitmarkerModule;
import dev.schizo.client.modules.render.NameTagsModule;
import dev.schizo.client.modules.render.ParticlesModule;
import dev.schizo.client.modules.render.ShaderModule;
import dev.schizo.client.modules.render.TracersModule;
import dev.schizo.client.modules.render.FreeLookModule;
import dev.schizo.client.modules.render.NoBobModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.ViewModelModule;
import dev.schizo.client.modules.render.XrayModule;
import dev.schizo.client.modules.render.LogoutSpotsModule;
import dev.schizo.client.modules.render.SearchModule;
import dev.schizo.client.modules.render.WaypointsModule;
import dev.schizo.client.modules.render.ViewClipModule;
import dev.schizo.client.modules.render.BlurModule;
import dev.schizo.client.modules.render.BordersModule;
import dev.schizo.client.modules.render.BreakHighlightModule;
import dev.schizo.client.modules.render.CrosshairModule;
import dev.schizo.client.modules.render.GlintModule;
import dev.schizo.client.modules.render.HighlightModule;
import dev.schizo.client.modules.render.HoleESPModule;
import dev.schizo.client.modules.render.PhaseESPModule;
import dev.schizo.client.modules.render.SkeletonModule;
import dev.schizo.client.modules.render.SkyboxModule;
import dev.schizo.client.modules.render.TooltipsModule;
import dev.schizo.client.modules.render.TrailsModule;
import dev.schizo.client.modules.render.TrajectoriesModule;
import dev.schizo.client.modules.render.VoidESPModule;
import dev.schizo.client.modules.render.ZoomModule;
import dev.schizo.client.modules.render.TunnelsModule;
import org.junit.jupiter.api.Test;

class ModuleCompatibilityTest {
   @Test
   void classifiesEveryFirstPassModuleByExactClass() {
      assertClassification(SettingsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiCheatModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ColorsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(FontsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(HUDModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(UIModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(CapesModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(DiscordModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(NotificationsModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(BaritoneModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AmbienceModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(AnimationsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ChamsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ESPModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HitmarkerModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(NameTagsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ParticlesModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ShaderModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TracersModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(NoBobModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(NoRenderModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ViewModelModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(XrayModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(LogoutSpotsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(SearchModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(WaypointsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(BlurModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(BordersModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(BreakHighlightModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(CrosshairModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(GlintModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HighlightModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HoleESPModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(PhaseESPModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(SkeletonModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(SkyboxModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TooltipsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TrailsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TrajectoriesModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(VoidESPModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TunnelsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(dev.schizo.client.modules.movement.NoFallModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(FreeLookModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(ReachModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(PhaseModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiPhaseModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(ElytraFlyModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(CriticalsModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(NoSlowModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FastClimbModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(ZoomModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(ViewClipModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(FastPlaceModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(SpeedMineModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AntiHungerModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(PingSpoofModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(FakeLagModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(PortalGodModeModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(WarpModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
   }

   @Test
   void classifiesEveryHudWidgetAsSourceReviewedVisualOnly() {
      Class<?>[] hudModules = {
         ArmorModule.class,
         ChatModule.class,
         CryptoModule.class,
         DirectionModule.class,
         EffectsModule.class,
         EntityListModule.class,
         FrameTimeGraphModule.class,
         GraphModule.class,
         IndicatorModule.class,
         InventoryModule.class,
         LagOMeterModule.class,
         MapModule.class,
         MetricsModule.class,
         ModuleListModule.class,
         MusicModule.class,
         PlayerModelModule.class,
         PositionModule.class,
         TextRadarModule.class,
         TotemsModule.class,
         WatermarkModule.class,
         WelcomerModule.class
      };

      for (Class<?> moduleType : hudModules) {
         assertClassification(moduleType, ModuleCompatibility.Classification.VISUAL_ONLY);
      }
   }

   @Test
   void classifiesReviewedMiscModulesWithoutGuessingSwing() {
      assertClassification(AnnouncerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AntiAFKModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiAimModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiCyrillicModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AntiQuitModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(AntiSpamModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AutoReconnectModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AutoRespawnModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoSignModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(BetterChatModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ChatFilterModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(ChestSearchBarModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(CoordLoggerModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(CustomDeathTextModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ExtraScreenshotModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(ExtraTabModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HeavenModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(KillEffectsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(MiddleClickModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(NoNarratorModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(NoPacketKickModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(NoRotateModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(SkinFlickerModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(SoundBlockerModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(SpammerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(StashFinderModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(VisualRangeModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(WebhookNotifsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);

      ModuleCompatibility.Assessment swing = ModuleCompatibility.assess(SwingModule.class);
      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, swing.classification());
      assertFalse(swing.sourceReviewed());
   }

   @Test
   void unknownClassesDefaultToUnverified() {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(UnreviewedModule.class);

      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, assessment.classification());
      assertFalse(assessment.sourceReviewed());
      assertTrue(assessment.requiresMultiplayerWarning());
   }

   @Test
   void sameSimpleNameCannotInheritAnotherClassCompatibilityLabel() {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(NoFallModule.class);

      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, assessment.classification());
      assertFalse(assessment.sourceReviewed());
   }

   @Test
   void reviewsMixedClientModuleSettingsIndividually() {
      assertReviewedSetting(
         NotificationsModule.class, "Watermark",
         ModuleCompatibility.Classification.CLIENT_ONLY
      );
      assertReviewedSetting(
         NotificationsModule.class, "SayChat",
         ModuleCompatibility.Classification.WORLD_INTERACTION
      );
      assertReviewedSetting(
         BaritoneModule.class, "BackFill",
         ModuleCompatibility.Classification.WORLD_INTERACTION
      );
      assertReviewedSetting(
         BaritoneModule.class, "Color",
         ModuleCompatibility.Classification.VISUAL_ONLY
      );
   }

   @Test
   void exactReviewedSettingsDoNotCertifyFutureOrUnknownSettings() {
      ModuleCompatibility.SettingAssessment cape =
         ModuleCompatibility.assessSetting(CapesModule.class, "Cape");
      assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, cape.classification());
      assertTrue(cape.individuallyReviewed());

      for (String name : new String[]{"Verticals", "LineWidth", "Height", "MinLength", "Fill", "Outline"}) {
         ModuleCompatibility.SettingAssessment tunnel =
            ModuleCompatibility.assessSetting(TunnelsModule.class, name);
         assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, tunnel.classification(), name);
         assertTrue(tunnel.individuallyReviewed(), name);
      }

      ModuleCompatibility.SettingAssessment unknown =
         ModuleCompatibility.assessSetting(TunnelsModule.class, "FutureSetting");
      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, unknown.classification());
      assertFalse(unknown.individuallyReviewed());
   }

   @Test
   void neverClaimsASettingWasReviewedWhenOnlyItsParentModuleWasReviewed() {
      ModuleCompatibility.SettingAssessment assessment =
         ModuleCompatibility.assessSetting(ReachModule.class, "mode");

      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, assessment.classification());
      assertEquals(ModuleCompatibility.Classification.SERVER_SENSITIVE, assessment.inheritedModuleClassification());
      assertFalse(assessment.individuallyReviewed());
      assertTrue(assessment.requiresWarning());
   }

   private static void assertReviewedSetting(
      Class<?> moduleType,
      String settingName,
      ModuleCompatibility.Classification expected
   ) {
      ModuleCompatibility.SettingAssessment assessment =
         ModuleCompatibility.assessSetting(moduleType, settingName);
      assertEquals(expected, assessment.classification(), moduleType.getName() + "/" + settingName);
      assertTrue(assessment.individuallyReviewed(), moduleType.getName() + "/" + settingName);
      assertTrue(assessment.reason() != null && !assessment.reason().isBlank());
   }

   private static void assertClassification(
      Class<?> moduleType,
      ModuleCompatibility.Classification expected
   ) {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(moduleType);
      assertEquals(expected, assessment.classification(), moduleType.getName());
      assertTrue(assessment.sourceReviewed(), moduleType.getName());
      assertTrue(assessment.reason() != null && !assessment.reason().isBlank(), moduleType.getName());
   }

   private static final class NoFallModule {}
   private static final class UnreviewedModule {}
}
