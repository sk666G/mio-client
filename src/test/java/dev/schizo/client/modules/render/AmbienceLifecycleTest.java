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
}
