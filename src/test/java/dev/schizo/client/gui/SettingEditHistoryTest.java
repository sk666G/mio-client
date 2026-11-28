package dev.schizo.client.gui;

import com.google.gson.JsonPrimitive;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingEditHistoryTest {
   @Test void undoRedoAndBranching() {
      var setting = new NumberSetting<>("Size", 3, 0, 10);
      var history = new SettingEditHistory();
      assertEquals(SettingEditHistory.Result.APPLIED, history.apply(setting, setting.toJson(), new JsonPrimitive(7)));
      assertEquals(7, setting.getValue());
      assertEquals(SettingEditHistory.Result.APPLIED, history.undo());
      assertEquals(3, setting.getValue());
      assertEquals(SettingEditHistory.Result.APPLIED, history.redo());
      assertEquals(7, setting.getValue());
      history.undo();
      history.apply(setting, setting.toJson(), new JsonPrimitive(4));
      assertFalse(history.canRedo());
   }

   @Test void externalChangeAndCallbackSettingsAreNotOverwritten() {
      var setting = new NumberSetting<>("Size", 3, 0, 10);
      var history = new SettingEditHistory();
      history.apply(setting, setting.toJson(), new JsonPrimitive(7));
      setting.setNumber(8);
      assertEquals(SettingEditHistory.Result.STALE, history.undo());
      assertEquals(8, setting.getValue());
      var bool = new BooleanSetting("Callback", false);
      bool.setChangeCallback(() -> fail("must not run"));
      assertEquals(SettingEditHistory.Result.UNSUPPORTED, history.apply(bool, bool.toJson(), new JsonPrimitive(true)));
   }

   @Test void actualClampedValueIsRecordedAndHistoryBounded() {
      var setting = new NumberSetting<>("Size", 3, 0, 1000);
      var history = new SettingEditHistory();
      history.apply(setting, setting.toJson(), new JsonPrimitive(2000));
      assertEquals(1000, setting.getValue());
      history.undo();
      assertEquals(3, setting.getValue());
      for (int i = 0; i < 80; i++) history.apply(setting, setting.toJson(), new JsonPrimitive(i+5));
      for (int i = 0; i < SettingEditHistory.LIMIT; i++) assertEquals(SettingEditHistory.Result.APPLIED, history.undo());
      assertEquals(SettingEditHistory.Result.EMPTY, history.undo());
   }

   @Test void sharedHistoryRevalidatesTargetsAndRefusesUnregisteredEdits() {
      var first = new BooleanSetting("First",false);
      var second = new BooleanSetting("Second",false);
      var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
      var history = new SettingEditHistory(setting -> setting == first || (setting == second && allowed.get()));
      assertEquals(SettingEditHistory.Result.APPLIED,history.apply(first,first.toJson(),new JsonPrimitive(true)));
      assertEquals(SettingEditHistory.Result.APPLIED,history.apply(second,second.toJson(),new JsonPrimitive(true)));
      assertSame(second,history.undoTarget());
      allowed.set(false);
      assertEquals(SettingEditHistory.Result.UNSUPPORTED,history.undo());
      assertTrue(second.getValue());
      assertSame(second,history.undoTarget());
      allowed.set(true);
      assertEquals(SettingEditHistory.Result.APPLIED,history.undo());
      assertSame(first,history.undoTarget());
      assertSame(second,history.redoTarget());
      assertEquals(SettingEditHistory.Result.APPLIED,history.undo());
      assertFalse(first.getValue());
      assertFalse(second.getValue());
   }
}
