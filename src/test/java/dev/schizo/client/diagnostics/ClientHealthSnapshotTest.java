package dev.schizo.client.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClientHealthSnapshotTest {
   @Test
   void healthySnapshotHasNoWarnings() {
      ClientHealthSnapshot snapshot = new ClientHealthSnapshot(
         false, false, false, false, 0, 0L, 0, 0, false
      );
      assertFalse(snapshot.hasWarnings());
   }

   @Test
   void diagnosticProblemsRaiseAttentionWithoutTreatingSafeModeItselfAsFailure() {
      assertFalse(new ClientHealthSnapshot(true, true, false, false, 0, 0L, 0, 0, false).hasWarnings());
      assertTrue(new ClientHealthSnapshot(false, false, true, false, 0, 0L, 0, 0, false).hasWarnings());
      assertTrue(new ClientHealthSnapshot(false, false, false, true, 0, 0L, 0, 0, false).hasWarnings());
      assertTrue(new ClientHealthSnapshot(false, false, false, false, 1, 0L, 0, 0, false).hasWarnings());
      assertTrue(new ClientHealthSnapshot(false, false, false, false, 0, 1L, 0, 0, false).hasWarnings());
      assertTrue(new ClientHealthSnapshot(false, false, false, false, 0, 0L, 0, 1, false).hasWarnings());
   }
}
