package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProfilerDisplayRowTest {
   @Test
   void preservesColumnOrderAndFormatting() {
      ProfilerDisplayRow row = new ProfilerDisplayRow("Widget", 3L, 7_500_000L, 2_500_000L, 4_000_000L);
      ProfilerDisplayRow.Values values = row.values();

      assertEquals("3", values.calls());
      assertEquals("2.500", values.average());
      assertEquals("4.000", values.maximum());
      assertEquals("7.500", values.total());
   }

   @Test
   void reusesFormattedValuesAcrossFrames() {
      ProfilerDisplayRow row = new ProfilerDisplayRow("Widget", 1L, 0L, 0L, 0L);
      ProfilerDisplayRow.Values first = row.values();

      assertSame(first, row.values());
      assertEquals("0.000", first.average());
   }

   @Test
   void retainsLongCallCounts() {
      ProfilerDisplayRow row = new ProfilerDisplayRow("Widget", Long.MAX_VALUE, 0L, 0L, 0L);

      assertEquals("9223372036854775807", row.values().calls());
   }

   @Test
   void cachesAbbreviationsAndInvalidatesOnWidthChange() {
      ProfilerDisplayRow row = new ProfilerDisplayRow("LongWidgetName", 0L, 0L, 0L, 0L);

      String narrow = row.displayName(5);
      assertEquals("Long…", narrow);
      assertSame(narrow, row.displayName(5));
      assertEquals("LongWidgetName", row.displayName(20));
      assertEquals("…", row.displayName(0));
      assertEquals("Long…", row.displayName(5));
   }
}
