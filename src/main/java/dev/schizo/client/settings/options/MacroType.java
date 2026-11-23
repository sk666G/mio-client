/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.modules.misc.HoldMacro;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.settings.NamedEntry;

import java.util.function.BiFunction;

public enum MacroType implements NamedEntry {
   SIMPLE("simple", SimpleMacro::new),
   QUEUE("queue", QueuedMacro::new),
   DOUBLE_TAP("double_tap", DoubleTapMacro::new),
   HOLD("hold", HoldMacro::new);

   private final String name;
   private final BiFunction<String, Keybind, Macro> factory;

   @Override
   public String getName() {
      return this.name;
   }

   MacroType(String name, BiFunction<String, Keybind, Macro> factory) {
      this.name = name;
      this.factory = factory;
   }

   public Macro create(String name, Keybind keybind) {
      return this.factory.apply(name, keybind);
   }

   public static MacroType fromName(String name) {
      for (MacroType type : values()) {
         String typeName = type.getName();
         if (typeName.equalsIgnoreCase(name)) {
            return type;
         }
      }

      return null;
   }
}
