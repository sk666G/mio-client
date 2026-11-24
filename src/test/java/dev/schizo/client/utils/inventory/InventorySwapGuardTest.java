package dev.schizo.client.utils.inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventorySwapGuardTest {
   @Test
   void runsCleanupAfterTheActionInOrder() {
      List<String> calls = new ArrayList<>();

      InventorySwapGuard.runWithCleanup(
         () -> calls.add("clicks"),
         () -> calls.add("cursor-restore")
      );

      assertEquals(List.of("clicks", "cursor-restore"), calls);
   }

   @Test
   void restoresCursorAfterAnActionFailureAndPropagatesTheSameFailure() {
      List<String> calls = new ArrayList<>();
      IllegalStateException expected = new IllegalStateException("second click failed");

      IllegalStateException actual = assertThrows(
         IllegalStateException.class,
         () -> InventorySwapGuard.runWithCleanup(
            () -> {
               calls.add("first click");
               throw expected;
            },
            () -> calls.add("restore cursor")
         )
      );

      assertSame(expected, actual);
      assertEquals(List.of("first click", "restore cursor"), calls);
   }

   @Test
   void keepsActionFailureAndSuppressesCursorRestoreFailure() {
      IllegalStateException actionFailure = new IllegalStateException("click failed");
      IllegalArgumentException restoreFailure = new IllegalArgumentException("cursor restore failed");

      IllegalStateException actual = assertThrows(
         IllegalStateException.class,
         () -> InventorySwapGuard.runWithCleanup(
            () -> { throw actionFailure; },
            () -> { throw restoreFailure; }
         )
      );

      assertSame(actionFailure, actual);
      assertArrayEquals(new Throwable[]{restoreFailure}, actual.getSuppressed());
   }

   @Test
   void propagatesCursorRestoreFailureAfterSuccessfulClicks() {
      IllegalArgumentException expected = new IllegalArgumentException("cursor restore failed");

      IllegalArgumentException actual = assertThrows(
         IllegalArgumentException.class,
         () -> InventorySwapGuard.runWithCleanup(
            () -> { },
            () -> { throw expected; }
         )
      );

      assertSame(expected, actual);
   }

   @Test
   void runsSwapActionAndRestoreInOrder() {
      List<String> calls = new ArrayList<>();

      InventorySwapGuard.run(
         () -> calls.add("swap-in"),
         () -> calls.add("action"),
         () -> calls.add("swap-back")
      );

      assertEquals(List.of("swap-in", "action", "swap-back"), calls);
   }

   @Test
   void restoresAfterActionFailureAndPropagatesTheSameFailure() {
      List<String> calls = new ArrayList<>();
      RuntimeException expected = new RuntimeException("action failed");

      RuntimeException actual = assertThrows(
         RuntimeException.class,
         () -> InventorySwapGuard.run(
            () -> calls.add("swap-in"),
            () -> {
               calls.add("action");
               throw expected;
            },
            () -> calls.add("swap-back")
         )
      );

      assertSame(expected, actual);
      assertEquals(List.of("swap-in", "action", "swap-back"), calls);
      assertEquals(0, actual.getSuppressed().length);
   }

   @Test
   void preservesActionFailureAndSuppressesRestoreFailure() {
      IllegalStateException actionFailure = new IllegalStateException("action failed");
      IllegalArgumentException restoreFailure = new IllegalArgumentException("restore failed");

      IllegalStateException actual = assertThrows(
         IllegalStateException.class,
         () -> InventorySwapGuard.run(
            () -> { },
            () -> { throw actionFailure; },
            () -> { throw restoreFailure; }
         )
      );

      assertSame(actionFailure, actual);
      assertArrayEquals(new Throwable[]{restoreFailure}, actual.getSuppressed());
   }

   @Test
   void doesNotAttemptRestoreWhenSwapInFails() {
      List<String> calls = new ArrayList<>();
      IllegalStateException expected = new IllegalStateException("swap failed");

      IllegalStateException actual = assertThrows(
         IllegalStateException.class,
         () -> InventorySwapGuard.run(
            () -> {
               calls.add("swap-in");
               throw expected;
            },
            () -> calls.add("action"),
            () -> calls.add("swap-back")
         )
      );

      assertSame(expected, actual);
      assertEquals(List.of("swap-in"), calls);
   }

   @Test
   void propagatesRestoreFailureAfterSuccessfulAction() {
      IllegalArgumentException expected = new IllegalArgumentException("restore failed");

      IllegalArgumentException actual = assertThrows(
         IllegalArgumentException.class,
         () -> InventorySwapGuard.run(
            () -> { },
            () -> { },
            () -> { throw expected; }
         )
      );

      assertSame(expected, actual);
   }

   @Test
   void requiresBothCleanupOperations() {
      assertThrows(NullPointerException.class,
         () -> InventorySwapGuard.runWithCleanup(null, () -> { }));
      assertThrows(NullPointerException.class,
         () -> InventorySwapGuard.runWithCleanup(() -> { }, null));
   }

   @Test
   void requiresAllOperations() {
      assertThrows(NullPointerException.class,
         () -> InventorySwapGuard.run(null, () -> { }, () -> { }));
      assertThrows(NullPointerException.class,
         () -> InventorySwapGuard.run(() -> { }, null, () -> { }));
      assertThrows(NullPointerException.class,
         () -> InventorySwapGuard.run(() -> { }, () -> { }, null));
   }
}
