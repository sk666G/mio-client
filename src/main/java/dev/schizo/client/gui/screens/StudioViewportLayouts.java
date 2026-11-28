package dev.schizo.client.gui.screens;

/** Shared pure viewport geometry for secondary Studio workspaces. */
final class StudioViewportLayouts {
   private StudioViewportLayouts() {}

   record Inspector(int left,int top,int width,boolean compact,boolean condensed,int rowsY,int rows,int controlsY) {}
   record Theme(int left,int top,int width,boolean compact,boolean appliedControls,int previewTop,int previewHeight,int bottom) {}

   static Inspector inspector(int width,int height) {
      int w=Math.max(1,Math.min(550,width-16));
      boolean compact=w<220 || height<170;
      boolean condensed=height<250;
      int top=Math.max(4,(height-320)/2);
      int rowsY=top+(condensed?67:100);
      int rows=compact?0:Math.max(1,Math.min(9,(height-4-rowsY-6-68)/13));
      return new Inspector((width-w)/2,top,w,compact,condensed,rowsY,rows,rowsY+6+rows*13);
   }

   static Theme theme(int width,int height) {
      int w=Math.max(1,Math.min(520,width-16));
      boolean compact=w<240 || height<180;
      boolean applied=!compact && height>=225;
      int top=Math.max(4,(height-(applied?273:250))/2);
      int previewTop=top+(applied?126:103);
      int bottom=Math.min(height-25,top+(applied?249:226));
      int previewHeight=compact?0:Math.max(0,bottom-previewTop-27);
      return new Theme((width-w)/2,top,w,compact,applied,previewTop,previewHeight,bottom);
   }

   record QuickAccess(int left, int top, int width, boolean compact, boolean stacked,
                      int searchY, int rowsY, int rows, int footerY) {}

   static QuickAccess quickAccess(int width, int height) {
      int w = Math.max(1, Math.min(490, width - 16));
      boolean compact = w < 240 || height < 180;
      boolean stacked = w < 390;
      int top = Math.max(4, (height - (stacked ? 291 : 268)) / 2);
      int searchY = top + (stacked ? 71 : 48);
      int rowsY = searchY + 27;
      // Two control rows, status text and a small bottom margin.
      int rows = compact ? 0 : Math.max(1, Math.min(10, (height - rowsY - 64) / 13));
      return new QuickAccess((width - w) / 2, top, w, compact, stacked,
         searchY, rowsY, rows, rowsY + rows * 13);
   }

   record Visited(int left, int top, int width, boolean compact, boolean stacked,
                  int searchY, int rowsY, int rows, int footerY, int statusY, int panelBottom) {}

   static Visited visited(int width, int height) {
      int w = Math.max(1, Math.min(480, width - 16));
      boolean compact = w < 220 || height < 150;
      boolean stacked = w < 360;
      int top = Math.max(4, (height - (stacked ? 273 : 250)) / 2);
      int searchY = top + 30, rowsY = top + 57;
      int footerReserve = stacked ? 64 : 41;
      int rows = compact ? 0 : Math.max(1, Math.min(10, (height - rowsY - footerReserve) / 13));
      int footerY = rowsY + rows * 13;
      int statusY = footerY + (stacked ? 46 : 23);
      return new Visited((width-w)/2, top, w, compact, stacked,
         searchY, rowsY, rows, footerY, statusY, statusY + 15);
   }

   record CustomPalette(int left, int top, int width, boolean compact, int columns,
                        int columnWidth, int fieldWidth, int footerY, int panelBottom) {
      int fieldX(int index) { return left + index % columns * (columnWidth + 6); }
      int fieldY(int index) { return top + 37 + index / columns * 33; }
      int pickerX(int index) { return fieldX(index) + fieldWidth + 4; }
      int swatchX(int index) { return pickerX(index) + 34; }
   }

   static CustomPalette customPalette(int width, int height) {
      int w = Math.max(1, Math.min(460, width - 16));
      boolean single = w < 320 && height >= 270;
      int columns = single ? 1 : 2;
      int bodyHeight = single ? 261 : 162;
      boolean compact = w < (single ? 180 : 240) || height < (single ? 270 : 170);
      int top = Math.max(4, (height - bodyHeight) / 2);
      int column = Math.max(1, (w - (columns - 1) * 6) / columns);
      int footer = top + (single ? 237 : 138);
      return new CustomPalette((width-w)/2, top, w, compact, columns, column,
         Math.max(1, column-52), footer, top+bodyHeight);
   }

   record SavedThemes(int left, int top, int width, boolean compact, boolean condensed,
                      int navigationY, int nameY, int actionsY, int secondaryY,
                      int noticeY, int noteY, int panelBottom) {}

   static SavedThemes savedThemes(int width, int height) {
      int w = Math.max(1, Math.min(480, width - 16));
      boolean compact = w < 240 || height < 152;
      boolean condensed = height < 186;
      int body = condensed ? 144 : 174;
      int top = Math.max(4, (height - body) / 2);
      return new SavedThemes((width-w)/2, top, w, compact, condensed,
         top+(condensed?26:30), top+(condensed?49:65), top+(condensed?72:92),
         top+(condensed?95:115), top+(condensed?118:144),
         top+(condensed?130:156), top+body);
   }
}
