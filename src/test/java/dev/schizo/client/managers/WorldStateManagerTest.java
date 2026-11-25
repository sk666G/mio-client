package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.options.DimensionType;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class WorldStateManagerTest {
   @Test
   void resetSessionStateClearsDerivedWorldState() throws Exception {
      WorldStateManager manager = new WorldStateManager();

      setField(manager, "chunkRadius", 12);
      setField(manager, "dimension", DimensionType.THE_END);
      setField(manager, "unconfirmedBreak", BlockPos.ZERO);
      setField(manager, "extraBreak", new BlockPos(1, 2, 3));
      setField(manager, "packetGeneration", 7L);
      manager.getPendingPlacements().put(BlockPos.ZERO, null);
      manager.getBlockEntities().add(null);

      manager.resetSessionState();

      assertEquals(0, manager.getChunkRadius());
      assertEquals(DimensionType.OVERWORLD, manager.getDimension());
      assertNull(manager.getUnconfirmedBreak());
      assertNull(manager.getExtraBreak());
      assertTrue(manager.getPendingPlacements().isEmpty());
      assertTrue(manager.getBlockEntities().isEmpty());
      assertEquals(8L, getLongField(manager, "packetGeneration"));
   }

   private static void setField(Object target, String name, Object value) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
   }

   private static long getLongField(Object target, String name) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.getLong(target);
   }
}
