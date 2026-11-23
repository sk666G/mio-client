/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.util.ArrayList;
import java.util.List;

/** Bounded, value-free diagnostics for rejected module configuration entries. */
public final class ModuleConfigLoadReport {
   public static final int MAX_RETAINED_ISSUES = 128;

   private final List<Issue> issues = new ArrayList<>();
   private int totalIssueCount;

   public synchronized void clear() {
      this.issues.clear();
      this.totalIssueCount = 0;
   }

   public synchronized void record(Issue issue) {
      if (issue == null) {
         return;
      }
      this.totalIssueCount++;
      if (this.issues.size() < MAX_RETAINED_ISSUES) {
         this.issues.add(issue);
      }
   }

   public synchronized void record(String moduleName, String settingName, Throwable failure) {
      String failureType = failure == null ? "UnknownFailure" : failure.getClass().getSimpleName();
      this.record(new Issue(moduleName, settingName, failureType));
   }

   public synchronized Snapshot snapshot() {
      return new Snapshot(this.issues, this.totalIssueCount);
   }

   public record Issue(String moduleName, String settingName, String failureType) {
      public Issue {
         moduleName = normalize(moduleName, "Unknown module");
         settingName = normalize(settingName, "(module)");
         failureType = normalize(failureType, "UnknownFailure");
      }
   }

   public record Snapshot(List<Issue> issues, int totalIssueCount) {
      public Snapshot {
         issues = List.copyOf(issues);
         totalIssueCount = Math.max(0, totalIssueCount);
      }

      public int omittedIssueCount() {
         return Math.max(0, this.totalIssueCount - this.issues.size());
      }
   }

   private static String normalize(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      String trimmed = value.strip();
      return trimmed.length() <= 128 ? trimmed : trimmed.substring(0, 128);
   }
}
