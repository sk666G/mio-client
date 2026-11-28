package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationDeliveryHistoryTest {
   @Test
   void keepsEntriesInChronologicalOrderWithinCapacity() {
      NotificationDeliveryHistory history = new NotificationDeliveryHistory(2);
      history.record(new NotificationDeliveryHistory.Entry(
         1L, NotificationDeliveryHistory.Status.DELIVERED, 3, "Delivered"
      ));
      history.record(new NotificationDeliveryHistory.Entry(
         2L, NotificationDeliveryHistory.Status.FAILED, 2, "IOException"
      ));
      history.record(new NotificationDeliveryHistory.Entry(
         3L, NotificationDeliveryHistory.Status.DROPPED, 1, "Queue full"
      ));

      List<NotificationDeliveryHistory.Entry> entries = history.snapshot();
      assertEquals(2, entries.size());
      assertEquals(2L, entries.get(0).timestampMillis());
      assertEquals(3L, entries.get(1).timestampMillis());
   }

   @Test
   void snapshotsAreImmutableAndHistoryCanBeCleared() {
      NotificationDeliveryHistory history = new NotificationDeliveryHistory();
      history.record(new NotificationDeliveryHistory.Entry(
         1L, NotificationDeliveryHistory.Status.DELIVERED, 1, "Delivered"
      ));

      List<NotificationDeliveryHistory.Entry> snapshot = history.snapshot();
      assertThrows(UnsupportedOperationException.class, snapshot::clear);
      history.clear();

      assertEquals(1, snapshot.size());
      assertTrue(history.snapshot().isEmpty());
   }

   @Test
   void capsDetailAndRejectsInvalidBounds() {
      NotificationDeliveryHistory history = new NotificationDeliveryHistory(1);
      String detail = "x".repeat(120);
      history.record(new NotificationDeliveryHistory.Entry(
         1L, NotificationDeliveryHistory.Status.FAILED, 1, detail
      ));

      assertEquals(96, history.snapshot().get(0).detail().length());
      assertThrows(IllegalArgumentException.class, () -> new NotificationDeliveryHistory(0));
      assertThrows(IllegalArgumentException.class, () -> new NotificationDeliveryHistory(257));
   }
}
