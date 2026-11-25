package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MovementPredictionManagerTest {
   @Test
   @SuppressWarnings("unchecked")
   void resetSessionStateClearsCrossSessionPredictionData() throws Exception {
      MovementPredictionManager manager = new MovementPredictionManager();

      Field previousPosition = MovementPredictionManager.class.getDeclaredField("previousPlayerPosition");
      previousPosition.setAccessible(true);
      previousPosition.set(manager, new Vec3(10.0, 64.0, -5.0));

      Field predictionsField = MovementPredictionManager.class.getDeclaredField("playerPredictions");
      predictionsField.setAccessible(true);
      Map<Integer, Object> predictions = (Map<Integer, Object>)predictionsField.get(manager);
      predictions.put(42, null);

      manager.resetSessionState();

      assertNull(previousPosition.get(manager));
      assertTrue(predictions.isEmpty());
   }

   @Test
   void adjustedMotionIsZeroWithoutUsableLocalState() {
      MovementPredictionManager manager = new MovementPredictionManager();

      assertEquals(Vec3.ZERO, manager.getAdjustedLocalMotion());
   }
}
