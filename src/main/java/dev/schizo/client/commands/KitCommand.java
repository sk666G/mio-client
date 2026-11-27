/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.KitRegistry;
import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.utils.inventory.Kit;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class KitCommand extends Command {

   public KitCommand() {
      super("kit");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {

      String[] saveAliasNames = {"save", "add"};
      RequiredArgumentBuilder saveBuilder = Command.aliasedLiteral(saveAliasNames);

      ArgumentBuilder saveNameBranch = Command.argument("name", FancyStringArgument.fancyString()).executes((com.mojang.brigadier.Command<Object>)context -> {

            String name = (String)context.getArgument("name", String.class);
            KitRegistry capturedRegistry = ClientContext.kitRegistry;
            if (!capturedRegistry.captureInventory(name)) {
               ChatUtils.printChat(Component.literal("Kit could not be saved"), ChatUtils.DEFAULT_SIGNATURE);
               return 0;
            }

            Object[] messageArguments = new Object[]{name};
            String messageFormat = "Kit %s has been saved";
            String formattedMessage = messageFormat.formatted(messageArguments);
            MutableComponent message = Component.literal(formattedMessage);

            ChatUtils.printChat(
               message, ChatUtils.createSignature(-1)
            );
            return 1;
         });
      ArgumentBuilder saveBranch = saveBuilder.then(saveNameBranch);
      root.then(saveBranch);

      String[] removeAliasNames = {"remove", "delete", "del"};
      RequiredArgumentBuilder removeBuilder = Command.aliasedLiteral(removeAliasNames);

      ArgumentBuilder removeKitBranch = Command.argument("kit", new KitArgumentType()).executes((com.mojang.brigadier.Command<Object>)context -> {

            Kit kit = (Kit)context.getArgument("kit", Kit.class);
            KitRegistry registryForRemoval = ClientContext.kitRegistry;
            if (!registryForRemoval.unregisterKit(kit)) {
               ChatUtils.printChat(Component.literal("Kit could not be removed"), ChatUtils.DEFAULT_SIGNATURE);
               return 0;
            }

            Object[] formattedArguments = new Object[1];
            Kit kitForMessage = kit;
            formattedArguments[0] = kitForMessage.getName();
            Object[] messageArguments = formattedArguments;
            String messageFormat = "Kit %s has been removed";
            String formattedMessage = messageFormat.formatted(messageArguments);
            MutableComponent message = Component.literal(formattedMessage);

            ChatUtils.printChat(
               message, ChatUtils.createSignature(-1)
            );
            return 1;
         });
      ArgumentBuilder removeBranch = removeBuilder.then(removeKitBranch);
      root.then(removeBranch);

      LiteralArgumentBuilder loadLiteralBuilder = Command.literal("load");

      ArgumentBuilder loadKitBranch = Command.argument("kit", new KitArgumentType()).executes((com.mojang.brigadier.Command<Object>)context -> {

            Kit kit = (Kit)context.getArgument("kit", Kit.class);
            KitRegistry selectionRegistry = ClientContext.kitRegistry;
            String kitName = kit.getName();

            selectionRegistry.setCurrentKitName(kitName);

            Object[] formattedArguments = new Object[1];
            Kit kitForMessage = kit;
            formattedArguments[0] = kitForMessage.getName();
            Object[] messageArguments = formattedArguments;
            String messageFormat = "Kit %s has been loaded";
            String formattedMessage = messageFormat.formatted(messageArguments);
            MutableComponent message = Component.literal(formattedMessage);

            ChatUtils.printChat(
               message, ChatUtils.createSignature(-1)
            );
            return 1;
         });
      ArgumentBuilder loadBranch = loadLiteralBuilder.then(loadKitBranch);
      root.then(loadBranch);

      root.then(Command.literal("rename").then(
         Command.argument("kit", new KitArgumentType()).then(
            Command.argument("name", FancyStringArgument.fancyString()).executes((com.mojang.brigadier.Command<Object>)context -> {
               Kit kit = (Kit)context.getArgument("kit", Kit.class);
               String name = (String)context.getArgument("name", String.class);
               if (!ClientContext.kitRegistry.renameKit(kit, name)) {
                  ChatUtils.printChat(Component.literal("Kit could not be renamed"), ChatUtils.DEFAULT_SIGNATURE);
                  return 0;
               }
               MutableComponent message = Component.literal(
                  "Kit %s has been renamed to %s".formatted(kit.getName(), name)
               );
               ChatUtils.printChat(message, ChatUtils.DEFAULT_SIGNATURE);
               return 1;
            })
         )
      ));
      root.executes((com.mojang.brigadier.Command<Object>)context -> {
            KitRegistry currentKitRegistry = ClientContext.kitRegistry;

            Kit currentKit = KitRegistry.getCurrentKit(currentKitRegistry);
            String selectedNameResult;
            if (currentKit == null) {

               selectedNameResult = "None";
            } else {
               selectedNameResult = currentKit.getName();
            }

            String selectedName = selectedNameResult;
            KitRegistry registryForAvailableKits = ClientContext.kitRegistry;
            Stream<String> availableNameStream = ((List<Kit>)registryForAvailableKits.getRegistry()).stream()
               .filter(candidate -> candidate != currentKit)
               .map(Kit::getName);
            String availableNames = availableNameStream.collect(Collectors.joining(", "));

            Object[] selectionMessageArguments = new Object[]{selectedName};
            String selectionMessageText = "Kit selected: %s".formatted(selectionMessageArguments);
            MutableComponent selectionMessage = Component.literal(selectionMessageText);

            ChatUtils.printChat(
               selectionMessage, ChatUtils.createSignature(-1)
            );

            Object[] availableMessageArguments = new Object[]{availableNames};
            String availableMessageText = "Kits available: %s.".formatted(availableMessageArguments);
            MutableComponent availableMessage = Component.literal(availableMessageText);

            ChatUtils.printChat(
               availableMessage, ChatUtils.createSignature(-341842)
            );
            return 1;
         });
   }

}
