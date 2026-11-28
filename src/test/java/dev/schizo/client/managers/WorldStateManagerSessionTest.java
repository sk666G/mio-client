package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.item.ItemStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldStateManagerSessionTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void disconnectClearsHandSnapshotsAndChunkRadius() throws Exception {
      WorldStateManager manager = new WorldStateManager();
      try {
         Field mainHand = WorldStateManager.class.getDeclaredField("mainHandSnapshot");
         Field offHand = WorldStateManager.class.getDeclaredField("offHandSnapshot");
         Field chunkRadius = WorldStateManager.class.getDeclaredField("chunkRadius");
         mainHand.setAccessible(true);
         offHand.setAccessible(true);
         chunkRadius.setAccessible(true);

         mainHand.set(manager, null);
         offHand.set(manager, null);
         chunkRadius.setInt(manager, 12);

         Method disconnect = WorldStateManager.class.getDeclaredMethod(
            "onMinecraftDisconnectEvent",
            MinecraftDisconnectEvent.class
         );
         disconnect.setAccessible(true);
         disconnect.invoke(manager, new MinecraftDisconnectEvent());

         assertSame(ItemStack.EMPTY, mainHand.get(manager));
         assertSame(ItemStack.EMPTY, offHand.get(manager));
         assertEquals(0, chunkRadius.getInt(manager));
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }
}
