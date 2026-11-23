package dev.schizo.client.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompatibilityAssessmentTest {
   @Test
   void unreviewedDefaultIsExplicitAndRisky() {
      CompatibilityAssessment assessment = CompatibilityAssessment.unverified();

      assertEquals(CompatibilityClass.UNVERIFIED, assessment.classification());
      assertEquals(CompatibilityAssessment.Evidence.UNREVIEWED, assessment.evidence());
      assertTrue(assessment.isRisky());
      assertTrue(assessment.summary().contains("multiplayer compatibility is unknown"));
   }

   @Test
   void riskClassificationAndEvidenceAreDistinct() {
      CompatibilityAssessment sourceReview = CompatibilityAssessment.sourceReviewed(
         CompatibilityClass.PACKET_AFFECTING,
         "Rewrites movement packet state."
      );
      CompatibilityAssessment clientOnly = CompatibilityAssessment.sourceReviewed(
         CompatibilityClass.CLIENT_ONLY,
         "Reads local UI state only."
      );

      assertTrue(sourceReview.isRisky());
      assertTrue(sourceReview.summary().contains("not server tested"));
      assertFalse(clientOnly.isRisky());
      assertEquals(CompatibilityAssessment.Evidence.SOURCE_REVIEWED, clientOnly.evidence());
   }

   @Test
   void classificationAndWarningReasonsAreSearchable() {
      CompatibilityAssessment assessment = CompatibilityAssessment.sourceReviewed(
         CompatibilityClass.PACKET_AFFECTING,
         "Rewrites movement packet state."
      );

      assertTrue(assessment.matchesSearch("packet_affecting"));
      assertTrue(assessment.matchesSearch("rewrites movement"));
      assertTrue(assessment.matchesSearch("source reviewed"));
      assertFalse(assessment.matchesSearch("unrelated"));
   }

   @Test
   void assessmentRequiresAReason() {
      assertThrows(
         IllegalArgumentException.class,
         () -> new CompatibilityAssessment(
            CompatibilityClass.UNVERIFIED,
            CompatibilityAssessment.Evidence.UNREVIEWED,
            " "
         )
      );
   }
}
