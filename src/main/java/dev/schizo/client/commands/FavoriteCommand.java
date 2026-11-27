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
import dev.schizo.client.managers.ModulePreferencesRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.utils.chat.ChatUtils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Commands for managing favorite and recently enabled modules. */
public final class FavoriteCommand extends Command {
   private static final int MAX_LISTED = 20;

   public FavoriteCommand() {
      super("favorite");
      this.setAliases("favorites", "fav");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.then(literal("toggle")
         .then(argument("module", new ModuleArgumentType())
            .executes(context -> this.toggle(context))));

      root.then(literal("add")
         .then(argument("module", new ModuleArgumentType())
            .executes(context -> this.setFavorite(context, true))));

      root.then(literal("remove")
         .then(argument("module", new ModuleArgumentType())
            .executes(context -> this.setFavorite(context, false))));

      root.then(literal("list").executes(context -> this.listFavorites()));
      root.then(literal("recent").executes(context -> this.listRecent()));
      root.then(literal("clear").executes(context -> this.clearFavorites()));
      root.then(literal("clearrecent").executes(context -> this.clearRecent()));

      root.executes(context -> {
         this.print("Favorites: toggle <module>, add <module>, remove <module>, list, recent, clear, clearrecent.");
         return 1;
      });
   }

   private ModulePreferencesRegistry registry() {
      return ClientContext.modulePreferencesRegistry;
   }

   private int toggle(CommandContext<?> context) {
      Module module = context.getArgument("module", Module.class);
      boolean favorite = this.registry().toggleFavorite(module);
      this.print(module.getName() + (favorite ? " added to" : " removed from") + " favorites.");
      return 1;
   }

   private int setFavorite(CommandContext<?> context, boolean favorite) {
      Module module = context.getArgument("module", Module.class);
      boolean changed = favorite
         ? this.registry().addFavorite(module)
         : this.registry().removeFavorite(module);
      this.print(changed
         ? module.getName() + (favorite ? " added to" : " removed from") + " favorites."
         : favorite ? module.getName() + " is already a favorite." : module.getName() + " is not a favorite.");
      return 1;
   }

   private int listFavorites() {
      this.printNames("Favorite modules", this.registry().getFavorites());
      return 1;
   }

   private int listRecent() {
      this.printNames("Recently enabled modules", this.registry().getRecentlyUsed());
      return 1;
   }

   private int clearFavorites() {
      this.registry().clearFavorites();
      this.print("Favorite modules cleared.");
      return 1;
   }

   private int clearRecent() {
      this.registry().clearRecentlyUsed();
      this.print("Recently enabled module history cleared.");
      return 1;
   }

   private void printNames(String title, List<String> names) {
      if (names.isEmpty()) {
         this.print(title + ": none.");
         return;
      }

      StringBuilder output = new StringBuilder(title).append(":");
      int count = Math.min(names.size(), MAX_LISTED);
      for (int index = 0; index < count; index++) {
         output.append("\n").append(index + 1).append(". ").append(names.get(index));
      }
      if (names.size() > count) {
         output.append("\n… and ").append(names.size() - count).append(" more");
      }
      this.print(output.toString());
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
