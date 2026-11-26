package dev.schizo.client.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

@SuppressWarnings("UnstableApiUsage")
final class GameTestWorlds {
   private static final long HOLD_LIMIT_NANOS = 30_000_000_000L;

   private GameTestWorlds() {
   }

   static TestSingleplayerContext create(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();
      return new TestSingleplayerContext() {
         @Override
         public TestWorldSave getWorldSave() {
            return world.getWorldSave();
         }

         @Override
         public TestClientWorldContext getClientWorld() {
            return world.getClientWorld();
         }

         @Override
         public TestServerContext getServer() {
            return world.getServer();
         }

         @Override
         public void close() {
            holdServerUntilHalted(context);
            world.close();
         }
      };
   }

   private static void holdServerUntilHalted(ClientGameTestContext context) {
      MinecraftServer server = context.computeOnClient(Minecraft::getSingleplayerServer);
      if (server == null) {
         return;
      }

      long deadline = System.nanoTime() + HOLD_LIMIT_NANOS;
      server.execute(
         () -> server.managedBlock(
            () -> !server.isRunning() || System.nanoTime() - deadline > 0L
         )
      );
   }
}
