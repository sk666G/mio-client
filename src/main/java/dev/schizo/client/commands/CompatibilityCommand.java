/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo compatibility inventory command.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientContext;
import dev.schizo.client.integrations.ModuleCompatibility;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.chat.ChatUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;

/** Shows source-reviewed multiplayer compatibility labels and unreviewed settings. */
public final class CompatibilityCommand extends Command {
   private static final int MAX_LISTED_MODULES = 30;
   private static final int MAX_LISTED_SETTINGS = 24;

   public CompatibilityCommand() {
      super("compatibility");
      this.setAliases("compat");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.executes(context -> this.showStatus());
      root.then(literal("status").executes(context -> this.showStatus()));
      root.then(literal("list")
         .executes(context -> this.listModules(null))
         .then(argument("classification", StringArgumentType.word()).executes(this::listFiltered)));
      root.then(literal("module")
         .then(argument("name", StringArgumentType.word()).executes(this::showModule)));
   }

   private int showStatus() {
      Map<ModuleCompatibility.Classification, Integer> counts =
         new EnumMap<>(ModuleCompatibility.Classification.class);
      int total = 0;
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)entry;
         ModuleCompatibility.Classification classification =
            ModuleCompatibility.assess(module.getClass()).classification();
         counts.merge(classification, 1, Integer::sum);
         total++;
      }

      StringBuilder output = new StringBuilder("Multiplayer compatibility inventory: ")
         .append(total).append(" module(s)");
      for (ModuleCompatibility.Classification classification : ModuleCompatibility.Classification.values()) {
         output.append("\n").append(classification).append(": ").append(counts.getOrDefault(classification, 0));
      }
      output.append("\nUnreviewed modules/settings remain UNVERIFIED. This report does not certify server compatibility.");
      this.print(output.toString());
      return 1;
   }

   private int listFiltered(CommandContext<?> context) {
      String raw = StringArgumentType.getString(context, "classification");
      try {
         return this.listModules(ModuleCompatibility.Classification.valueOf(raw.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException invalid) {
         this.print("Unknown classification. Use: CLIENT_ONLY, VISUAL_ONLY, INPUT_MODIFYING, WORLD_INTERACTION, PACKET_AFFECTING, SERVER_SENSITIVE, UNVERIFIED.");
         return 0;
      }
   }

   private int listModules(ModuleCompatibility.Classification filter) {
      List<String> lines = new ArrayList<>();
      int total = 0;
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)entry;
         ModuleCompatibility.Assessment assessment = ModuleCompatibility.assess(module.getClass());
         if (filter != null && assessment.classification() != filter) {
            continue;
         }
         if (filter == null && !assessment.requiresMultiplayerWarning()) {
            continue;
         }
         total++;
         if (lines.size() < MAX_LISTED_MODULES) {
            lines.add(module.getName() + " — " + assessment.classification());
         }
      }

      String header = filter == null
         ? "Modules requiring a multiplayer warning: " + total
         : filter + " modules: " + total;
      StringBuilder output = new StringBuilder(header);
      for (String line : lines) {
         output.append("\n").append(line);
      }
      if (total > lines.size()) {
         output.append("\n… and ").append(total - lines.size()).append(" more; use ;compat module <ModuleClass> for details.");
      }
      this.print(output.toString());
      return 1;
   }

   private int showModule(CommandContext<?> context) {
      String requested = StringArgumentType.getString(context, "name");
      Module found = null;
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)entry;
         String simpleName = module.getClass().getSimpleName();
         String withoutSuffix = simpleName.endsWith("Module")
            ? simpleName.substring(0, simpleName.length() - "Module".length())
            : simpleName;
         if (module.getName().equalsIgnoreCase(requested)
            || simpleName.equalsIgnoreCase(requested)
            || withoutSuffix.equalsIgnoreCase(requested)) {
            found = module;
            break;
         }
      }

      if (found == null) {
         this.print("Module not found: " + requested + ". Use ;compat list to browse module names.");
         return 0;
      }

      ModuleCompatibility.Assessment moduleAssessment = ModuleCompatibility.assess(found.getClass());
      StringBuilder output = new StringBuilder("Compatibility review: ")
         .append(found.getName()).append(" (").append(found.getClass().getSimpleName()).append(")")
         .append("\nModule classification: ").append(moduleAssessment.classification())
         .append("\nSource reviewed: ").append(moduleAssessment.sourceReviewed() ? "yes" : "no")
         .append("\n").append(moduleAssessment.reason())
         .append("\nSettings (each remains UNVERIFIED until individually reviewed):");

      List<Setting<?>> settings = found.getSettings();
      int displayed = Math.min(settings.size(), MAX_LISTED_SETTINGS);
      for (int index = 0; index < displayed; index++) {
         Setting<?> setting = settings.get(index);
         ModuleCompatibility.SettingAssessment settingAssessment =
            ModuleCompatibility.assessSetting(found.getClass(), setting.getConfigName());
         output.append("\n• ").append(setting.getConfigName())
            .append(": ").append(settingAssessment.classification())
            .append(" (inherits ").append(settingAssessment.inheritedModuleClassification()).append(")");
      }
      if (settings.size() > displayed) {
         output.append("\n… and ").append(settings.size() - displayed).append(" more setting(s)");
      }
      this.print(output.toString());
      return 1;
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
