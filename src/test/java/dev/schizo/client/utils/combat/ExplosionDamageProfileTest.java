package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplosionDamageProfileTest {
   @Test
   void defensivelyCopiesBlockingReductionInputs() {
      List<ExplosionBlockingDamageMath.Reduction> reductions = new ArrayList<>();
      reductions.add(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 0.0F, 0.5F));

      ExplosionDamageProfile profile = profile(reductions);
      reductions.clear();

      assertEquals(1, profile.blockingReductions().size());
      assertThrows(UnsupportedOperationException.class, () ->
         profile.blockingReductions().clear());
   }

   @Test
   void rejectsMissingIdentityAndInvalidBounds() {
      assertThrows(NullPointerException.class, () -> new ExplosionDamageProfile(
         1, null, 1L, 1L, "minecraft:overworld", false, 20.0F,
         0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D, 1.0D,
         0.0F, 0.0F, false, 1.0F, false, 0, false, List.of(),
         ExplosionDamageMath.Difficulty.NORMAL
      ));
      assertThrows(IllegalArgumentException.class, () -> new ExplosionDamageProfile(
         1, "uuid", 1L, 1L, "minecraft:overworld", false, 20.0F,
         2.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D, 1.0D,
         0.0F, 0.0F, false, 1.0F, false, 0, false, List.of(),
         ExplosionDamageMath.Difficulty.NORMAL
      ));
   }

   static ExplosionDamageProfile profile(List<ExplosionBlockingDamageMath.Reduction> reductions) {
      return new ExplosionDamageProfile(
         42,
         "00000000-0000-0000-0000-00000000002a",
         7L,
         12L,
         "minecraft:overworld",
         false,
         20.0F,
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D,
         0.0D, 1.0D,
         0.0F,
         0.0F,
         false,
         1.0F,
         false,
         0,
         false,
         reductions,
         ExplosionDamageMath.Difficulty.NORMAL
      );
   }
}
