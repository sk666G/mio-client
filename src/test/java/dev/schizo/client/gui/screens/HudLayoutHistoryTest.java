package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HudLayoutHistoryTest {
   @Test
   void restoresPreviousStatesAndSupportsRedo() {
      HudLayoutHistory<String> history = new HudLayoutHistory<>(10);

      history.record("A");
      assertEquals("A", history.undo("B"));
      assertTrue(history.canRedo());
      assertFalse(history.canUndo());

      assertEquals("B", history.redo("A"));
      assertTrue(history.canUndo());
      assertFalse(history.canRedo());
   }

   @Test
   void recordingANewEditDiscardsTheRedoBranch() {
      HudLayoutHistory<String> history = new HudLayoutHistory<>(10);

      history.record("A");
      assertEquals("A", history.undo("B"));
      history.record("A");
      assertFalse(history.canRedo());
      assertNull(history.redo("B"));
   }

   @Test
   void keepsOnlyTheMostRecentStatesWithinCapacity() {
      HudLayoutHistory<String> history = new HudLayoutHistory<>(2);
      history.record("A");
      history.record("B");
      history.record("C");

      assertEquals("C", history.undo("D"));
      assertEquals("B", history.undo("C"));
      assertNull(history.undo("B"));
   }

   @Test
   void dragAndResizeEachCreateOneUndoStepAndCanBeReplayed() {
      var history = new HudLayoutHistory<String>(10);
      history.beginEdit("original");
      history.beginEdit("mid-drag");
      history.finishEdit("snapped");
      history.beginEdit("snapped");
      history.finishEdit("resized");
      assertEquals("snapped", history.undo("resized"));
      assertEquals("original", history.undo("snapped"));
      assertFalse(history.canUndo());
      assertEquals("snapped", history.redo("original"));
      assertEquals("resized", history.redo("snapped"));
   }

   @Test
   void cancelOrNoOpAfterUndoPreservesRedo() {
      var history = new HudLayoutHistory<String>(10);
      history.record("original");
      assertEquals("original", history.undo("edited"));
      history.beginEdit("original");
      assertEquals("original", history.cancelEdit());
      history.finishEdit("discarded");
      history.beginEdit("original");
      history.finishEdit("original");
      assertFalse(history.canUndo());
      assertEquals("edited", history.redo("original"));
   }

   @Test
   void rejectsAnInvalidCapacity() {
      assertThrows(IllegalArgumentException.class, () -> new HudLayoutHistory<>(0));
   }
}
