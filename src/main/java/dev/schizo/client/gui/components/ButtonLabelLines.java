package dev.schizo.client.gui.components;

import java.util.List;
import java.util.Objects;

/** One immutable split result per current label; suppliers remain live. */
final class ButtonLabelLines {
   private String label;
   private List<String> lines=List.of("");

   List<String> resolve(String next) {
      if(!Objects.equals(label,next)) {
         label=Objects.requireNonNull(next);
         lines=next.indexOf('\n')<0?List.of(next):List.of(next.split("\\n"));
      }
      return lines;
   }
}
