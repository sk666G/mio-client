package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.collections.IntPair;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class PositionModuleSessionTest {
   @Test
   void disableAndDisconnectClearFakeCoordinateOffset() throws Exception {
      PositionModule module = new PositionModule();
      Field fakeOffset = PositionModule.class.getDeclaredField("fakeOffset");
      fakeOffset.setAccessible(true);

      fakeOffset.set(module, new IntPair(12, -34));
      module.onDisable();
      assertNull(fakeOffset.get(module));

      fakeOffset.set(module, new IntPair(-56, 78));
      Method disconnect = PositionModule.class.getDeclaredMethod(
         "onMinecraftDisconnectEvent",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());
      assertNull(fakeOffset.get(module));
   }

   @Test
   void redactedTextPreservesPrefixAndColorLayout() {
      assertEquals(
         "XYZ " + net.minecraft.ChatFormatting.WHITE + "REDACTED",
         PositionModule.formatRedactedText("XYZ ")
      );
   }

}
