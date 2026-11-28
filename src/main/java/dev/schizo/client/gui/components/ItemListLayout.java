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
      return MeasuredControlText.fit(label,width,measure,true);
   }
}
