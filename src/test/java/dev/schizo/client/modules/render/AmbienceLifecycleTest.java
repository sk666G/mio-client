package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.render.GameOptionsOverrides;
import net.minecraft.client.OptionInstance;
import org.junit.jupiter.api.Test;

class AmbienceLifecycleTest {
   @Test
   void disabledBrightnessChangeDoesNotTouchLiveGameOptions() {
      AmbienceModule module = new AmbienceModule();

      if (ClientAccess.schizoMinecraft == null || ClientAccess.schizoMinecraft.options == null) {
         assertDoesNotThrow(() ->
            module.brightnessSetting.setValue(AmbienceModule.BrightnessChoice.SCREEN)
         );
         return;
      }

      OptionInstance<Double> gamma = ClientAccess.schizoMinecraft.options.gamma();
      double original = gamma.get();
      double sentinel = original == 0.73D ? 0.61D : 0.73D;
      GameOptionsOverrides.forceSetOptionValue(gamma, sentinel);
      try {
         assertDoesNotThrow(() ->
            module.brightnessSetting.setValue(AmbienceModule.BrightnessChoice.SCREEN)
         );
         assertEquals(sentinel, gamma.get());
      } finally {
         GameOptionsOverrides.forceSetOptionValue(gamma, original);
      }
   }

   @Test
   void weatherSuppressionRestoresStateAfterFailure() {
      AmbienceModule module = new AmbienceModule();
      IllegalStateException failure = new IllegalStateException("render failure");
      assertFalse(module.isWeatherOverrideSuppressed());
      assertSame(failure, assertThrows(IllegalStateException.class,
         () -> module.withWeatherOverrideSuppressed(() -> {
            assertTrue(module.isWeatherOverrideSuppressed());
            throw failure;
         })));
      assertFalse(module.isWeatherOverrideSuppressed());
   }

   @Test
   void nestedWeatherSuppressionPreservesOuterScope() {
      AmbienceModule module = new AmbienceModule();
      Object sentinel = new Object();
      assertSame(sentinel, module.withWeatherOverrideSuppressed(() -> {
         assertTrue(module.isWeatherOverrideSuppressed());
         assertSame(sentinel, module.withWeatherOverrideSuppressed(() -> {
            assertTrue(module.isWeatherOverrideSuppressed());
            return sentinel;
         }));
         assertTrue(module.isWeatherOverrideSuppressed());
         assertThrows(IllegalArgumentException.class,
            () -> module.withWeatherOverrideSuppressed(() -> {
               throw new IllegalArgumentException("nested failure");
            }));
         assertTrue(module.isWeatherOverrideSuppressed());
         return sentinel;
      }));
      assertFalse(module.isWeatherOverrideSuppressed());
   }


   @Test
   void gammaRestoreRequiresAmbienceToStillOwnCurrentValue() {
      assertTrue(AmbienceModule.shouldRestoreGamma(true, 1000.0D, 1000.0D));
      assertFalse(AmbienceModule.shouldRestoreGamma(false, 1000.0D, 1000.0D));
      assertFalse(AmbienceModule.shouldRestoreGamma(true, 0.75D, 1000.0D));
   }

   @Test
   void disablingNonGammaAmbiencePreservesCurrentGamma() {
      if (ClientAccess.schizoMinecraft == null || ClientAccess.schizoMinecraft.options == null) {
         return;
      }

      OptionInstance<Double> gamma = ClientAccess.schizoMinecraft.options.gamma();
      double original = gamma.get();
      AmbienceModule module = new AmbienceModule();
      module.brightnessSetting.setValue(AmbienceModule.BrightnessChoice.NONE);
      module.onEnable();

      double sentinel = original == 0.57D ? 0.64D : 0.57D;
      GameOptionsOverrides.forceSetOptionValue(gamma, sentinel);
      try {
         assertDoesNotThrow(module::onDisable);
         assertEquals(sentinel, gamma.get());
      } finally {
         GameOptionsOverrides.forceSetOptionValue(gamma, original);
      }
   }

}
