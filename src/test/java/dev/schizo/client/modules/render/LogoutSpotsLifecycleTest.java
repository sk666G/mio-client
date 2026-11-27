package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.options.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class LogoutSpotsLifecycleTest {
   @Test
   void disconnectSessionResetDropsStoredSpotSnapshots() {
      LogoutSpotsModule module = new LogoutSpotsModule();
      LogoutSpotsModule.LogoutSpot spot = new LogoutSpotsModule.LogoutSpot(
         1L,
         null,
         DimensionType.OVERWORLD,
         20,
         0,
         Vec3.ZERO,
         null,
         "server",
         false
      );

      module.putSpot("player", spot);
      assertSame(spot, module.getSpot("player"));

      module.clearSessionState();

      assertNull(module.getSpot("player"));
   }
}
