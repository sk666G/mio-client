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
import dev.schizo.client.integrations.MultiplayerCompatibilityPolicy;
import dev.schizo.client.integrations.MultiplayerCompatibilityWarnings;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.PermanentModule;
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
      root.then(literal("policy")
         .executes(context -> this.showPolicy())
         .then(literal("status").executes(context -> this.showPolicy()))
         .then(literal("warn").executes(context -> this.setPolicyMode(MultiplayerCompatibilityPolicy.Mode.WARN_ONLY)))
         .then(literal("conservative").executes(context -> this.setPolicyMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE)))
         .then(literal("allow")
            .then(argument("name", StringArgumentType.word()).executes(this::allowModule)))
         .then(literal("deny")
            .then(argument("name", StringArgumentType.word()).executes(this::denyModule)))
         .then(literal("reset")
            .then(argument("name", StringArgumentType.word()).executes(this::resetModulePreference)))
         .then(literal("clear").executes(context -> this.clearPolicyExceptions())));
   }

   private int showPolicy() {
      MultiplayerCompatibilityPolicy policy = MultiplayerCompatibilityPolicy.GLOBAL;
      List<String> exceptions = policy.getAllowedModules();
      StringBuilder output = new StringBuilder("Multiplayer policy: ")
         .append(policy.getMode())
         .append("\nConservative mode blocks SERVER_SENSITIVE, PACKET_AFFECTING and UNVERIFIED modules in remote multiplayer.")
         .append("\nInput-modifying and world-interaction modules remain available with warnings.")
         .append("\nAlways-on permanent service modules remain warning-only until their settings can be enforced safely.")
         .append("\nExplicit module exceptions: ").append(exceptions.size());
      for (String exception : exceptions) {
         output.append("\n- ").append(exception);
      }
      output.append("\nUse ;compat policy warn|conservative|allow <ModuleClass>|deny <ModuleClass>|reset <ModuleClass>|clear.");
      this.print(output.toString());
      return 1;
   }

   private int setPolicyMode(MultiplayerCompatibilityPolicy.Mode mode) {
      MultiplayerCompatibilityPolicy.GLOBAL.setMode(mode);
      MultiplayerCompatibilityWarnings.enforceActiveModules();
      this.print(mode == MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE
         ? "Conservative multiplayer policy enabled. Blocked modules were suspended without changing their saved enable preferences. They will not reactivate on disconnect."
         : "Multiplayer policy is warning-only. Suspended modules remain off until you explicitly enable them or restart.");
      return 1;
   }

   private int allowModule(CommandContext<?> context) {
      Module module = this.findModule(StringArgumentType.getString(context, "name"));
      if (module == null) {
         this.print("Module not found. Use ;compat list to browse module names.");
         return 0;
      }
      if (module instanceof PermanentModule) {
         this.print("Permanent service modules cannot be excepted as a whole; review their individual settings instead.");
         return 0;
      }
      boolean added = MultiplayerCompatibilityPolicy.GLOBAL.allowModule(module.getClass().getName());
      this.print(added
         ? module.getName() + " is now an explicit conservative-policy exception. It will not be auto-enabled; toggle it manually if needed."
         : module.getName() + " is already an explicit policy exception.");
      return 1;
   }

   private int resetModulePreference(CommandContext<?> context) {
      Module module = this.findModule(StringArgumentType.getString(context, "name"));
      if (module == null) {
         this.print("Module not found. Use ;compat list to browse module names.");
         return 0;
      }
      if (module instanceof PermanentModule) {
         this.print("Permanent service modules have no user-toggleable enabled preference; review their individual settings instead.");
         return 0;
      }
      module.resetCompatibilityPolicyPreference();
      this.print(module.getName() + " is now saved as disabled. The conservative policy exception (if any) is unchanged.");
      return 1;
   }

   private int denyModule(CommandContext<?> context) {
      Module module = this.findModule(StringArgumentType.getString(context, "name"));
      if (module == null) {
         this.print("Module not found. Use ;compat list to browse module names.");
         return 0;
      }
      boolean removed = MultiplayerCompatibilityPolicy.GLOBAL.removeAllowedModule(module.getClass().getName());
      MultiplayerCompatibilityWarnings.enforceActiveModules();
      this.print(removed
         ? module.getName() + " is no longer excepted by the conservative policy."
         : module.getName() + " had no explicit policy exception.");
      return 1;
   }

   private int clearPolicyExceptions() {
      MultiplayerCompatibilityPolicy.GLOBAL.clearAllowedModules();
      MultiplayerCompatibilityWarnings.enforceActiveModules();
      this.print("All conservative-policy exceptions were removed. Newly disallowed active modules were suspended; none will be auto-enabled.");
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
      output.append("\nExplicitly reviewed settings: ").append(ModuleCompatibility.reviewedSettingCount());
      output.append("\nAll registered modules are source-reviewed. Individual settings remain UNVERIFIED unless explicitly marked reviewed. This report does not certify server compatibility.");
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
      Module found = this.findModule(requested);

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
         .append("\nSettings (reviewed entries are explicit; others remain UNVERIFIED):");

      List<Setting<?>> settings = found.getSettings();
      int displayed = Math.min(settings.size(), MAX_LISTED_SETTINGS);
      for (int index = 0; index < displayed; index++) {
         Setting<?> setting = settings.get(index);
         ModuleCompatibility.SettingAssessment settingAssessment =
            ModuleCompatibility.assessSetting(found.getClass(), setting.getConfigName());
         output.append("\n• ").append(setting.getConfigName())
            .append(": ").append(settingAssessment.classification())
            .append(settingAssessment.individuallyReviewed() ? " [reviewed]" : " [unreviewed]")
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
