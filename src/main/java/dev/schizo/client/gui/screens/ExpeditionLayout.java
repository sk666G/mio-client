package dev.schizo.client.gui.screens;

/** Pure vertical layout for keeping Expedition controls reachable on short viewports. */
final class ExpeditionLayout {
   private static final int ROW_HEIGHT = 34;
   private static final int FULL_HEIGHT = 180;
   private static final int COMPACT_ACTIONS_HEIGHT = 130;

   private ExpeditionLayout() {
   }

   static Geometry forViewportHeight(int viewportHeight) {
      int height = Math.max(0, viewportHeight);
      if (height >= FULL_HEIGHT) {
         int rowsTop = 100;
         int rowsBottom = Math.max(rowsTop, height - 64);
         return new Geometry(
            10, 27, 49, 72,
            rowsTop, rowsBottom,
            Math.max(0, height - 61),
            Math.max(0, height - 49),
            Math.max(0, height - 26),
            true, true, true
         );
      }

      int rowsTop = 60;
      int statusY = Math.max(0, height - 12);
      boolean showActions = height >= COMPACT_ACTIONS_HEIGHT;
      int actionY = Math.max(0, height - 34);
      int rowsBottom = Math.max(rowsTop, (showActions ? actionY : statusY) - 2);
      return new Geometry(
         2, 14, 36, -1,
         rowsTop, rowsBottom,
         statusY,
         actionY,
         -1,
         false, showActions, false
      );
   }

   record Geometry(
      int titleY,
      int searchY,
      int tabsY,
      int secondaryY,
      int rowsTop,
      int rowsBottom,
      int statusY,
      int actionY,
      int pageY,
      boolean showSecondaryControls,
      boolean showActions,
      boolean showPagination
   ) {
      int visibleRows() {
         return Math.max(0, (this.rowsBottom - this.rowsTop) / ROW_HEIGHT);
      }
   }
}
