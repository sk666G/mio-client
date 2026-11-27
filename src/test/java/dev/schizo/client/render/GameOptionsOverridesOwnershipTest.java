package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.NamedFeature;
import dev.schizo.client.settings.NamedEntry;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameOptionsOverridesOwnershipTest {
   @Test
   void cameraOverrideOwnersUseObjectIdentityInsteadOfNameEquality() {
      NamedEntry first = new NamedFeature("FreeLook");
      NamedEntry second = new NamedFeature("FreeLook");
      Set<NamedEntry> owners = GameOptionsOverrides.newCameraOwnerSet();

      assertEquals(first, second);
      assertNotSame(first, second);
      assertTrue(owners.add(first));
      assertTrue(owners.add(second));
      assertEquals(2, owners.size());

      assertTrue(owners.remove(first));
      assertEquals(1, owners.size());
      assertTrue(owners.contains(second));
   }
}
