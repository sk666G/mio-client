/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo module-certification command.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.schizo.client.ClientContext;
import dev.schizo.client.integrations.ModuleCertification;
import dev.schizo.client.modules.Module;
import dev.schizo.client.utils.chat.ChatUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Reports conservative per-module certification progress without overstating test evidence. */
public final class CertificationCommand extends Command {
   private static final int MAX_LISTED = 30;

   public CertificationCommand() {
      super("certification");
      this.setAliases("cert");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.executes(context -> this.showStatus());
      root.then(literal("status").executes(context -> this.showStatus()));
      root.then(literal("pending").executes(context -> this.listPending()));
      root.then(literal("module")
         .then(argument("name", StringArgumentType.word()).executes(this::showModule)));
   }

   private int showStatus() {
      int modules = 0;
      int sourceReviewed = 0;
      int automated = 0;
      int settingsComplete = 0;
      int runtimeValidated = 0;
      int multiplayerValidated = 0;
      int complete = 0;

      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         ModuleCertification.Assessment assessment = ModuleCertification.assess((Module)entry);
         modules++;
         if (assessment.sourceReviewed()) sourceReviewed++;
         if (assessment.automatedEvidencePresent()) automated++;
         if (assessment.allSettingsReviewed()) settingsComplete++;
         if (assessment.runtimeValidated()) runtimeValidated++;
         if (assessment.multiplayerValidated()) multiplayerValidated++;
         if (assessment.complete()) complete++;
      }

      this.print(
         "Schizo module certification: " + modules + " registered module(s)"
            + "\nSource reviewed: " + sourceReviewed + "/" + modules
            + "\nTargeted automated evidence: " + automated + "/" + modules
            + "\nAll settings individually reviewed: " + settingsComplete + "/" + modules
            + "\nLive runtime validated: " + runtimeValidated + "/" + modules
            + "\nMultiplayer gate satisfied/not-required: " + multiplayerValidated + "/" + modules
            + "\nFully certified: " + complete + "/" + modules
            + "\nUse ;cert pending or ;cert module <ModuleClass>. A unit test alone never means certified."
      );
      return 1;
   }

   private int listPending() {
      List<Module> modules = new ArrayList<>();
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         modules.add((Module)entry);
      }
      modules.sort(
         Comparator.comparingInt((Module module) -> ModuleCertification.assess(module).passedGates())
            .thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER)
      );

      StringBuilder output = new StringBuilder("Least-certified modules:");
      int displayed = Math.min(MAX_LISTED, modules.size());
      for (int index = 0; index < displayed; index++) {
         Module module = modules.get(index);
         ModuleCertification.Assessment assessment = ModuleCertification.assess(module);
         output.append("\n")
            .append(module.getName())
            .append(" — ")
            .append(assessment.passedGates()).append("/").append(assessment.totalGates())
            .append(" — next: ").append(assessment.nextGate());
      }
      if (modules.size() > displayed) {
         output.append("\n… and ").append(modules.size() - displayed).append(" more.");
      }
      this.print(output.toString());
      return 1;
   }

   private int showModule(CommandContext<?> context) {
      String requested = StringArgumentType.getString(context, "name");
      Module module = this.findModule(requested);
      if (module == null) {
         this.print("Module not found: " + requested);
         return 0;
      }

      ModuleCertification.Assessment assessment = ModuleCertification.assess(module);
      StringBuilder output = new StringBuilder("Certification: ")
         .append(module.getName()).append(" (").append(module.getClass().getSimpleName()).append(")")
         .append("\nProgress: ").append(assessment.passedGates()).append("/").append(assessment.totalGates())
         .append("\nSource reviewed: ").append(assessment.sourceReviewed() ? "yes" : "no")
         .append("\nCompatibility: ").append(assessment.compatibility())
         .append("\nSettings reviewed: ").append(assessment.reviewedSettings())
         .append("/").append(assessment.settingCount())
         .append("\nAutomated evidence: ").append(assessment.automatedEvidence())
         .append("\nRuntime evidence: ").append(assessment.runtimeEvidence())
         .append("\nMultiplayer evidence: ").append(assessment.multiplayerEvidence())
         .append("\nNext gate: ").append(assessment.nextGate());

      if (assessment.automatedEvidenceNote() != null) {
         output.append("\nAutomated evidence note: ").append(assessment.automatedEvidenceNote());
      }
      this.print(output.toString());
      return 1;
   }

   private Module findModule(String requested) {
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)entry;
         String simpleName = module.getClass().getSimpleName();
         String withoutSuffix = simpleName.endsWith("Module")
            ? simpleName.substring(0, simpleName.length() - "Module".length())
            : simpleName;
         if (module.getName().equalsIgnoreCase(requested)
            || module.getClass().getName().equalsIgnoreCase(requested)
            || simpleName.equalsIgnoreCase(requested)
            || withoutSuffix.equalsIgnoreCase(requested)) {
            return module;
         }
      }
      return null;
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
