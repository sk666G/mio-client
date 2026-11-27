package dev.schizo.client.diagnostics;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.Module;
import java.util.ArrayList;
import java.util.List;

/** Read-only snapshot of registered module bindings on the client thread. */
public final class ClientBindingDiagnostics {
   private ClientBindingDiagnostics() {}

   public static KeybindOverlap.Snapshot capture() {
      List<Keybind> bindings = new ArrayList<>();
      if (ClientContext.moduleRegistry != null) {
         for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
            if (entry instanceof Module module) bindings.add(module.getKeybind());
         }
      }
      return KeybindOverlap.capture(bindings);
   }
}
