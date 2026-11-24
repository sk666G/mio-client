package dev.schizo.client.utils.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class PlayerStateTrackerSneakStateTest {
   @Test
   void restoresSneakingAfterSuccessfulTemporarySneak() {
      assertRestoresState(true, true, false, false);
   }

   @Test
   void restoresNonSneakingAfterSuccessfulTemporaryUnsneak() {
      assertRestoresState(false, false, true, true);
   }

   @Test
   void restoresSneakingWhenTemporaryUnsneakActionThrows() {
      assertRestoresState(false, true, false, true);
   }

   @Test
   void restoresNonSneakingWhenTemporarySneakActionThrows() {
      assertRestoresState(true, false, true, false);
   }

   @Test
   void doesNotToggleWhenAlreadyInRequestedState() {
      AtomicBoolean state = new AtomicBoolean(true);
      List<Boolean> changes = new ArrayList<>();

      PlayerStateTracker.runWithSneakingState(true, true, requested -> {
         changes.add(requested);
         state.set(requested);
      }, () -> {
         assertEquals(true, state.get());
      });

      assertEquals(List.of(), changes);
      assertEquals(true, state.get());
   }

   private static void assertRestoresState(
      boolean requestedSneaking,
      boolean initialSneaking,
      boolean actionThrows,
      boolean expectedFinalSneaking
   ) {
      AtomicBoolean state = new AtomicBoolean(initialSneaking);
      List<Boolean> changes = new ArrayList<>();
      Runnable action = () -> {
         assertEquals(requestedSneaking, state.get());
         if (actionThrows) {
            throw new IllegalStateException("simulated action failure");
         }
      };

      if (actionThrows) {
         assertThrows(IllegalStateException.class, () ->
            PlayerStateTracker.runWithSneakingState(requestedSneaking, initialSneaking, requested -> {
               changes.add(requested);
               state.set(requested);
            }, action)
         );
      } else {
         PlayerStateTracker.runWithSneakingState(requestedSneaking, initialSneaking, requested -> {
            changes.add(requested);
            state.set(requested);
         }, action);
      }

      assertEquals(initialSneaking, state.get());
      assertEquals(expectedFinalSneaking, state.get());
      assertEquals(List.of(requestedSneaking, initialSneaking), changes);
   }
}
