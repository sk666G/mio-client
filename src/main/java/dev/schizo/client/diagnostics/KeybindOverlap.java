package dev.schizo.client.diagnostics;

import dev.schizo.client.modules.Keybind;
import java.util.HashMap;
import java.util.Map;

/** Physical-input overlaps, not evidence of incompatible module behavior. */
public final class KeybindOverlap {
   private KeybindOverlap() {}

   private record Input(int code, boolean mouse) {}

   public static final class Snapshot {
      private final Map<Input, Integer> counts;
      private Snapshot(Map<Input, Integer> counts) { this.counts = Map.copyOf(counts); }

      public int groupCount() {
         int groups = 0;
         for (int count : counts.values()) if (count > 1) groups++;
         return groups;
      }

      /** Other bindings on the same input, assuming the queried binding was captured. */
      public int othersFor(Keybind binding) {
         if (binding == null || binding.isUnbound()) return 0;
         return Math.max(0, counts.getOrDefault(new Input(binding.code(), binding.mouse()), 0) - 1);
      }
   }

   public static Snapshot capture(Iterable<Keybind> bindings) {
      Map<Input, Integer> counts = new HashMap<>();
      for (Keybind binding : bindings) {
         if (binding == null || binding.isUnbound()) continue;
         counts.merge(new Input(binding.code(), binding.mouse()), 1, Integer::sum);
      }
      return new Snapshot(counts);
   }
}
