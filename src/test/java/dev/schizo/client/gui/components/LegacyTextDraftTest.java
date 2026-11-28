package dev.schizo.client.gui.components;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyTextDraftTest {
   @Test void movesAndDeletesCompleteSupplementaryCodePoints() {
      var draft=new LegacyTextDraft("A😀B");
      draft.left();
      assertEquals(3,draft.caret());
      draft.backspace();
      assertEquals("AB",draft.text());
      assertEquals(1,draft.caret());
      draft.insert("🌙");
      assertEquals("A🌙B",draft.text());
      assertEquals(3,draft.caret());
      draft.left();
      draft.delete();
      assertEquals("AB",draft.text());
   }

   @Test void draftPreservesOriginalAndDetectsExternalChanges() {
      var draft=new LegacyTextDraft("before");
      assertFalse(draft.changed());
      draft.home();
      draft.insert("new ");
      assertTrue(draft.changed());
      assertTrue(draft.matchesLive(new String("before")));
      assertFalse(draft.matchesLive("other"));
      assertEquals("new before",draft.text());
   }

   @Test void cursorOperationsAtBoundariesAreSafe() {
      var draft=new LegacyTextDraft("");
      draft.left();draft.right();draft.backspace();draft.delete();
      assertEquals(0,draft.caret());
      draft.insert("😀");
      draft.home();
      draft.right();
      assertEquals(2,draft.caret());
      draft.end();
      draft.backspace();
      assertEquals("",draft.text());
      assertFalse(draft.changed());
   }
}
