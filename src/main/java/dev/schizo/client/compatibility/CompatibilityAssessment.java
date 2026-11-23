package dev.schizo.client.compatibility;

import java.util.Objects;

public record CompatibilityAssessment(
   CompatibilityClass classification,
   Evidence evidence,
   String explanation
) {
   public CompatibilityAssessment {
      Objects.requireNonNull(classification, "classification");
      Objects.requireNonNull(evidence, "evidence");
      explanation = Objects.requireNonNull(explanation, "explanation").strip();
      if (explanation.isEmpty()) {
         throw new IllegalArgumentException("A compatibility explanation is required");
      }
   }

   public static CompatibilityAssessment sourceReviewed(
      CompatibilityClass classification, String explanation
   ) {
      return new CompatibilityAssessment(classification, Evidence.SOURCE_REVIEWED, explanation);
   }

   public static CompatibilityAssessment sourceReviewed(
      CompatibilityClass classification, String explanation
   ) {
      return new CompatibilityAssessment(classification, Evidence.SOURCE_REVIEWED, explanation);
   }

   public static CompatibilityAssessment unverified() {
      return new CompatibilityAssessment(
         CompatibilityClass.UNVERIFIED,
         Evidence.UNREVIEWED,
         "Behavior has not been reviewed; multiplayer compatibility is unknown."
      );
   }

   public boolean isRisky() {
      return switch (classification) {
         case PACKET_AFFECTING, SERVER_SENSITIVE, UNVERIFIED -> true;
         case CLIENT_ONLY, VISUAL_ONLY, INPUT_MODIFYING, WORLD_INTERACTION -> false;
      };
   }

   public String summary() {
      return classification + " (" + evidence.label + "): " + explanation;
   }

   public enum Evidence {
      UNREVIEWED("unreviewed"),
      SOURCE_REVIEWED("source reviewed; not server tested"),
      CONTROLLED_SERVER_TEST("controlled server tested");

      private final String label;

      Evidence(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }
   }
}
