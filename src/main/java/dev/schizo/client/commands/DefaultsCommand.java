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
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.render.BlurModule;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.chat.ChatUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class DefaultsCommand extends Command {

   public DefaultsCommand() {
      super("defaults");
   }

   @Override
   public void exec(LiteralArgumentBuilder commandBuilder) {
      com.mojang.brigadier.Command<Object> resetCommand = context -> {
         ConfirmScreen confirmationScreen = new ConfirmScreen((BooleanConsumer)(confirmed -> {
            if (confirmed) {
               ModuleRegistry moduleRegistry = ClientContext.moduleRegistry;
               Iterator moduleIterator = ((List)moduleRegistry.getRegistry()).iterator();

               while (moduleIterator.hasNext()) {
                  Module module = (Module)moduleIterator.next();

                  Iterator settingIterator = module.getSettings().iterator();

                  while (settingIterator.hasNext()) {
                     Setting<?> setting = (Setting<?>)settingIterator.next();
                     setting.reset();
                  }

                  if (module instanceof UIModule) {

                     Keybind defaultUiBinding = new Keybind(defaultUiKeyCode(), Keybind.Mode.TOGGLE, false);
                     module.setKeybind(defaultUiBinding);
                  } else {
                     Keybind defaultBinding = Keybind.UNBOUND;
                     module.setKeybind(defaultBinding);
                  }

                  if (!(module instanceof FontsModule) && !(module instanceof BlurModule)) {
                     module.setToggled(false);
                  }
               }
               this.scheduleRotationTask(() -> ChatUtils.printChat(Component.literal("All modules have been reset to defaults."), ChatUtils.DEFAULT_SIGNATURE));
            }

            Minecraft client = ClientAccess.schizoMinecraft;
            client.setScreen(null);
         }), resetConfirmationTitle(), Component.nullToEmpty("Continue?"));
         Runnable showConfirmation = () -> {
            Minecraft client = ClientAccess.schizoMinecraft;
            client.setScreen(confirmationScreen);
         };
         this.scheduleRotationTask(showConfirmation);
         return 1;
      };
      commandBuilder.executes(resetCommand);
   }

   private static Component resetConfirmationTitle() {
      return Component.nullToEmpty("Reset to defaults.");
   }

   private static int defaultUiKeyCode() {

      return 344;
   }
}
