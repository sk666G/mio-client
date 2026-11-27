package dev.schizo.client.profiling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkStoreTest {
   @TempDir Path root;

   @Test void slotsAreIndependentAndRoundTrip() throws IOException {
      var store = new BenchmarkStore(root);
      var session = new BenchmarkSession(100, 1, 10, 12, 20, 0, List.of(10.0));
      assertNull(store.load(BenchmarkStore.Slot.A));
      store.save(BenchmarkStore.Slot.A, session);
      assertEquals(session, store.load(BenchmarkStore.Slot.A));
      assertNull(store.load(BenchmarkStore.Slot.B));
      store.save(BenchmarkStore.Slot.B, session);
      assertEquals(session, store.load(BenchmarkStore.Slot.B));
   }

   @Test void timelineFitsTheThirtySecondGraph() {
      assertThrows(IllegalArgumentException.class, () ->
         new BenchmarkSession(100, 1, 10, 12, 20, 0, java.util.Collections.nCopies(31, 10.0)));
      assertThrows(IllegalArgumentException.class, () ->
         new BenchmarkSession(100, 1, 10, 12, 20, 0, List.of(21.0)));
   }

   @Test void oversizedAndMalformedSummariesAreRejected() throws IOException {
      var store = new BenchmarkStore(root);
      Path path = root.resolve("benchmark-a.json");
      Files.writeString(path, "{}");
      assertThrows(IOException.class, () -> store.load(BenchmarkStore.Slot.A));
      Files.writeString(path, "{\"frames\":10,\"seconds\":1,\"meanMillis\":100,\"p99Millis\":100,\"maxMillis\":100,\"hitches\":10,\"timeline\":[null]}");
      assertThrows(IOException.class, () -> store.load(BenchmarkStore.Slot.A));
      Files.writeString(path, " ".repeat(17000));
      assertThrows(IOException.class, () -> store.load(BenchmarkStore.Slot.A));
   }
}
