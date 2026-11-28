package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DirectionModuleTest {
   @Test
   void directionDetailsJoinWithoutFormatterArtifacts() {
      assertEquals(
         "-45.0, 12.5, +X -Z",
         DirectionModule.joinDirectionDetails("-45.0, 12.5", "+X -Z")
      );
   }
}
