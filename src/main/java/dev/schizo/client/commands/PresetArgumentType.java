/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.managers.PresetRegistry;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.settings.options.PresetCategory;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class PresetArgumentType implements ArgumentType {

   @Override
   public String parse(StringReader reader) throws CommandSyntaxException {

      return CommandStringReader.readString(reader);
   }

   @Override
   public CompletableFuture listSuggestions(CommandContext context, SuggestionsBuilder builder) {
      PresetCategory category = (PresetCategory)context.getArgument("category", PresetCategory.class);
      PresetRegistry registry = ClientContext.configurationManager.getPresetRegistry(category);
      return SharedSuggestionProvider.suggest(registry.getPresetNames(), builder);
   }

   public static PresetData getPreset(CommandContext context, String categoryArgumentName, String presetArgumentName) throws CommandSyntaxException {
      PresetCategory category = (PresetCategory)context.getArgument(categoryArgumentName, PresetCategory.class);
      String presetName = (String)context.getArgument(presetArgumentName, String.class);
      ConfigurationManager configurationManager = ClientContext.configurationManager;
      PresetRegistry registryForLookup = configurationManager.getPresetRegistry(category);
      PresetData preset = registryForLookup.getPreset(presetName);
      if (preset == null) {
         DynamicCommandExceptionType notFoundException = new DynamicCommandExceptionType(
            missingName -> {
               String missingNameText = String.valueOf(missingName);
               String errorMessage = "Preset not found " + String.valueOf(missingNameText);
               return Component.nullToEmpty(errorMessage);
            }
         );
         throw notFoundException.create(presetName);
      } else {
         return preset;
      }
   }

}
