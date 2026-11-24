package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExplosionExposureMathTest {
   @Test
   void samplesAllEightCornersForDegenerateBounds() {
      AtomicInteger calls = new AtomicInteger();

      float exposure = ExplosionExposureMath.calculate(
         2.0D, 3.0D, 4.0D,
         2.0D, 3.0D, 4.0D,
         10.0D, 11.0D, 12.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> calls.getAndIncrement() < 4
      );

      assertEquals(8, calls.get());
      assertEquals(0.5F, exposure);
   }

   @Test
   void forwardsTheExplosionEndpointToEachVisibilityTest() {
      AtomicInteger calls = new AtomicInteger();

      ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         7.0D, 8.0D, 9.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> {
            assertEquals(7.0D, explosionX);
            assertEquals(8.0D, explosionY);
            assertEquals(9.0D, explosionZ);
            calls.incrementAndGet();
            return true;
         }
      );

      assertEquals(8, calls.get());
   }

   @Test
   void doesNotCallVisibilityTesterWhenSampleStepIsNegative() {
      AtomicInteger calls = new AtomicInteger();

      float exposure = ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         -1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> {
            calls.incrementAndGet();
            return true;
         }
      );

      assertEquals(0.0F, exposure);
      assertEquals(0, calls.get());
   }

   @Test
   void requiresAVisibilityTester() {
      assertThrows(NullPointerException.class, () -> ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D,
         null
      ));
   }
}
