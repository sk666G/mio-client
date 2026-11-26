/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.settings.options.MacroType;

public class HoldMacro extends Macro {
   private boolean pressed;

   public HoldMacro(String name, Keybind keybind) {
      super(name, MacroType.HOLD, keybind);
   }

   @Override
   public void run() {
   }

   @Override
   public void setToggled(boolean toggled) {
      super.setToggled(toggled);
      if (!toggled) {
         try {
            this.updatePressed(false);
         } catch (RuntimeException releaseFailure) {
            ClientAccess.logger.warn("Hold macro {} failed while releasing disabled state", this.getName(), releaseFailure);
         }
      }
   }

   public void updatePressed(boolean pressed) {
      if (this.pressed == pressed) {
         return;
      }
      this.pressed = pressed;
      if (this.commands.isEmpty()) {
         return;
      }
      if (this.pressed) {
         String command = (String)this.commands.getFirst();
         Macro.executeCommands(command);
      } else {
         String command = (String)this.commands.getLast();
         Macro.executeCommands(command);
      }
   }

}
