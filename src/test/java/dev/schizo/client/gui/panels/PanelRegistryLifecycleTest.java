package dev.schizo.client.gui.panels;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.gui.GuiElement;
import org.junit.jupiter.api.Test;

class PanelRegistryLifecycleTest {
   @Test
   void unregisterActuallyRemovesPreviouslyRegisteredChild() {
      Panel panel = new Panel("Test");
      GuiElement child = new GuiElement() {};

      assertTrue(panel.register(child));
      assertTrue(panel.getRegistry().contains(child));
      assertTrue(panel.unregister(child));
      assertFalse(panel.getRegistry().contains(child));
      assertFalse(panel.unregister(child));
   }

   @Test
   void gameStyleRegistryDoesNotDuplicateSameReferenceThroughUnregister() {
      Panel panel = new Panel("Test");
      GuiElement child = new GuiElement() {};

      panel.register(child);
      panel.unregister(child);
      panel.register(child);

      assertEquals(1, panel.getRegistry().stream().filter(entry -> entry == child).count());
   }
}
