package dev.schizo.client.gui.panels;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.gui.GuiElement;
import org.junit.jupiter.api.Test;

class PanelRegistryTest {
   @Test
   void unregisterRemovesAnExistingChild() {
      Panel panel = new Panel("Test");
      GuiElement child = new GuiElement() {};

      assertTrue(panel.register(child));
      assertTrue(panel.getRegistry().contains(child));

      assertTrue(panel.unregister(child));
      assertFalse(panel.getRegistry().contains(child));
   }

   @Test
   void unregisterReturnsFalseForUnknownChild() {
      Panel panel = new Panel("Test");
      GuiElement child = new GuiElement() {};

      assertFalse(panel.unregister(child));
      assertTrue(panel.getRegistry().isEmpty());
   }
}
