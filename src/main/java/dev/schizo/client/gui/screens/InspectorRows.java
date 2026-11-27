package dev.schizo.client.gui.screens;

/** Shared list viewport math; spacer pixels are not clickable. */
final class InspectorRows {
   private InspectorRows() {}

   static int hit(double x, double y, int left, int top, int width, int rows, int offset, int size) {
      if (!Double.isFinite(x) || !Double.isFinite(y) || x < left || x >= left + width || y < top || y >= top + rows * 13) return -1;
      int local = (int)(y - top);
      if (local % 13 >= 12) return -1;
      int index = offset + local / 13;
      return index >= 0 && index < size ? index : -1;
   }

   static int reveal(int selected, int offset, int rows, int size) {
      if (rows <= 0 || size <= 0) return 0;
      if (selected < offset) offset = selected;
      if (selected >= offset + rows) offset = selected - rows + 1;
      return Math.max(0, Math.min(offset, Math.max(0, size - rows)));
   }
}
