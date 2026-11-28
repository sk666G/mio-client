package dev.schizo.client.gui.screens;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresetImportSnapshotTest {
   @Test void rejectsChangedCategoryOrNestedValues() {
      var source=JsonParser.parseString("{\"category\":\"HUD\",\"values\":{\"scale\":1}}").getAsJsonObject();
      var snapshot=new PresetImportSnapshot(source);
      assertTrue(snapshot.matches(source.deepCopy()));
      source.getAsJsonObject("values").addProperty("scale",2);
      assertFalse(snapshot.matches(source));
      var changedCategory=snapshot.copy();
      changedCategory.addProperty("category","Other");
      assertFalse(snapshot.matches(changedCategory));
   }

   @Test void copyCannotModifyReviewedSnapshot() {
      var source=JsonParser.parseString("{\"name\":\"test\"}").getAsJsonObject();
      var snapshot=new PresetImportSnapshot(source);
      snapshot.copy().remove("name");
      assertTrue(snapshot.matches(source));
   }
}
