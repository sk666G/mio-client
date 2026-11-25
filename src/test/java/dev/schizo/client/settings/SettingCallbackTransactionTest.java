package dev.schizo.client.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SettingCallbackTransactionTest {
   @Test
   void failedCallbackRestoresCommittedStagedAndPreviousValues() {
      BooleanSetting setting = new BooleanSetting("Toggle", false);
      AtomicInteger callbacks = new AtomicInteger();
      setting.setChangeCallback(() -> {
         callbacks.incrementAndGet();
         throw new IllegalStateException("callback failure");
      });

      assertThrows(IllegalStateException.class, () -> setting.setValue(true));

      assertFalse(setting.getValue());
      assertFalse(setting.getStagedValue());
      assertFalse(setting.getPreviousValue());
      assertEquals(1, callbacks.get());
   }

   @Test
   void successfulCallbackCommitsNormally() {
      BooleanSetting setting = new BooleanSetting("Toggle", false);
      AtomicInteger callbacks = new AtomicInteger();
      setting.setChangeCallback(callbacks::incrementAndGet);

      setting.setValue(true);

      assertTrue(setting.getValue());
      assertTrue(setting.getStagedValue());
      assertTrue(setting.getPreviousValue());
      assertEquals(1, callbacks.get());
   }
}
