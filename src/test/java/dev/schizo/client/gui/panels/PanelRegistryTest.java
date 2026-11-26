package dev.schizo.client.gui.panels;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.gui.GuiElement;
import org.junit.jupiter.api.Test;

class PanelRegistryTest {
   @Test
   void unregisterRemovesTheExistingChild() {
      Panel panel = new Panel("test");
      GuiElement child = new GuiElement() {};

      assertTrue(panel.register(child));
      assertEquals(1, panel.getRegistry().size());
      assertTrue(panel.unregister(child));
      assertTrue(panel.getRegistry().isEmpty());
      assertFalse(panel.unregister(child));
   }
}
