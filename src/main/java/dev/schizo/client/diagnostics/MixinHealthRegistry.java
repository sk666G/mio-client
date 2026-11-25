package dev.schizo.client.diagnostics;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime evidence that critical mixin callbacks are actually being reached.
 *
 * <p>An unseen hook is not automatically a failure: some hooks require a world, a network
 * connection, input, or shutdown before they can fire. The snapshot makes that distinction
 * explicit instead of silently assuming every optional injection applied successfully.</p>
 */
public final class MixinHealthRegistry {
   private static final Set<String> EXPECTED = Set.of(
      "minecraft.tick",
      "minecraft.runTick",
      "minecraft.destroy",
      "connection.active",
      "connection.receive",
      "connection.send",
      "keyboard.keyPress",
      "renderer.renderLevel"
   );
   private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

   private MixinHealthRegistry() {
   }

   public static void markSeen(String hook) {
      if (hook != null && EXPECTED.contains(hook)) {
         SEEN.add(hook);
      }
   }

   public static Snapshot snapshot() {
      List<String> unseen = EXPECTED.stream()
         .filter(hook -> !SEEN.contains(hook))
         .sorted()
         .toList();
      return new Snapshot(EXPECTED.size(), EXPECTED.size() - unseen.size(), unseen);
   }

   public static void clearObserved() {
      SEEN.clear();
   }

   public record Snapshot(int expected, int seen, List<String> unseen) {
      public Snapshot {
         unseen = List.copyOf(unseen);
      }

      public boolean allObserved() {
         return this.seen == this.expected;
      }
   }
}
