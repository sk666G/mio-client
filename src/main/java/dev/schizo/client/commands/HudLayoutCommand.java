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
import dev.schizo.client.config.HudLayoutStore;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.utils.chat.ChatUtils;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class HudLayoutCommand extends Command {
   public HudLayoutCommand() {
      super("hudlayout");
      this.setAliases("hudlayouts");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.then(literal("list").executes(context -> listLayouts()));
      root.then(literal("save").then(argument("name", StringArgumentType.string())
         .executes(context -> saveLayout(context))));
      root.then(literal("load").then(argument("name", StringArgumentType.string())
         .executes(context -> loadLayout(context))));
      root.then(literal("delete").then(argument("name", StringArgumentType.string())
         .executes(context -> deleteLayout(context))));
      root.then(literal("rename").then(argument("old_name", StringArgumentType.string())
         .then(argument("new_name", StringArgumentType.string())
            .executes(context -> renameLayout(context)))));
      root.executes(context -> {
         ChatUtils.printChat(
            Component.literal("HUD layouts: list, save <name>, load <name>, rename <old> <new>, delete <name>."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 1;
      });
   }

   private static HudLayoutStore store() {
      return new HudLayoutStore(ConfigurationManager.ROOT_DIRECTORY.resolve("hud-layouts"));
   }

   private static int listLayouts() {
      try {
         List<String> names = store().list();
         String message = names.isEmpty()
            ? "No saved HUD layouts. Use ;hudlayout save <name> to create one."
            : "Saved HUD layouts: " + String.join(", ", names);
         ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
         return 1;
      } catch (IOException failure) {
         ClientAccess.logger.warn("Could not list HUD layouts", failure);
         ChatUtils.printChat(Component.literal("Could not list saved HUD layouts."), ChatUtils.DEFAULT_SIGNATURE);
         return 0;
      }
   }

   private static int saveLayout(CommandContext<?> context) {
      String name = StringArgumentType.getString(context, "name");
      try {
         JsonObject layout = ClientContext.getHudEditor().exportLayoutPreset();
         store().save(name, layout);
         ChatUtils.printChat(
            Component.literal("Saved HUD layout \"" + name + "\"."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         ClientAccess.logger.warn("Could not save HUD layout {}", name, failure);
         ChatUtils.printChat(
            Component.literal("Could not save HUD layout \"" + name + "\". Check the name and try again."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 0;
      }
   }

   private static int loadLayout(CommandContext<?> context) {
      String name = StringArgumentType.getString(context, "name");
      final JsonObject layout;
      try {
         layout = store().load(name);
      } catch (IOException | IllegalArgumentException failure) {
         ClientAccess.logger.warn("Could not load HUD layout {}", name, failure);
         ChatUtils.printChat(
            Component.literal("Could not load HUD layout \"" + name + "\"."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 0;
      }

      ClientAccess.schizoMinecraft.execute(() -> {
         try {
            ClientContext.getHudEditor().applyLayoutPreset(layout);
            ChatUtils.printChat(
               Component.literal("Loaded HUD layout \"" + name + "\"."),
               ChatUtils.DEFAULT_SIGNATURE
            );
         } catch (IllegalArgumentException failure) {
            ClientAccess.logger.warn("HUD layout {} could not be applied", name, failure);
            ChatUtils.printChat(
               Component.literal("HUD layout \"" + name + "\" is incompatible with this client."),
               ChatUtils.DEFAULT_SIGNATURE
            );
         }
      });
      return 1;
   }

   private static int deleteLayout(CommandContext<?> context) {
      String name = StringArgumentType.getString(context, "name");
      try {
         if (!store().delete(name)) {
            ChatUtils.printChat(Component.literal("HUD layout \"" + name + "\" does not exist."),
               ChatUtils.DEFAULT_SIGNATURE);
            return 0;
         }
         ChatUtils.printChat(Component.literal("Deleted HUD layout \"" + name + "\"."),
            ChatUtils.DEFAULT_SIGNATURE);
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         ClientAccess.logger.warn("Could not delete HUD layout {}", name, failure);
         ChatUtils.printChat(Component.literal("Could not delete HUD layout \"" + name + "\"."),
            ChatUtils.DEFAULT_SIGNATURE);
         return 0;
      }
   }

   private static int renameLayout(CommandContext<?> context) {
      String oldName = StringArgumentType.getString(context, "old_name");
      String newName = StringArgumentType.getString(context, "new_name");
      try {
         store().rename(oldName, newName);
         ChatUtils.printChat(
            Component.literal("Renamed HUD layout \"" + oldName + "\" to \"" + newName + "\"."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         ClientAccess.logger.warn("Could not rename HUD layout {} to {}", oldName, newName, failure);
         ChatUtils.printChat(
            Component.literal("Could not rename HUD layout \"" + oldName + "\"."),
            ChatUtils.DEFAULT_SIGNATURE
         );
         return 0;
      }
   }
}
