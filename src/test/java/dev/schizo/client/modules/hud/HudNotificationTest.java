package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class HudNotificationTest {
   private static final long MILLIS = 1_000_000L;

   @Test
   void tracksLifetimeAndExpiryWithMonotonicTime() {
      AtomicLong clock = new AtomicLong(1_000_000_000L);
      HudNotification notification = new HudNotification("test", 1_000L, null, clock::get);

      assertEquals(0L, notification.getElapsedMillis());
      assertEquals(1_000L, notification.getLifetimeMillis());
      assertFalse(notification.isExpired());

      clock.addAndGet(999L * MILLIS);
      assertEquals(999L, notification.getElapsedMillis());
      assertFalse(notification.isExpired());

      clock.addAndGet(1L * MILLIS);
      assertTrue(notification.isExpired());
   }

   @Test
   void finishAfterShortensTheDeadlineWithoutRestartingElapsedTime() {
      AtomicLong clock = new AtomicLong(0L);
      HudNotification notification = new HudNotification("test", 10_000L, null, clock::get);

      clock.addAndGet(1_000L * MILLIS);
      notification.finishAfter(500L);

      assertEquals(1_000L, notification.getElapsedMillis());
      assertEquals(1_500L, notification.getLifetimeMillis());

      clock.addAndGet(499L * MILLIS);
      assertFalse(notification.isExpired());
      clock.addAndGet(1L * MILLIS);
      assertTrue(notification.isExpired());
   }

   @Test
   void expirySurvivesSignedLongWraparound() {
      AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 50L * MILLIS);
      HudNotification notification = new HudNotification("test", 100L, null, clock::get);

      clock.addAndGet(50L * MILLIS);
      assertEquals(50L, notification.getElapsedMillis());
      assertFalse(notification.isExpired());

      clock.addAndGet(50L * MILLIS);
      assertEquals(100L, notification.getElapsedMillis());
      assertTrue(notification.isExpired());
   }

   @Test
   void negativeDurationsExpireImmediately() {
      AtomicLong clock = new AtomicLong(123L);
      HudNotification notification = new HudNotification("test", -1L, null, clock::get);

      assertEquals(0L, notification.getLifetimeMillis());
      assertTrue(notification.isExpired());
   }
}
