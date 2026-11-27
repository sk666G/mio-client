package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.TickRateController;
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
      FeatureBoxRenderer previousRenderer = ClientContext.featureBoxRenderer;
      TickRateController.PlayerTicks previousPlayerTicks = ClientContext.playerTickController;
      FeatureBoxRenderer renderer = new FeatureBoxRenderer();
      TickRateController.PlayerTicks playerTicks = new TickRateController.PlayerTicks();
      ClientContext.featureBoxRenderer = renderer;
      ClientContext.playerTickController = playerTicks;
      try {
         ScaffoldModule module = new ScaffoldModule();
         Field savedJumpStrength = ScaffoldModule.class.getDeclaredField("savedJumpStrength");
         savedJumpStrength.setAccessible(true);
         savedJumpStrength.setFloat(module, 0.42F);

         assertDoesNotThrow(module::onDisable);
         assertEquals(-1.0F, savedJumpStrength.getFloat(module));
      } finally {
         ClientAccess.eventBus.unsubscribe(renderer);
         ClientAccess.eventBus.unsubscribe(playerTicks);
         ClientContext.featureBoxRenderer = previousRenderer;
         ClientContext.playerTickController = previousPlayerTicks;
      }
   }
}
