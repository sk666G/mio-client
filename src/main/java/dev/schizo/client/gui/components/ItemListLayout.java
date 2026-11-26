package dev.schizo.client.gui.components;

import java.util.function.ToDoubleFunction;

final class ItemListLayout {
   private ItemListLayout() {
   }

   static int entryAt(double y, int offset, int size) {
      if (!(y >= 0 && y < 128) || offset < 0 || offset >= size) {
         return -1;
      }
      int row = (int)y / 16;
      return row < size - offset ? offset + row : -1;
   }

   static String fitLabel(String label, float width, ToDoubleFunction<String> measure) {
      if (measure.applyAsDouble(label) <= width) {
         return label;
      }
      String fitted = "...";
      if (measure.applyAsDouble(fitted) > width) {
         return "";
      }
      for (int end = 0; end < label.length();) {
         end = label.offsetByCodePoints(end, 1);
         String candidate = label.substring(0, end).trim() + "...";
         if (measure.applyAsDouble(candidate) > width) {
            break;
         }
         fitted = candidate;
      }
      return fitted;
   }
}
