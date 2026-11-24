package dev.schizo.client.settings;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class NumberSettingTest {
   @Test
   void invalidInputLeavesCommittedAndStagedValuesAlone() {
      var setting = new NumberSetting<>("Scale", 1.0F, 0.5F, 2.0F);
      assertThrows(IllegalArgumentException.class, () -> setting.setValueFromText("NaN"));
      assertThrows(IllegalArgumentException.class, () -> setting.fromJson(new JsonPrimitive("Infinity")));
      assertThrows(IllegalArgumentException.class, () -> setting.setNumber(Double.MAX_VALUE));
      assertEquals(1.0F, setting.getValue().floatValue());
      assertEquals(1.0F, setting.getStagedValue().floatValue());
   }

   @Test
   void oversizedIntegerInJsonClampsAtTheCorrectEnd() {
      var setting = new NumberSetting<>("Limit", 0, -10, 10);
      setting.fromJson(new JsonPrimitive(2147483648L));
      assertEquals(10, setting.getValue().intValue());
      setting.fromJson(new JsonPrimitive(-2147483649L));
      assertEquals(-10, setting.getValue().intValue());
   }

   @Test
   void doubleBoundsRemainDistinctAtFloatPrecisionLimit() {
      var setting = new NumberSetting<>("Height", 1.0, 0.0, 1.0);
      setting.setValue(Math.nextUp(1.0));
      assertEquals(1.0, setting.getValue().doubleValue());
   }
}
