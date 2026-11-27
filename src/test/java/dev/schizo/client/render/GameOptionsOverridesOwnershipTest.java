package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.NamedFeature;
import dev.schizo.client.settings.NamedEntry;
import java.util.Set;
import net.minecraft.client.CameraType;
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

   @Test
   void cameraRestoreRequiresOverrideToStillOwnLiveType() {
      assertTrue(GameOptionsOverrides.shouldRestoreCameraType(
         CameraType.FIRST_PERSON, CameraType.FIRST_PERSON
      ));
      assertFalse(GameOptionsOverrides.shouldRestoreCameraType(
         null, CameraType.FIRST_PERSON
      ));
      assertFalse(GameOptionsOverrides.shouldRestoreCameraType(
         CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK
      ));
   }

}
