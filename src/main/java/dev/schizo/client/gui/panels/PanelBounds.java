package dev.schizo.client.gui.panels;

final class PanelBounds {
   private PanelBounds() {
   }

   static int clampPosition(int coordinate, double viewportSize, int panelSize) {
      int limit = (int)Math.max(0, viewportSize - panelSize);
      return Math.clamp(coordinate, 0, limit);
   }
}
