package dev.schizo.client.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Client-thread, session-local recent accepted picker colors. */
public final class PaletteRecentColors {
   public static final int LIMIT=8;
   private final List<Color> colors=new ArrayList<>();

   public void record(Color color) {
      Objects.requireNonNull(color);
      colors.remove(color);
      colors.addFirst(color);
      if(colors.size()>LIMIT) colors.removeLast();
   }

   public List<Color> snapshot() { return List.copyOf(colors); }
}
