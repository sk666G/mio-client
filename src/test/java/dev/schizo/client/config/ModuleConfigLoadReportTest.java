package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleConfigLoadReportTest {
   @Test
   void storesModuleAndSettingNamesWithoutExceptionMessages() {
      ModuleConfigLoadReport report = new ModuleConfigLoadReport();
      report.record("FastLatency", "Timeout", new IllegalArgumentException("private value should not be exposed"));

      ModuleConfigLoadReport.Snapshot snapshot = report.snapshot();
      assertEquals(1, snapshot.totalIssueCount());
      assertEquals(0, snapshot.omittedIssueCount());
      assertEquals(new ModuleConfigLoadReport.Issue("FastLatency", "Timeout", "IllegalArgumentException"),
         snapshot.issues().getFirst());
   }

   @Test
   void capsRetainedIssueDetailsAndCountsTheRemainder() {
      ModuleConfigLoadReport report = new ModuleConfigLoadReport();
      for (int index = 0; index < ModuleConfigLoadReport.MAX_RETAINED_ISSUES + 7; index++) {
         report.record("Module" + index, "Setting", new IllegalStateException());
      }

      ModuleConfigLoadReport.Snapshot snapshot = report.snapshot();
      assertEquals(ModuleConfigLoadReport.MAX_RETAINED_ISSUES, snapshot.issues().size());
      assertEquals(ModuleConfigLoadReport.MAX_RETAINED_ISSUES + 7, snapshot.totalIssueCount());
      assertEquals(7, snapshot.omittedIssueCount());
   }

   @Test
   void clearResetsBothDetailsAndTotals() {
      ModuleConfigLoadReport report = new ModuleConfigLoadReport();
      report.record("Module", "Setting", new IllegalArgumentException());
      report.clear();

      assertEquals(List.of(), report.snapshot().issues());
      assertEquals(0, report.snapshot().totalIssueCount());
      assertEquals(0, report.snapshot().omittedIssueCount());
   }

   @Test
   void snapshotsAreImmutable() {
      ModuleConfigLoadReport report = new ModuleConfigLoadReport();
      report.record("Module", "Setting", new IllegalArgumentException());

      assertThrows(UnsupportedOperationException.class, () -> report.snapshot().issues().clear());
   }

   @Test
   void normalizesMissingAndExcessivelyLongIssueFields() {
      ModuleConfigLoadReport.Issue issue = new ModuleConfigLoadReport.Issue("  ", "S".repeat(200), null);

      assertEquals("Unknown module", issue.moduleName());
      assertEquals(128, issue.settingName().length());
      assertEquals("UnknownFailure", issue.failureType());
   }
}
