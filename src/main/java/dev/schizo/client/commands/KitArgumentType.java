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
import dev.schizo.client.managers.KitRegistry;
import dev.schizo.client.utils.inventory.Kit;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class KitArgumentType implements ArgumentType {
   private static final String unknownKitFormat = "Kit %s doesn't exist";

   @Override
   public Kit parse(StringReader reader) throws CommandSyntaxException {

      String requestedKitName = CommandStringReader.readString(reader);
      KitRegistry kitRegistry = ClientContext.kitRegistry;
      Kit matchingKit = kitRegistry.findByName(requestedKitName).orElse(null);
      if (matchingKit != null) {
         return matchingKit;
      }

      DynamicCommandExceptionType unknownKitException = new DynamicCommandExceptionType(argument -> {
         String format = unknownKitFormat;
         Object[] formatArguments = new Object[]{argument};
         String errorMessage = String.format(format, formatArguments);
         return Component.literal(errorMessage);
      });
      throw unknownKitException.create(requestedKitName);

   }

   @Override
   public CompletableFuture listSuggestions(CommandContext context, SuggestionsBuilder builder) {
      KitRegistry kitRegistry = ClientContext.kitRegistry;
      Stream<String> kitNames = ((List<Kit>)kitRegistry.getRegistry()).stream().map(Kit::getName);
      return SharedSuggestionProvider.suggest(kitNames, builder);
   }

}
