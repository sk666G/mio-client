package dev.schizo.client.utils.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PlayerStateTrackerDisconnectStateTest {
   @Test
   void clearsPacketMovementAndInputStateAtSessionBoundary() {
      PlayerStateTracker.SessionState state = new PlayerStateTracker.SessionState();
      state.packetX = 12.5;
      state.packetY = 68.0;
      state.packetZ = -4.25;
      state.packetOnGround = true;
      state.sneaking = true;
      state.lastSentInput = new Input(true, false, true, false, true, true, true);
      state.lastGroundedY = 64.0;
      state.currentTickY = 68.0;
      state.previousTickY = 67.5;
      state.groundedTicks = 12;
      state.airborneTicks = 3;
      state.fallDistance = 5.5F;
      state.packetRotations = new float[] { 90.0F, 30.0F };
      state.tickDisplacement = new Vec3(0.5, 0.5, -0.25);

      state.reset();

      assertEquals(0.0, state.packetX);
      assertEquals(0.0, state.packetY);
      assertEquals(0.0, state.packetZ);
      assertFalse(state.packetOnGround);
      assertFalse(state.sneaking);
      assertNull(state.lastSentInput);
      assertEquals(0.0, state.lastGroundedY);
      assertEquals(0.0, state.currentTickY);
      assertEquals(0.0, state.previousTickY);
      assertEquals(0, state.groundedTicks);
      assertEquals(0, state.airborneTicks);
      assertEquals(0.0F, state.fallDistance);
      assertArrayEquals(new float[2], state.packetRotations);
      assertNull(state.tickDisplacement);
   }
}
