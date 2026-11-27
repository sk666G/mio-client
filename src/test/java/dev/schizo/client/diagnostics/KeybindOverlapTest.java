package dev.schizo.client.diagnostics;

import dev.schizo.client.modules.Keybind;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeybindOverlapTest {
   @Test void groupsPhysicalInputsAcrossModesButSeparatesMouseAndKeyboard() {
      var a = Keybind.toggle(65, false);
      var b = a.withMode(Keybind.Mode.HOLD);
      var c = a.withMode(Keybind.Mode.HOLD_REVERSE);
      var mouse = Keybind.toggle(65, true);
      var result = KeybindOverlap.capture(List.of(a, b, c, mouse, Keybind.UNBOUND, Keybind.UNBOUND));
      assertEquals(1, result.groupCount());
      assertEquals(2, result.othersFor(a));
      assertEquals(2, result.othersFor(b));
      assertEquals(0, result.othersFor(mouse));
      assertEquals(0, result.othersFor(Keybind.UNBOUND));
   }

   @Test void countsGroupsRatherThanPairsAndSnapshotsAreIndependent() {
      var a = Keybind.toggle(1, true);
      var b = Keybind.toggle(2, true);
      var bindings = new java.util.ArrayList<>(List.of(a, a, a, b, b));
      var captured = KeybindOverlap.capture(bindings);
      bindings.clear();
      assertEquals(2, captured.groupCount());
      assertEquals(2, captured.othersFor(a));
      assertEquals(1, captured.othersFor(b));
      assertEquals(0, KeybindOverlap.capture(bindings).groupCount());
   }
}
