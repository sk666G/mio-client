package dev.schizo.client.gui.screens;

import dev.schizo.client.render.HudGroupAlignment;

/** Shared draw/hit geometry and enable rules for the optional HUD tools overlay. */
final class HudToolbar {
   enum Action {
      GRID("Grid", null, 0), UNDO("Undo", null, 0), REDO("Redo", null, 0), CLEAR("Clear", null, 1),
      LEFT("Left", HudGroupAlignment.Mode.LEFT, 2), CENTER_X("Center X", HudGroupAlignment.Mode.CENTER_X, 2),
      RIGHT("Right", HudGroupAlignment.Mode.RIGHT, 2), TOP("Top", HudGroupAlignment.Mode.TOP, 2),
      CENTER_Y("Center Y", HudGroupAlignment.Mode.CENTER_Y, 2), BOTTOM("Bottom", HudGroupAlignment.Mode.BOTTOM, 2),
      DISTRIBUTE_X("Space X", HudGroupAlignment.Mode.DISTRIBUTE_X, 3), DISTRIBUTE_Y("Space Y", HudGroupAlignment.Mode.DISTRIBUTE_Y, 3),
      BACKGROUND("", null, Integer.MAX_VALUE);

      final String label;
      final HudGroupAlignment.Mode mode;
      final int required;
      Action(String label, HudGroupAlignment.Mode mode, int required) {
         this.label = label;
         this.mode = mode;
         this.required = required;
      }
      boolean enabled(int selected, boolean undo, boolean redo) {
         return this != BACKGROUND && selected >= required && (this != UNDO || undo) && (this != REDO || redo);
      }
   }

   static final Action[] ACTIONS = java.util.Arrays.stream(Action.values()).filter(a -> a != Action.BACKGROUND).toArray(Action[]::new);
   private static final int PAD = 6, GAP = 3, BUTTON_HEIGHT = 18, HEADER = 19;

   record Cell(int x, int y, int width, int height) {
      boolean contains(double px, double py) {
         return px >= x && py >= y && px < x + width && py < y + height;
      }
   }

   record Layout(boolean visible, int x, int y, int width, int height, int columns, int cellWidth) {
      boolean contains(double px, double py) {
         return visible && new Cell(x, y, width, height).contains(px, py);
      }
      Cell cell(Action action) {
         int i = action.ordinal();
         return new Cell(x + PAD + i % columns * (cellWidth + GAP), y + HEADER + i / columns * (BUTTON_HEIGHT + GAP), cellWidth, BUTTON_HEIGHT);
      }
      Action hit(double px, double py) {
         if (!contains(px, py)) return null;
         for (Action action : ACTIONS) if (cell(action).contains(px, py)) return action;
         return null;
      }
   }

   static Layout layout(int viewportWidth, int viewportHeight, boolean requested) {
      if (!requested || viewportWidth < 180 || viewportHeight < 140) return new Layout(false, 0, 0, 0, 0, 1, 0);
      int available = Math.min(480, viewportWidth - 16);
      int columns = Math.max(2, Math.min(6, (available - 2 * PAD + GAP) / (56 + GAP)));
      int cellWidth = (available - 2 * PAD - (columns - 1) * GAP) / columns;
      int rows = (ACTIONS.length + columns - 1) / columns;
      int height = HEADER + rows * BUTTON_HEIGHT + (rows - 1) * GAP + PAD;
      int width = 2 * PAD + columns * cellWidth + (columns - 1) * GAP;
      if (height > viewportHeight / 2) return new Layout(false, 0, 0, 0, 0, 1, 0);
      return new Layout(true, (viewportWidth - width) / 2, viewportHeight - height - 8, width, height, columns, cellWidth);
   }

   private HudToolbar() {}
}
