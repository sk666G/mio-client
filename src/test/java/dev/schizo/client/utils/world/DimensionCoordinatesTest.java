package dev.schizo.client.utils.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DimensionCoordinatesTest {
   @Test
   void savedDimensionAliasesAndNegativeCoordinatesConvertConsistently() {
      assertEquals(1, DimensionCoordinates.scale("THE_NETHER", "minecraft:the_nether"));
      assertEquals(-10, -80 * DimensionCoordinates.scale("overworld", "nether"));
      assertEquals(-80, -10 * DimensionCoordinates.scale("the_nether", "overworld"));
      assertTrue(Double.isNaN(DimensionCoordinates.scale("end", "overworld")));
      assertEquals(1, DimensionCoordinates.scale("custom:moon", "custom:moon"));
   }
}
