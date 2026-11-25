/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo module-certification inventory.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.Setting;
import java.util.Map;

/**
 * Conservative certification inventory.
 *
 * Automated evidence is deliberately separate from source review and live validation.
 * A targeted unit test never implies that the full module, every setting, or multiplayer
 * behavior has been certified.
 */
public final class ModuleCertification {
   private static final Map<String, String> TARGETED_AUTOMATED_EVIDENCE = Map.ofEntries(
      evidence("dev.schizo.client.modules.combat.AnchorAuraModule",
         "Anchor target-selection and bounded placement-scan policy tests"),
      evidence("dev.schizo.client.modules.combat.AntiPhaseModule",
         "AntiPhase target and inventory-swap policy tests"),
      evidence("dev.schizo.client.modules.combat.AutoArmorModule",
         "AutoArmor range-policy test"),
      evidence("dev.schizo.client.modules.combat.AutoCrystalModule",
         "AutoCrystal placement guard plus shared crystal damage/target policy tests"),
      evidence("dev.schizo.client.modules.combat.AutoLogModule",
         "AutoLog disconnect-policy test"),
      evidence("dev.schizo.client.modules.combat.BedAuraModule",
         "BedAura target/direction and placement-scanner policy tests"),
      evidence("dev.schizo.client.modules.combat.HoleFillModule",
         "HoleFill target-policy test"),
      evidence("dev.schizo.client.modules.combat.PistonCrystalModule",
         "PistonCrystal planner and policy tests"),
      evidence("dev.schizo.client.modules.combat.PusherModule",
         "Pusher target/power policy tests"),
      evidence("dev.schizo.client.modules.exploit.AntiHungerModule",
         "AntiHunger packet-policy test"),
      evidence("dev.schizo.client.modules.exploit.NewChunksModule",
         "NewChunks data-codec test"),
      evidence("dev.schizo.client.modules.exploit.PhaseModule",
         "Phase interaction-policy test"),
      evidence("dev.schizo.client.modules.exploit.PingSpoofModule",
         "PingSpoof deadline and packet-classification tests"),
      evidence("dev.schizo.client.modules.exploit.TimerModule",
         "Timer priority-policy test"),
      evidence("dev.schizo.client.modules.hud.ArmorModule",
         "Armor HUD rendering-state test"),
      evidence("dev.schizo.client.modules.hud.MusicModule",
         "Music display-text test"),
      evidence("dev.schizo.client.modules.hud.WelcomerModule",
         "Welcomer overlap-policy test"),
      evidence("dev.schizo.client.modules.movement.FastFallModule",
         "FastFall timer-policy test"),
      evidence("dev.schizo.client.modules.movement.FastWebModule",
         "FastWeb timer-policy test"),
      evidence("dev.schizo.client.modules.movement.StepModule",
         "Step timer-policy test"),
      evidence("dev.schizo.client.modules.render.PhaseESPModule",
         "PhaseESP monotonic animation/lifecycle test"),
      evidence("dev.schizo.client.modules.render.SearchModule",
         "Search bounded chunk-scan cursor test"),
      evidence("dev.schizo.client.modules.render.TunnelsModule",
         "Tunnels bounded chunk-scan cursor test")
   );

   private ModuleCertification() {
   }

   public static Assessment assess(Module module) {
      if (module == null) {
         return assessClass(null, 0, 0);
      }

      int reviewedSettings = 0;
      for (Setting<?> setting : module.getSettings()) {
         if (ModuleCompatibility.assessSetting(module.getClass(), setting.getConfigName()).individuallyReviewed()) {
            reviewedSettings++;
         }
      }
      return assessClass(module.getClass(), module.getSettings().size(), reviewedSettings);
   }

   public static Assessment assessClass(Class<?> moduleType) {
      return assessClass(moduleType, 0, 0);
   }

   private static Assessment assessClass(Class<?> moduleType, int settingCount, int reviewedSettings) {
      ModuleCompatibility.Assessment compatibility = ModuleCompatibility.assess(moduleType);
      String typeName = moduleType == null ? "" : moduleType.getName();
      String automatedEvidence = TARGETED_AUTOMATED_EVIDENCE.get(typeName);
      AutomatedEvidence automated = automatedEvidence == null
         ? AutomatedEvidence.NONE
         : AutomatedEvidence.TARGETED;

      boolean multiplayerRequired = compatibility.classification()
         != ModuleCompatibility.Classification.CLIENT_ONLY
         && compatibility.classification() != ModuleCompatibility.Classification.VISUAL_ONLY;

      return new Assessment(
         compatibility.sourceReviewed(),
         compatibility.classification(),
         automated,
         automatedEvidence,
         settingCount,
         reviewedSettings,
         LiveEvidence.NOT_RECORDED,
         multiplayerRequired ? LiveEvidence.NOT_RECORDED : LiveEvidence.NOT_REQUIRED
      );
   }

   private static Map.Entry<String, String> evidence(String className, String note) {
      return Map.entry(className, note);
   }

   public enum AutomatedEvidence {
      NONE,
      TARGETED
   }

   public enum LiveEvidence {
      NOT_REQUIRED,
      NOT_RECORDED,
      PASSED,
      FAILED
   }

   public record Assessment(
      boolean sourceReviewed,
      ModuleCompatibility.Classification compatibility,
      AutomatedEvidence automatedEvidence,
      String automatedEvidenceNote,
      int settingCount,
      int reviewedSettings,
      LiveEvidence runtimeEvidence,
      LiveEvidence multiplayerEvidence
   ) {
      public boolean allSettingsReviewed() {
         return this.settingCount == this.reviewedSettings;
      }

      public boolean automatedEvidencePresent() {
         return this.automatedEvidence != AutomatedEvidence.NONE;
      }

      public boolean runtimeValidated() {
         return this.runtimeEvidence == LiveEvidence.PASSED;
      }

      public boolean multiplayerValidated() {
         return this.multiplayerEvidence == LiveEvidence.NOT_REQUIRED
            || this.multiplayerEvidence == LiveEvidence.PASSED;
      }

      public int passedGates() {
         int passed = 0;
         if (this.sourceReviewed) passed++;
         if (this.allSettingsReviewed()) passed++;
         if (this.automatedEvidencePresent()) passed++;
         if (this.runtimeValidated()) passed++;
         if (this.multiplayerValidated()) passed++;
         return passed;
      }

      public int totalGates() {
         return 5;
      }

      public boolean complete() {
         return this.passedGates() == this.totalGates();
      }

      public String nextGate() {
         if (!this.sourceReviewed) return "source review";
         if (!this.allSettingsReviewed()) return "setting-level review";
         if (!this.automatedEvidencePresent()) return "targeted automated coverage";
         if (!this.runtimeValidated()) return "live runtime validation";
         if (!this.multiplayerValidated()) return "controlled multiplayer validation";
         return "complete";
      }
   }
}
