package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.modules.misc.StashEntry;
import dev.schizo.client.modules.misc.StashFinderModule;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class StashRegistryTest {
   @Test
   void persistedStashesRoundTripWhenLoggingIsEnabled() throws Exception {
      StashFinderModule stashFinder = new StashFinderModule();
      Object previousBinding = replaceStashFinderBinding(stashFinder);
      try {
         StashRegistry source = new StashRegistry();
         StashEntry first = new StashEntry("first", 100, -200, "OVERWORLD", "example.org");
         StashEntry second = new StashEntry("second", -32, 48, "NETHER", "other.example");
         assertTrue(source.registerEntry(first));
         assertTrue(source.registerEntry(second));

         JsonObject serialized = source.toJson().getAsJsonObject();
         assertTrue(serialized.has("stashes"));
         assertEquals(2, serialized.getAsJsonArray("stashes").size());

         StashRegistry restored = new StashRegistry();
         restored.fromJson(serialized);

         assertEquals(2, restored.getRegistry().size());
         java.util.Iterator<StashEntry> restoredEntries = restored.getRegistry().iterator();
         assertSameStash(first, restoredEntries.next());
         assertSameStash(second, restoredEntries.next());
      } finally {
         restoreStashFinderBinding(previousBinding);
      }
   }

   @Test
   void disabledLoggingSerializesIntentionalEmptyStateThatCanBeReloaded() throws Exception {
      StashFinderModule stashFinder = new StashFinderModule();
      stashFinder.logSetting.setValue(false);
      Object previousBinding = replaceStashFinderBinding(stashFinder);
      try {
         StashRegistry source = new StashRegistry();
         assertTrue(source.registerEntry(new StashEntry("stash", 100, -200, "OVERWORLD", "example.org")));

         JsonObject serialized = source.toJson().getAsJsonObject();
         assertTrue(serialized.entrySet().isEmpty());

         StashRegistry restored = new StashRegistry();
         StashEntry liveEntry = new StashEntry("live", 1, 2, "OVERWORLD", "live.example");
         assertTrue(restored.registerEntry(liveEntry));

         assertDoesNotThrow(() -> restored.fromJson(serialized));
         assertEquals(1, restored.getRegistry().size());
         assertSame(liveEntry, restored.getRegistry().iterator().next());
      } finally {
         restoreStashFinderBinding(previousBinding);
      }
   }

   @Test
   void emptyRootRepresentsNoPersistedStashDataAndPreservesLiveState() {
      StashRegistry registry = new StashRegistry();
      StashEntry existing = new StashEntry("stash", 100, -200, "OVERWORLD", "example.org");
      assertTrue(registry.registerEntry(existing));

      assertDoesNotThrow(() -> registry.fromJson(new JsonObject()));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, registry.getRegistry().iterator().next());
   }

   @Test
   void malformedNonEmptyRootThrowsAndPreservesExistingState() {
      StashRegistry registry = new StashRegistry();
      StashEntry existing = new StashEntry("stash", 100, -200, "OVERWORLD", "example.org");
      assertTrue(registry.registerEntry(existing));

      JsonObject malformed = new JsonObject();
      malformed.addProperty("unexpected", true);
      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(malformed));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, registry.getRegistry().iterator().next());
   }

   private static Object replaceStashFinderBinding(StashFinderModule replacement) throws Exception {
      Field field = StashRegistry.class.getDeclaredField("stashFinderModule");
      field.setAccessible(true);
      Object previous = field.get(null);
      field.set(null, replacement);
      return previous;
   }

   private static void restoreStashFinderBinding(Object previous) throws Exception {
      Field field = StashRegistry.class.getDeclaredField("stashFinderModule");
      field.setAccessible(true);
      field.set(null, previous);
   }

   private static void assertSameStash(StashEntry expected, StashEntry actual) {
      assertEquals(expected.getName(), actual.getName());
      assertEquals(expected.getX(), actual.getX());
      assertEquals(expected.getZ(), actual.getZ());
      assertEquals(expected.getDimension(), actual.getDimension());
      assertEquals(expected.getServer(), actual.getServer());
      assertEquals(expected.firstSeenMillis(), actual.firstSeenMillis());
      assertEquals(expected.lastSeenMillis(), actual.lastSeenMillis());
   }
}
