package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.combat.AntiPhaseModule;
import dev.schizo.client.modules.combat.CriticalsModule;
import dev.schizo.client.modules.exploit.AntiHungerModule;
import dev.schizo.client.modules.exploit.PhaseModule;
import dev.schizo.client.modules.exploit.PingSpoofModule;
import dev.schizo.client.modules.exploit.PortalGodModeModule;
import dev.schizo.client.modules.exploit.ReachModule;
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
import dev.schizo.client.modules.misc.BetterChatModule;
import dev.schizo.client.modules.misc.AntiCyrillicModule;
import dev.schizo.client.modules.misc.AntiSpamModule;
import dev.schizo.client.modules.misc.NoNarratorModule;
import dev.schizo.client.modules.misc.SoundBlockerModule;
import dev.schizo.client.modules.misc.VisualRangeModule;
import dev.schizo.client.modules.player.NameProtectModule;
import dev.schizo.client.modules.client.NotificationsModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.misc.WebhookNotifsModule;
import dev.schizo.client.modules.misc.ExtraScreenshotModule;
import dev.schizo.client.modules.misc.ExtraTabModule;
import dev.schizo.client.modules.misc.ChestSearchBarModule;
import dev.schizo.client.modules.misc.CustomDeathTextModule;
import dev.schizo.client.modules.render.TunnelsModule;
import dev.schizo.client.modules.client.CapesModule;
import dev.schizo.client.modules.misc.ChatFilterModule;
import dev.schizo.client.modules.player.NoEffectsModule;
import dev.schizo.client.modules.player.NoGlitchBlocksModule;
import dev.schizo.client.modules.combat.NoHitDelayModule;
import dev.schizo.client.modules.player.NoInteractModule;
import dev.schizo.client.modules.movement.NoJumpDelayModule;
import dev.schizo.client.modules.exploit.NoMineAnimationModule;
import dev.schizo.client.modules.misc.NoRotateModule;
import dev.schizo.client.modules.misc.StashFinderModule;
import dev.schizo.client.modules.exploit.NewChunksModule;
import dev.schizo.client.modules.misc.KillEffectsModule;
import dev.schizo.client.modules.misc.SkinFlickerModule;
import dev.schizo.client.modules.misc.CoordLoggerModule;
import dev.schizo.client.modules.exploit.FakeVanillaModule;
import dev.schizo.client.modules.misc.AnnouncerModule;
import dev.schizo.client.modules.misc.AntiAFKModule;
import dev.schizo.client.modules.misc.AutoReconnectModule;
import dev.schizo.client.modules.misc.AutoRespawnModule;
import dev.schizo.client.modules.misc.SpammerModule;
import dev.schizo.client.modules.player.AutoFishModule;
import dev.schizo.client.modules.player.AutoFarmModule;
import dev.schizo.client.modules.player.InventorySorterModule;
import dev.schizo.client.modules.player.ChestStealerModule;
import dev.schizo.client.modules.player.InventoryCleanerModule;
import dev.schizo.client.modules.movement.FireworksModule;
import dev.schizo.client.modules.misc.MiddleClickModule;
import dev.schizo.client.modules.player.AutoBreedModule;
import dev.schizo.client.modules.player.AutoTameModule;
import dev.schizo.client.modules.player.AutoTradeModule;
import dev.schizo.client.modules.player.AutoCraftModule;
import dev.schizo.client.modules.combat.AnchorAuraModule;
import dev.schizo.client.modules.misc.AntiAimModule;
import dev.schizo.client.modules.combat.AntiBotModule;
import dev.schizo.client.modules.combat.AntiFeetPlaceModule;
import dev.schizo.client.modules.combat.AntiPearlModule;
import dev.schizo.client.modules.combat.AuraModule;
import dev.schizo.client.modules.combat.AutoArmorModule;
import dev.schizo.client.modules.combat.AutoBowReleaseModule;
import dev.schizo.client.modules.combat.AutoClickerModule;
import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.combat.AutoDropModule;
import dev.schizo.client.modules.combat.AutoExpModule;
import dev.schizo.client.modules.combat.AutoLogModule;
import dev.schizo.client.modules.combat.AutoMaceModule;
import dev.schizo.client.modules.combat.AutoMineModule;
import dev.schizo.client.modules.combat.AutoPotModule;
import dev.schizo.client.modules.combat.AutoTrapModule;
import dev.schizo.client.modules.combat.BedAuraModule;
import dev.schizo.client.modules.combat.BlockerModule;
import dev.schizo.client.modules.combat.BowAimModule;
import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.modules.exploit.AntiVanishModule;
import dev.schizo.client.modules.movement.AvoidModule;
import dev.schizo.client.modules.combat.ArrowsModule;
import dev.schizo.client.modules.player.AutoEatModule;
import dev.schizo.client.modules.player.AutoMountModule;
import dev.schizo.client.modules.player.AutoNameTagModule;
import dev.schizo.client.modules.misc.AutoSignModule;
import dev.schizo.client.modules.player.AutoToolModule;
import dev.schizo.client.modules.player.AutoTemplateModule;
import dev.schizo.client.modules.movement.AutoWalkModule;
import dev.schizo.client.modules.exploit.ChorusControlModule;
import dev.schizo.client.modules.movement.EntityControlModule;
import dev.schizo.client.modules.movement.FastFallModule;
import dev.schizo.client.modules.exploit.FastLatencyModule;
import dev.schizo.client.modules.movement.FastStairsModule;
import dev.schizo.client.modules.movement.FastSwimModule;
import dev.schizo.client.modules.movement.FastWebModule;
import dev.schizo.client.modules.combat.FeetPlaceModule;
import dev.schizo.client.modules.combat.FishTrapModule;
import dev.schizo.client.modules.movement.FlightModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.misc.HeavenModule;
import dev.schizo.client.modules.movement.HighJumpModule;
import dev.schizo.client.modules.exploit.HitboxDesyncModule;
import dev.schizo.client.modules.combat.HoleFillModule;
import dev.schizo.client.modules.movement.HoleSnapModule;
import dev.schizo.client.modules.exploit.IllegalDisconnectModule;
import dev.schizo.client.modules.player.InventoryTweaksModule;
import dev.schizo.client.modules.player.ItemSaverModule;
import dev.schizo.client.modules.movement.JesusModule;
import dev.schizo.client.modules.combat.KeyPearlModule;
import dev.schizo.client.modules.movement.LongJumpModule;
import dev.schizo.client.modules.combat.MainhandModule;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.misc.NoPacketKickModule;
import dev.schizo.client.modules.player.NukerModule;
import dev.schizo.client.modules.combat.OffhandModule;
import dev.schizo.client.modules.combat.PistonCrystalModule;
import dev.schizo.client.modules.combat.PusherModule;
import dev.schizo.client.modules.player.ReplenishModule;
import dev.schizo.client.modules.exploit.RocketExtenderModule;
import dev.schizo.client.modules.player.RotationLockModule;
import dev.schizo.client.modules.movement.SafeWalkModule;
import dev.schizo.client.modules.player.ScaffoldModule;
import dev.schizo.client.modules.combat.SelfFillModule;
import dev.schizo.client.modules.combat.SelfTrapModule;
import dev.schizo.client.modules.combat.SelfWebModule;
import dev.schizo.client.modules.player.SourceFillModule;
import dev.schizo.client.modules.movement.SpeedModule;
import dev.schizo.client.modules.movement.SprintModule;
import dev.schizo.client.modules.movement.StepModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.modules.exploit.TimerModule;
import dev.schizo.client.modules.exploit.TridentModule;
import dev.schizo.client.modules.movement.VelocityModule;
import dev.schizo.client.modules.combat.WebAuraModule;
import dev.schizo.client.modules.exploit.XCarryModule;
import org.junit.jupiter.api.Test;

class ModuleCompatibilityTest {
   @Test
   void classifiesEveryFirstPassModuleByExactClass() {
      assertClassification(SettingsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiCheatModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ColorsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(FontsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(HUDModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(BetterChatModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiSpamModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(NoNarratorModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(SoundBlockerModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(VisualRangeModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(NameProtectModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(UIModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(DiscordModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(WebhookNotifsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(ExtraScreenshotModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(ExtraTabModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ChestSearchBarModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(CustomDeathTextModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TunnelsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(AntiCyrillicModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(NotificationsModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(CapesModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ChatFilterModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(StashFinderModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(CoordLoggerModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(NewChunksModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(KillEffectsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(NoEffectsModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(NoHitDelayModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(NoJumpDelayModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(NoInteractModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(NoGlitchBlocksModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(NoMineAnimationModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(NoRotateModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(SkinFlickerModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(FakeVanillaModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AnnouncerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AntiAFKModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoReconnectModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AutoRespawnModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(SpammerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoFishModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoFarmModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(InventorySorterModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(ChestStealerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(InventoryCleanerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(FireworksModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(MiddleClickModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoBreedModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoTameModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoTradeModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoCraftModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AnchorAuraModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AntiAimModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiBotModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiFeetPlaceModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AntiPearlModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AuraModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoArmorModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AutoBowReleaseModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoClickerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoCrystalModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AutoDropModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoExpModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoLogModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AutoMaceModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AutoMineModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoPotModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoTrapModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(BedAuraModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(BlockerModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(BowAimModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiQuitModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiVanishModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AvoidModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(ArrowsModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AutoEatModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoMountModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoNameTagModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoSignModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoToolModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(AutoTemplateModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(AutoWalkModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(ChorusControlModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(EntityControlModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FastFallModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FastLatencyModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(FastStairsModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(FastSwimModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FastWebModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(FeetPlaceModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FishTrapModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(FlightModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FreecamModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(HeavenModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HighJumpModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(HitboxDesyncModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(HoleFillModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(HoleSnapModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(IllegalDisconnectModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(InventoryTweaksModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(ItemSaverModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(JesusModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(KeyPearlModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(LongJumpModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(MainhandModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(MultiTaskModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(NoPacketKickModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(NukerModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(OffhandModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(PistonCrystalModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(PusherModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(ReplenishModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(RocketExtenderModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(RotationLockModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(SafeWalkModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
      assertClassification(ScaffoldModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(SelfFillModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(SelfTrapModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(SelfWebModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(SourceFillModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(SpeedModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(SprintModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(StepModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(SwingModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TimerModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(TridentModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(VelocityModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(WebAuraModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(XCarryModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
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
   void reviewedServerSensitiveSettingKeepsItsOwnWarningClassification() {
      ModuleCompatibility.SettingAssessment assessment =
         ModuleCompatibility.assessSetting(TridentModule.class, "Always");

      assertEquals(ModuleCompatibility.Classification.SERVER_SENSITIVE, assessment.classification());
      assertEquals(ModuleCompatibility.Classification.SERVER_SENSITIVE, assessment.inheritedModuleClassification());
      assertTrue(assessment.individuallyReviewed());
      assertTrue(assessment.requiresWarning());
      assertTrue(assessment.reason().contains("riptide"));
   }

   @Test
   void reviewedVisualSettingDoesNotInheritAWarning() {
      ModuleCompatibility.SettingAssessment assessment =
         ModuleCompatibility.assessSetting(SwingModule.class, "Speed");

      assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, assessment.classification());
      assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, assessment.inheritedModuleClassification());
      assertTrue(assessment.individuallyReviewed());
      assertFalse(assessment.requiresWarning());
   }

   @Test
   void reviewedLocalDiagnosticSettingCanBeSaferThanItsParentModule() {
      ModuleCompatibility.SettingAssessment assessment =
         ModuleCompatibility.assessSetting(NoPacketKickModule.class, "Message");

      assertEquals(ModuleCompatibility.Classification.CLIENT_ONLY, assessment.classification());
      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, assessment.inheritedModuleClassification());
      assertTrue(assessment.individuallyReviewed());
      assertFalse(assessment.requiresWarning());
   }

   @Test
   void packetTimingSettingsRetainPacketAffectingClassification() {
      ModuleCompatibility.SettingAssessment pingDelay =
         ModuleCompatibility.assessSetting(PingSpoofModule.class, "Delay");
      ModuleCompatibility.SettingAssessment noFallMode =
         ModuleCompatibility.assessSetting(dev.schizo.client.modules.movement.NoFallModule.class, "Mode");

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, pingDelay.classification());
      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, noFallMode.classification());
      assertTrue(pingDelay.individuallyReviewed());
      assertTrue(noFallMode.individuallyReviewed());
      assertTrue(pingDelay.requiresWarning());
      assertTrue(noFallMode.requiresWarning());
   }

   @Test
   void reviewedPacketSelectorsAndVisualControlsStayDistinct() {
      ModuleCompatibility.SettingAssessment movement =
         ModuleCompatibility.assessSetting(FakeLagModule.class, "Movement");
      ModuleCompatibility.SettingAssessment render =
         ModuleCompatibility.assessSetting(FakeLagModule.class, "Render");
      ModuleCompatibility.SettingAssessment reach =
         ModuleCompatibility.assessSetting(ReachModule.class, "Modifier");
      ModuleCompatibility.SettingAssessment chunkPackets =
         ModuleCompatibility.assessSetting(NoRenderModule.class, "Chunks");

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, movement.classification());
      assertTrue(movement.requiresWarning());
      assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, render.classification());
      assertFalse(render.requiresWarning());
      assertEquals(ModuleCompatibility.Classification.SERVER_SENSITIVE, reach.classification());
      assertTrue(reach.requiresWarning());
      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, chunkPackets.classification());
      assertTrue(chunkPackets.requiresWarning());
   }

   @Test
   void settingReviewCountIncludesExplicitReviewsOnly() {
      assertTrue(ModuleCompatibility.reviewedSettingCount() >= 111);
   }

   @Test
   void fastLatencyUsesItsActualResponseConfigKey() {
      ModuleCompatibility.SettingAssessment response =
         ModuleCompatibility.assessSetting(FastLatencyModule.class, "Response");
      ModuleCompatibility.SettingAssessment proseOnlyAlias =
         ModuleCompatibility.assessSetting(FastLatencyModule.class, "FastResponse");

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, response.classification());
      assertTrue(response.individuallyReviewed());
      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, proseOnlyAlias.classification());
      assertFalse(proseOnlyAlias.individuallyReviewed());
   }

   @Test
   void packetUtilitySettingsRemainExplicitlyReviewed() {
      assertEquals(
         ModuleCompatibility.Classification.PACKET_AFFECTING,
         ModuleCompatibility.assessSetting(RocketExtenderModule.class, "Hold").classification()
      );
      assertEquals(
         ModuleCompatibility.Classification.PACKET_AFFECTING,
         ModuleCompatibility.assessSetting(NoRotateModule.class, "InBlocks").classification()
      );
      assertEquals(
         ModuleCompatibility.Classification.WORLD_INTERACTION,
         ModuleCompatibility.assessSetting(XCarryModule.class, "AutoPickup").classification()
      );
   }

   @Test
   void permanentServiceSettingsExposeTheirOwnCompatibilityRisk() {
      ModuleCompatibility.SettingAssessment serverSlot =
         ModuleCompatibility.assessSetting(AntiCheatModule.class, "NoServerSlot");
      ModuleCompatibility.SettingAssessment movementSync =
         ModuleCompatibility.assessSetting(AntiCheatModule.class, "MovementSync");
      ModuleCompatibility.SettingAssessment direction =
         ModuleCompatibility.assessSetting(AntiCheatModule.class, "StrictDirection");

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, serverSlot.classification());
      assertEquals(ModuleCompatibility.Classification.SERVER_SENSITIVE, movementSync.classification());
      assertEquals(ModuleCompatibility.Classification.WORLD_INTERACTION, direction.classification());
      assertTrue(serverSlot.individuallyReviewed());
      assertTrue(movementSync.individuallyReviewed());
      assertTrue(direction.individuallyReviewed());
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
