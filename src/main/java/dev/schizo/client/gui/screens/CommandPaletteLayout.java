/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

/** Pure viewport fitting for the command palette. */
final class CommandPaletteLayout {
   static final int MAX_VISIBLE_RESULTS = 8;
   static final int RESULT_ROW_HEIGHT = 22;
   static final int HEADER_HEIGHT = 62;
   static final int FOOTER_HEIGHT = 30;
   static final int PREFERRED_WIDTH = 390;
   static final int HORIZONTAL_MARGIN = 12;
   static final int TOP_MARGIN = 8;

   private CommandPaletteLayout() {
   }

   static Layout calculate(int screenWidth, int screenHeight) {
      int safeWidth = Math.max(1, screenWidth);
      int safeHeight = Math.max(1, screenHeight);

      float panelWidth = Math.max(
         1.0F,
         Math.min(PREFERRED_WIDTH, safeWidth - HORIZONTAL_MARGIN * 2.0F)
      );
      float panelLeft = Math.max(0.0F, (safeWidth - panelWidth) / 2.0F);

      int rowSpace = Math.max(
         RESULT_ROW_HEIGHT,
         safeHeight - TOP_MARGIN * 2 - HEADER_HEIGHT - FOOTER_HEIGHT
      );
      int visibleRows = Math.max(
         1,
         Math.min(MAX_VISIBLE_RESULTS, rowSpace / RESULT_ROW_HEIGHT)
      );

      float maximumPanelHeight = HEADER_HEIGHT
         + visibleRows * RESULT_ROW_HEIGHT
         + FOOTER_HEIGHT;
      float desiredTop = Math.max(18.0F, safeHeight * 0.22F);
      float maximumTop = Math.max(TOP_MARGIN, safeHeight - TOP_MARGIN - maximumPanelHeight);
      float panelTop = Math.max(TOP_MARGIN, Math.min(desiredTop, maximumTop));

      return new Layout(panelLeft, panelTop, panelWidth, visibleRows);
   }

   record Layout(float panelLeft, float panelTop, float panelWidth, int visibleRows) {
   }
}
