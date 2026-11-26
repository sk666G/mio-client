package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConfigurationSaveGateTest {
   @Test
   void newerAsyncGenerationSupersedesOlderPendingWrite() {
      ConfigurationSaveGate gate = new ConfigurationSaveGate();
      long older = gate.reserveAsyncSave();
      long newer = gate.reserveAsyncSave();
      AtomicInteger writes = new AtomicInteger();

      gate.runIfCurrent(older, () -> writes.addAndGet(10));
      gate.runIfCurrent(newer, writes::incrementAndGet);

      assertEquals(1, writes.get());
   }

   @Test
   void synchronousSaveInvalidatesPendingAsyncGeneration() {
      ConfigurationSaveGate gate = new ConfigurationSaveGate();
      long pendingAsync = gate.reserveAsyncSave();
      AtomicInteger state = new AtomicInteger();

      gate.invalidatePendingAndRun(() -> state.set(2));
      gate.runIfCurrent(pendingAsync, () -> state.set(1));

      assertEquals(2, state.get());
   }

   @Test
   void asyncSaveReservedAfterSynchronousSaveCanRun() {
      ConfigurationSaveGate gate = new ConfigurationSaveGate();
      AtomicInteger writes = new AtomicInteger();

      gate.invalidatePendingAndRun(writes::incrementAndGet);
      long async = gate.reserveAsyncSave();
      gate.runIfCurrent(async, writes::incrementAndGet);

      assertEquals(2, writes.get());
   }

   @Test
   void closingAsyncSavesInvalidatesPendingWorkAndRejectsNewReservations() {
      ConfigurationSaveGate gate = new ConfigurationSaveGate();
      long pendingAsync = gate.reserveAsyncSave();
      AtomicInteger writes = new AtomicInteger();

      gate.closeAsyncSaves();
      gate.closeAsyncSaves();

      assertEquals(ConfigurationSaveGate.NO_ASYNC_SAVE, gate.reserveAsyncSave());
      gate.runIfCurrent(pendingAsync, writes::incrementAndGet);
      assertEquals(0, writes.get());

      gate.invalidatePendingAndRun(writes::incrementAndGet);
      assertEquals(1, writes.get());
   }

   @Test
   void rejectsMissingActions() {
      ConfigurationSaveGate gate = new ConfigurationSaveGate();
      long generation = gate.reserveAsyncSave();

      assertThrows(NullPointerException.class, () -> gate.runIfCurrent(generation, null));
      assertThrows(NullPointerException.class, () -> gate.invalidatePendingAndRun(null));
   }
}
