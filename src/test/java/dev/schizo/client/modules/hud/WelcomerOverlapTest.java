package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WelcomerOverlapTest {
   @Test
   void includesTheExactDistanceBoundary() {
      assertTrue(WelcomerModule.isWithinOverlapDistance(0.0F, 0.0F, 100.0F, 0.0F, 100.0D));
      assertTrue(WelcomerModule.isWithinOverlapDistance(0.0F, 0.0F, 60.0F, 80.0F, 100.0D));
   }

   @Test
   void rejectsCoordinatesOutsideTheDistanceBoundary() {
      assertFalse(WelcomerModule.isWithinOverlapDistance(0.0F, 0.0F, 101.0F, 0.0F, 100.0D));
      assertFalse(WelcomerModule.isWithinOverlapDistance(0.0F, 0.0F, 61.0F, 80.0F, 100.0D));
   }

   @Test
   void preservesExistingIntegerCoordinateTruncation() {
      assertTrue(WelcomerModule.isWithinOverlapDistance(0.9F, 0.9F, 100.9F, 0.9F, 100.0D));
      assertFalse(WelcomerModule.isWithinOverlapDistance(-0.9F, 0.0F, 101.0F, 0.0F, 100.0D));
   }
}
