package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.render.RenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RenderStateManagerTest {
   @AfterEach
   void clearRegisteredStates() {
      RenderStateManager.reset();
   }

   @Test
   void clearsRegisteredStatesWhenDrawThrows() {
      IllegalStateException expected = new IllegalStateException("render failed");
      RenderState state = entering -> {};

      IllegalStateException actual = assertThrows(
         IllegalStateException.class,
         () -> RenderStateManager.runWithStates(
            () -> {
               assertTrue(RenderStateManager.isActive());
               throw expected;
            },
            state
         )
      );

      assertSame(expected, actual);
      assertFalse(RenderStateManager.isActive());
   }

   @Test
   void clearsRegisteredStatesAfterSuccessfulDraw() {
      RenderState state = entering -> {};

      RenderStateManager.runWithStates(
         () -> assertTrue(RenderStateManager.isActive()),
         state
      );

      assertFalse(RenderStateManager.isActive());
   }
}
