package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.options.DimensionType;
import org.junit.jupiter.api.Test;

class CoordLoggerModuleLifecycleTest {
   @Test
   void disableClearsCachedCoordinates() {
      CoordLoggerModule module = new CoordLoggerModule();
      module.cacheCoordinates(12, 64, -8, DimensionType.OVERWORLD);

      assertNotNull(module.snapshotCoordinates());

      module.onDisable();

      assertNull(module.snapshotCoordinates());
   }

   @Test
   void enableStartsWithFreshSessionCache() {
      CoordLoggerModule module = new CoordLoggerModule();
      module.cacheCoordinates(1, 2, 3, DimensionType.THE_NETHER);

      module.onEnable();

      assertNull(module.snapshotCoordinates());
   }

   @Test
   void capturedSnapshotDoesNotChangeWhenCacheChanges() {
      CoordLoggerModule module = new CoordLoggerModule();
      module.cacheCoordinates(10, 70, 20, DimensionType.OVERWORLD);
      CoordLoggerModule.CoordinateSnapshot snapshot = module.snapshotCoordinates();

      module.cacheCoordinates(-30, 55, 90, DimensionType.THE_END);

      assertNotNull(snapshot);
      assertEquals(10, snapshot.x());
      assertEquals(70, snapshot.y());
      assertEquals(20, snapshot.z());
      assertEquals(DimensionType.OVERWORLD, snapshot.dimension());
   }
}
