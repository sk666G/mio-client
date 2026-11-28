package dev.schizo.client.gui.screens;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InspectorEditDraftTest {
   @Test void dirtyDraftKeepsOriginalExpectedValueAcrossRepeatedRebuilds() {
      var original = new JsonPrimitive(10);
      var draft = ModuleInspectorScreen.EditDraft.empty().refresh(original, false).withText("12.5");
      draft = draft.refresh(new JsonPrimitive(11), true).refresh(new JsonPrimitive(13), true);
      assertEquals(original, draft.expected());
      assertEquals("12.5", draft.text());
   }

   @Test void cleanDraftReloadsLiveValue() {
      var draft = ModuleInspectorScreen.EditDraft.empty().refresh(new JsonPrimitive(10), false);
      draft = draft.refresh(new JsonPrimitive(11), true);
      assertEquals(new JsonPrimitive(11), draft.expected());
      assertEquals("11", draft.text());
   }

   @Test void invalidOrEmptyInputSurvivesResizeButExplicitSelectionResetsIt() {
      var draft = ModuleInspectorScreen.EditDraft.empty().refresh(new JsonPrimitive(true), false).withText("");
      assertEquals("", draft.refresh(new JsonPrimitive(false), true).text());
      draft = draft.refresh(new JsonPrimitive(false), false);
      assertEquals("false", draft.text());
      assertEquals(new JsonPrimitive(false), draft.expected());
   }

   @Test void unavailableSettingClearsDraftAndComparisonValue() {
      var draft = ModuleInspectorScreen.EditDraft.empty().refresh(new JsonPrimitive(10), false).withText("invalid");
      assertEquals("invalid", draft.refresh(new JsonPrimitive(11), true).text());
      draft = draft.refresh(null, true);
      assertNull(draft.expected());
      assertEquals("", draft.text());
   }
}
