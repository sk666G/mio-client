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

   record Picker(int left, int top, int width, boolean compact, boolean condensed,
                 int railLeft, int railWidth, int sampleY, int sampleHeight,
                 int firstRowY, int rowStep, int footerY, int panelBottom) {
      int rowY(int channel) { return firstRowY + channel * rowStep; }
   }

   static Picker picker(int width, int height) {
      int w = Math.max(1, Math.min(340, width - 16));
      boolean compact = w < 218 || height < 158;
      boolean condensed = height < 202;
      int body = condensed ? 150 : 194;
      int top = Math.max(4, (height - body) / 2), left = (width - w) / 2;
      return new Picker(left, top, w, compact, condensed, left+62, Math.max(1,w-116),
         top+(condensed?20:22), condensed?16:22, top+(condensed?40:53),
         condensed?22:27, top+(condensed?128:169), top+body);
   }

   /** Shared geometry for read-only review lists with a single footer row. */
   record Review(int left,int top,int width,boolean compact,int rowsY,int rowHeight,
                 int rows,int footerY,int bottom) {
      int rowIndex(double x,double y,int offset,int total) {
         if(compact || !Double.isFinite(x) || !Double.isFinite(y) || x<left || x>=left+width
            || y<rowsY || y>=rowsY+rows*rowHeight) return -1;
         long index=(long)offset+(int)((y-rowsY)/rowHeight);
         return index>=0 && index<total?(int)index:-1;
      }
   }

   static Review health(int width,int height) { return review(width,height,440,38,17,12,210); }
   static Review contextReview(int width,int height) { return review(width,height,560,34,13,17,220); }

   private static Review review(int width,int height,int maxWidth,int header,int rowHeight,int maxRows,int minWidth) {
      int w=Math.max(1,Math.min(maxWidth,width-16));
      boolean compact=w<minWidth || height<header+rowHeight+36;
      int top=Math.max(4,(height-(header+maxRows*rowHeight+32))/2);
      int rows=compact?0:Math.max(1,Math.min(maxRows,(height-top-header-32)/rowHeight));
      int rowsY=top+header, footerY=rowsY+rows*rowHeight+4;
      return new Review((width-w)/2,top,w,compact,rowsY,rowHeight,rows,footerY,footerY+24);
   }

   static int scrollOffset(int offset,int delta,int total,int visible) {
      long maximum=Math.max(0L,(long)total-Math.max(0,visible));
      return (int)Math.max(0L,Math.min(maximum,(long)offset+delta));
   }

   record Benchmark(int left,int top,int width,boolean compact,int graphY,int graphHeight,int footerY,int bottom) {}

   static Benchmark benchmark(int width,int height) {
      int w=Math.max(1,Math.min(560,width-16));
      boolean compact=w<240 || height<146;
      int top=Math.max(4,(height-270)/2);
      int graphY=top+99;
      int graphHeight=height-top-graphY-54;
      if(graphHeight<24) graphHeight=0;
      int footer=graphHeight==0?top+90:graphY+graphHeight+6;
      return new Benchmark((width-w)/2,top,w,compact,graphY,graphHeight,footer,footer+46);
   }

   record ContextEditor(int left,int top,int width,boolean compact,boolean condensed,
                        int firstFieldY,int secondFieldY,int categoryY,int browseY,
                        int previewY,int backY,int noticeY,int noteY,int bottom) {}

   static ContextEditor contextEditor(int width,int height) {
      int w=Math.max(1,Math.min(520,width-16));
      boolean compact=w<240 || height<202;
      boolean condensed=height<224;
      int body=condensed?194:216;
      int top=Math.max(4,(height-body)/2);
      return new ContextEditor((width-w)/2,top,w,compact,condensed,
         top+(condensed?34:41),top+(condensed?61:72),top+(condensed?81:94),
         top+(condensed?103:118),top+(condensed?125:142),top+(condensed?147:166),
         top+(condensed?170:190),top+(condensed?182:202),top+body);
   }

   /** HUD preview preserves a cancellation route even when the ordinary action row cannot fit. */
   record HudPreview(int left,int buttonWidth,int buttonY,int stripBottom,boolean compact) {}

   static HudPreview hudPreview(int width,int height) {
      boolean compact=width<160 || height<58;
      int buttonWidth=Math.max(1,Math.min(100,compact?width-8:(width-24)/2));
      int left=compact?(width-buttonWidth)/2:width/2-buttonWidth-2;
      int y=compact?Math.max(0,Math.min(30,height-20)):30;
      return new HudPreview(left,buttonWidth,y,Math.max(0,Math.min(height,y+25)),compact);
   }
}
