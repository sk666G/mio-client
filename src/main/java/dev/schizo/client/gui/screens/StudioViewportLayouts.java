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
}
