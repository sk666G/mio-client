package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ModuleCompatibilityTest {
   @Test
   void classifiesReviewedPacketAffectingModules() {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(NoFallModule.class);

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING, assessment.classification());
      assertTrue(assessment.sourceReviewed());
      assertTrue(assessment.requiresMultiplayerWarning());
      assertTrue(assessment.reason().contains("on-ground"));
   }

   @Test
   void classifiesClearlyLocalSettingsModules() {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(SettingsModule.class);

      assertEquals(ModuleCompatibility.Classification.CLIENT_ONLY, assessment.classification());
      assertTrue(assessment.sourceReviewed());
      assertFalse(assessment.requiresMultiplayerWarning());
   }

   @Test
   void defaultsUnknownModulesToUnverified() {
      ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(UnreviewedModule.class);

      assertEquals(ModuleCompatibility.Classification.UNVERIFIED, assessment.classification());
      assertFalse(assessment.sourceReviewed());
      assertTrue(assessment.requiresMultiplayerWarning());
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

   private static final class NoFallModule {}
   private static final class SettingsModule {}
   private static final class ReachModule {}
   private static final class UnreviewedModule {}
}
