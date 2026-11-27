package dev.schizo.client.gui.screens;

/** Bounded geometry shared by the Studio draw and pointer paths. */
record StudioLayout(int x, int y, int width, int height, int navWidth, int navRowHeight, int count) {
   static StudioLayout fit(int viewportWidth, int viewportHeight, int count) {
      int w = Math.max(1, Math.min(620, viewportWidth - 16));
      int h = Math.max(1, Math.min(390, viewportHeight - 16));
      int nav = Math.max(1, Math.min(122, w / 3));
      int rows = Math.max(1, count);
      int rowHeight = Math.max(1, Math.min(24, Math.max(0, h - 68) / rows));
      return new StudioLayout(Math.max(0, (viewportWidth - w) / 2),
         Math.max(0, (viewportHeight - h) / 2), w, h, nav, rowHeight, count);
   }
   int right() { return x + width; }
   int bottom() { return y + height; }
   int searchY() { return y + 9; }
   int contentX() { return x + navWidth + 9; }
   int contentY() { return y + 39; }
   int contentWidth() { return Math.max(1, right() - contentX() - 9); }
   int contentHeight() { return Math.max(1, toolsY() - contentY() - 5); }
   int toolsY() { return bottom() - 43; }
   static final int TOOL_COUNT = 5;
   int toolWidth() { return Math.max(1, (contentWidth() - 4 * (TOOL_COUNT - 1)) / TOOL_COUNT); }
   int toolX(int index) { return contentX() + index * (toolWidth() + 4); }
   int toolIndex(double px, double py) {
      if (py < toolsY() || py >= toolsY() + 21) return -1;
      for (int i = 0; i < TOOL_COUNT; i++) {
         if (px >= toolX(i) && px < toolX(i) + toolWidth()) return i;
      }
      return -1;
   }
   int navY(int index) { return y + 43 + index * navRowHeight; }
   int navIndex(double pointerY) {
      double offset = pointerY - navY(0);
      if (offset < 0 || offset >= (double) count * navRowHeight) return -1;
      return (int)(offset / navRowHeight);
   }
}
