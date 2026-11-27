package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.FeatureBoxRenderer;
import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ScaffoldLifecycleTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void worldlessCleanupDiscardsCapturedJumpStrength() throws Exception {
      assertNull(ClientAccess.schizoMinecraft.player);
      assertNull(ClientAccess.schizoMinecraft.level);

      FeatureBoxRenderer previousRenderer = ClientContext.featureBoxRenderer;
      ClientContext.featureBoxRenderer = new FeatureBoxRenderer();
      try {
         ScaffoldModule module = new ScaffoldModule();
         Field savedJumpStrength = ScaffoldModule.class.getDeclaredField("savedJumpStrength");
         savedJumpStrength.setAccessible(true);
         savedJumpStrength.setFloat(module, 0.42F);

         assertDoesNotThrow(module::onDisable);
         assertEquals(-1.0F, savedJumpStrength.getFloat(module));
      } finally {
         ClientContext.featureBoxRenderer = previousRenderer;
      }
   }
}
