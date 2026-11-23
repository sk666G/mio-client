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
import org.junit.jupiter.api.Test;

class ModuleCompatibilityTest {
   @Test
   void classifiesEveryFirstPassModuleByExactClass() {
      assertClassification(SettingsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AntiCheatModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ColorsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(FontsModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(HUDModule.class, ModuleCompatibility.Classification.CLIENT_ONLY);
      assertClassification(AmbienceModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(AnimationsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ChamsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ESPModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(HitmarkerModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(NameTagsModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ParticlesModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(ShaderModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(TracersModule.class, ModuleCompatibility.Classification.VISUAL_ONLY);
      assertClassification(dev.schizo.client.modules.movement.NoFallModule.class, ModuleCompatibility.Classification.PACKET_AFFECTING);
      assertClassification(ReachModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(PhaseModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(AntiPhaseModule.class, ModuleCompatibility.Classification.WORLD_INTERACTION);
      assertClassification(ElytraFlyModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(CriticalsModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(NoSlowModule.class, ModuleCompatibility.Classification.SERVER_SENSITIVE);
      assertClassification(FastClimbModule.class, ModuleCompatibility.Classification.INPUT_MODIFYING);
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
