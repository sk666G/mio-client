package dev.schizo.client.gui.screens;

/** Avoid redundant hints for fully visible labels without contextual help. */
final class StudioTooltipText {
   private StudioTooltipText() {}

   static String resolve(String label, boolean clipped, String help) {
      boolean hasLabel = clipped && !label.isBlank();
      boolean hasHelp = help != null && !help.isBlank();
      if (hasLabel && hasHelp) return label + "\n" + help;
      if (hasLabel) return label;
      return hasHelp ? help : null;
   }
}
