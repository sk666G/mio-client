/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ContextProfileRegistry;
import dev.schizo.client.managers.PresetRegistry;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.chat.ChatUtils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class ContextProfileCommand extends Command {
   private static final int MAX_LISTED_RULES = 20;

   public ContextProfileCommand() {
      super("contextprofile");
      this.setAliases("contextprofiles", "ctxprofile");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.then(literal("enable").executes(context -> setEnabled(true)));
      root.then(literal("disable").executes(context -> setEnabled(false)));
      root.then(literal("status").executes(context -> showStatus()));
      root.then(literal("refresh").executes(context -> refresh()));
      root.then(literal("list").executes(context -> listRules()));
      root.then(literal("clear").executes(context -> clearRules()));

      root.then(literal("add")
         .then(argument("server", StringArgumentType.string())
            .then(argument("dimension", StringArgumentType.word())
               .then(argument("game_mode", StringArgumentType.word())
                  .then(argument("category", StringArgumentType.word())
                     .then(argument("preset", StringArgumentType.string())
                        .executes(this::addRule)))))));

      root.then(literal("remove")
         .then(argument("server", StringArgumentType.string())
            .then(argument("dimension", StringArgumentType.word())
               .then(argument("game_mode", StringArgumentType.word())
                  .then(argument("category", StringArgumentType.word())
                     .executes(this::removeRule))))));

      root.executes(context -> {
         this.print("Context profiles: enable, disable, status, refresh, list, add <server> <dimension> <game_mode> <category> <preset>, remove <server> <dimension> <game_mode> <category>, clear.");
         return 1;
      });
   }

   private static ContextProfileRegistry registry() {
      return ClientContext.contextProfileRegistry;
   }

   private int setEnabled(boolean enabled) {
      registry().setEnabled(enabled);
      this.print(enabled
         ? "Automatic context profiles enabled. Rules apply on the next player tick."
         : "Automatic context profiles disabled. The active profile's temporary changes were restored.");
      return 1;
   }

   private int showStatus() {
      ContextProfileRegistry profiles = registry();
      String failedContext = profiles.getFailedContextDescription();
      this.print("Context profiles: " + (profiles.isEnabled() ? "enabled" : "disabled")
         + "\nCurrent context: " + profiles.getCurrentContextDescription()
         + "\nRules: " + profiles.getRules().size()
         + (failedContext == null
            ? ""
            : "\nLast preset application failed for: " + failedContext + ". Fix the preset and run ;contextprofile refresh."));
      return 1;
   }

   private int refresh() {
      if (!registry().retryCurrentContext()) {
         this.print("Context profiles are disabled. Enable them before requesting a refresh.");
         return 0;
      }
      this.print("Context profile refresh requested. Rules will retry on the next player tick.");
      return 1;
   }

   private int listRules() {
      ContextProfileRegistry profiles = registry();
      List<ContextProfileRegistry.Rule> rules = profiles.getRules();
      StringBuilder message = new StringBuilder();
      message.append("Context profiles ").append(profiles.isEnabled() ? "(enabled)" : "(disabled)")
         .append(": ").append(rules.size()).append(" rule(s)");

      int displayed = Math.min(rules.size(), MAX_LISTED_RULES);
      for (int index = 0; index < displayed; index++) {
         ContextProfileRegistry.Rule rule = rules.get(index);
         message.append("\n")
            .append(index + 1).append(". ")
            .append(rule.server()).append(" / ")
            .append(rule.dimension()).append(" / ")
            .append(rule.gameMode()).append(" → ")
            .append(rule.category().getName()).append(": ")
            .append(rule.presetName());
      }
      if (rules.size() > displayed) {
         message.append("\n… and ").append(rules.size() - displayed).append(" more");
      }

      this.print(message.toString());
      return 1;
   }

   private int addRule(CommandContext<?> context) {
      String server = StringArgumentType.getString(context, "server");
      String dimension = StringArgumentType.getString(context, "dimension");
      String gameMode = StringArgumentType.getString(context, "game_mode");
      String categoryName = StringArgumentType.getString(context, "category");
      String presetName = StringArgumentType.getString(context, "preset");

      PresetCategory category = PresetCategory.findByName(categoryName);
      if (category == null) {
         this.print("Unknown preset category: " + categoryName);
         return 0;
      }

      PresetRegistry presetRegistry = ClientContext.configurationManager.getPresetRegistry(category);
      if (presetRegistry == null || presetRegistry.getPreset(presetName) == null) {
         this.print("Preset \"" + presetName + "\" does not exist in category " + category.getName() + ".");
         return 0;
      }

      try {
         registry().addRule(server, dimension, gameMode, category, presetName);
         this.print("Saved context rule: " + server + " / " + dimension + " / " + gameMode
            + " → " + category.getName() + ": " + presetName + ".");
         return 1;
      } catch (IllegalArgumentException | IllegalStateException invalidRule) {
         this.print("Could not add context profile rule: " + invalidRule.getMessage());
         return 0;
      }
   }

   private int removeRule(CommandContext<?> context) {
      String server = StringArgumentType.getString(context, "server");
      String dimension = StringArgumentType.getString(context, "dimension");
      String gameMode = StringArgumentType.getString(context, "game_mode");
      String categoryName = StringArgumentType.getString(context, "category");
      PresetCategory category = PresetCategory.findByName(categoryName);
      if (category == null) {
         this.print("Unknown preset category: " + categoryName);
         return 0;
      }

      try {
         boolean removed = registry().removeRule(server, dimension, gameMode, category);
         this.print(removed ? "Context profile rule removed." : "No matching context profile rule was found.");
         return removed ? 1 : 0;
      } catch (IllegalArgumentException invalidRule) {
         this.print("Could not remove context profile rule: " + invalidRule.getMessage());
         return 0;
      }
   }

   private int clearRules() {
      registry().clearRules();
      this.print("All context profile rules cleared.");
      return 1;
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
