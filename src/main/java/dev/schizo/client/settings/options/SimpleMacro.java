/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.Macro;
import java.util.Iterator;

public class SimpleMacro extends Macro {

   public SimpleMacro(String name, Keybind keybind) {
      super(name, MacroType.SIMPLE, keybind);
   }

   @Override
   public void run() {
      Iterator commands = this.commands.iterator();
      while (commands.hasNext()) {
         String command = (String)commands.next();
         Macro.executeCommands(command);
      }
   }

}
