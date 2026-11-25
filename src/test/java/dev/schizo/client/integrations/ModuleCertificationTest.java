package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.render.PhaseESPModule;
import org.junit.jupiter.api.Test;

class ModuleCertificationTest {
   @Test
   void targetedTestsDoNotImplySourceReviewOrFullCertification() {
      ModuleCertification.Assessment assessment =
         ModuleCertification.assessClass(AutoCrystalModule.class);

      assertEquals(ModuleCertification.AutomatedEvidence.TARGETED, assessment.automatedEvidence());
      assertFalse(assessment.sourceReviewed());
      assertFalse(assessment.complete());
      assertEquals("source review", assessment.nextGate());
   }

   @Test
   void visualModulesDoNotRequireControlledMultiplayerEvidence() {
      ModuleCertification.Assessment assessment =
         ModuleCertification.assessClass(PhaseESPModule.class);

      assertTrue(assessment.sourceReviewed());
      assertEquals(ModuleCompatibility.Classification.VISUAL_ONLY, assessment.compatibility());
      assertEquals(ModuleCertification.LiveEvidence.NOT_REQUIRED, assessment.multiplayerEvidence());
      assertFalse(assessment.runtimeValidated());
      assertFalse(assessment.complete());
   }

   @Test
   void unknownClassesRemainConservativelyUnreviewed() {
      ModuleCertification.Assessment assessment =
         ModuleCertification.assessClass(UnreviewedModule.class);

      assertFalse(assessment.sourceReviewed());
      assertFalse(assessment.allSettingsReviewed());
      assertEquals(-1, assessment.settingCount());
      assertEquals(ModuleCertification.AutomatedEvidence.NONE, assessment.automatedEvidence());
      assertEquals(ModuleCertification.LiveEvidence.NOT_RECORDED, assessment.runtimeEvidence());
      assertEquals(ModuleCertification.LiveEvidence.NOT_RECORDED, assessment.multiplayerEvidence());
      assertFalse(assessment.complete());
   }

   private static final class UnreviewedModule {}
}
