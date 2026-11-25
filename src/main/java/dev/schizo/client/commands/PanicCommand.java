/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.access.ChatLineAccess;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.access.VisibleChatLinesAccess;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.Module;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public final class PanicCommand extends Command {
   public static boolean unloaded;
   private static final String HIDDEN_PREFIX = "ㅤ";

   static {
      unloaded = false;
   }

   public PanicCommand() {
      super("panic");
   }

   private void releaseRuntimeState() {
      if (ClientContext.taskScheduler != null) {
         ClientContext.taskScheduler.clearPendingTasks();
      }
      if (ClientContext.tickRateController != null) {
         ClientContext.tickRateController.clearRequests();
      }
      if (ClientContext.playerTickController != null) {
         ClientContext.playerTickController.clearRequests();
      }
      if (ClientContext.inputOverrides != null) {
         ClientContext.inputOverrides.clearPendingOverrides();
      }
      if (ClientContext.rotationManager != null) {
         ClientContext.rotationManager.resetRuntimeState();
      }
      if (ClientContext.inventoryStateManager != null) {
         ClientContext.inventoryStateManager.resetRuntimeState();
      }
   }

   private void unload() {
      ConfigurationManager configuration = ClientContext.configurationManager;
      configuration.save();
      unloaded = true;
      String previousPrefix = CommandRegistry.getPrefix();
      CommandRegistry.setPrefix(HIDDEN_PREFIX);
      ModuleRegistry modules = ClientContext.moduleRegistry;
      Iterator moduleIterator = ((List)modules.getRegistry()).iterator();

      while (moduleIterator.hasNext()) {
         Module module = (Module)moduleIterator.next();
         Function<Keybind, Keybind> unbind = binding -> {
            return binding.withCode(-1);
         };
         module.modifyKeybind(unbind);
         if (module.isToggled()) {
            module.toggle();
         }
      }
      this.releaseRuntimeState();

      Gui recentChatGui = ClientAccess.schizoMinecraft.gui;
      ChatComponent recentChat = recentChatGui.getChat();
      recentChat.getRecentChat().removeIf((Predicate<String>)message -> message.startsWith(previousPrefix));
      Gui visibleChatGui = ClientAccess.schizoMinecraft.gui;
      ((VisibleChatLinesAccess)visibleChatGui.getChat()).getVisible().removeIf((Predicate<Line>)line -> {
         ChatLineAccess access = (ChatLineAccess)(Object)line;
         if (access.getSignature() != null) {
            MessageSignature signature = access.getSignature();
            ByteBuffer signatureBytes = signature.asByteBuffer();
            if (signatureBytes.getInt() < 0) {
               return true;
            }
         }
         return false;
      });
      Gui remainingChatGui = ClientAccess.schizoMinecraft.gui;
      ChatComponent remainingChat = remainingChatGui.getChat();
      remainingChat.getRecentChat().removeIf((Predicate<String>)message -> message.startsWith(previousPrefix));
   }

   @Override
   public void exec(LiteralArgumentBuilder builder) {
      com.mojang.brigadier.Command<Object> command = context -> {
         this.scheduleRotationTask(() -> {
            Minecraft minecraft = ClientAccess.schizoMinecraft;
            ConfirmScreen confirmationScreen = new ConfirmScreen(
               (BooleanConsumer)(confirmed -> {
                  if (confirmed) {
                     this.unload();
                  }
                  Minecraft currentMinecraft = ClientAccess.schizoMinecraft;
                  currentMinecraft.setScreen(null);
               }),
               Component.literal("Unload the client."),
               Component.literal("Continue?")
            );
            minecraft.setScreen(confirmationScreen);
         });
         return 1;
      };
      builder.executes(command);
   }
}
