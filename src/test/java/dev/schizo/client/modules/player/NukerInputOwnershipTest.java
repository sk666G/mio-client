package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NukerInputOwnershipTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void disablingNukerDoesNotReleasePhysicalAttackKey() {
      if (ClientAccess.schizoMinecraft == null || ClientAccess.schizoMinecraft.options == null) {
         return;
      }

      boolean original = ClientAccess.schizoMinecraft.options.keyAttack.isDown();
      ClientAccess.schizoMinecraft.options.keyAttack.setDown(true);
      try {
         NukerModule module = new NukerModule();
         module.onDisable();
         assertTrue(ClientAccess.schizoMinecraft.options.keyAttack.isDown());
      } finally {
         ClientAccess.schizoMinecraft.options.keyAttack.setDown(original);
      }
   }
}
