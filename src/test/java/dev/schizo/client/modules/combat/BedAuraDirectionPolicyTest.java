package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class BedAuraDirectionPolicyTest {
   @Test
   void selectsTheFirstAvailableStrictDirection() {
      assertEquals(
         Direction.EAST,
         BedAuraModule.selectInteractionDirection(List.of(Direction.EAST, Direction.SOUTH), Direction.UP)
      );
   }

   @Test
   void usesTheFallbackWhenNoStrictDirectionExists() {
      assertEquals(
         Direction.UP,
         BedAuraModule.selectInteractionDirection(List.of(), Direction.UP)
      );
   }
}
