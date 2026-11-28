package dev.schizo.client.gui.components;

import com.google.gson.JsonObject;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.settings.options.PresetCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresetConfirmationTest {
   @Test void requiresSameActionAndReviewedTargetTwice() {
      var state=new PresetConfirmation();
      var target=new PresetData("first",new JsonObject());
      var second=new PresetData("second",new JsonObject());
      var category=PresetCategory.MODULES;
      assertFalse(state.confirm("Save",category,target));
      assertTrue(state.armed("Save"));
      assertFalse(state.confirm("Delete",category,target));
      assertFalse(state.confirm("Delete",category,second));
      assertTrue(state.confirm("Delete",category,second));
      assertFalse(state.armed("Delete"));
   }

   @Test void changedContentsCategoryAndCancellationInvalidateConfirmation() {
      var state=new PresetConfirmation();
      var original=new PresetData("name",new JsonObject());
      var json=new JsonObject();json.addProperty("changed",true);
      var changed=new PresetData("name",json);
      var category=PresetCategory.MODULES;
      assertFalse(state.confirm("Delete",category,original));
      assertFalse(state.confirm("Delete",category,changed));
      assertFalse(state.confirm("Delete",null,changed));
      state.clear();
      assertFalse(state.confirm("Delete",category,changed));
      assertTrue(state.confirm("Delete",category,changed));
   }
}
