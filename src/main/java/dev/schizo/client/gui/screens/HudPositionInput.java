package dev.schizo.client.gui.screens;

/** Exact GUI-pixel coordinates; invalid geometry is rejected rather than silently clamped. */
record HudPositionInput(float x, float y) {
   static HudPositionInput parse(String x, String y) {
      try {
         float px=Float.parseFloat(x.strip()), py=Float.parseFloat(y.strip());
         return Float.isFinite(px) && Float.isFinite(py)?new HudPositionInput(px,py):null;
      } catch(NumberFormatException invalid) { return null; }
   }

   boolean fits(float viewportWidth,float viewportHeight,float widgetWidth,float widgetHeight) {
      return Float.isFinite(x) && Float.isFinite(y)
         && Float.isFinite(viewportWidth) && Float.isFinite(viewportHeight)
         && Float.isFinite(widgetWidth) && Float.isFinite(widgetHeight)
         && viewportWidth>0 && viewportHeight>0 && widgetWidth>0 && widgetHeight>0
         && x>=0 && y>=0 && (double)x+widgetWidth<=viewportWidth && (double)y+widgetHeight<=viewportHeight;
   }

   /** Percent input accepts whole or fractional percentages, never silently clamps. */
   static Float parseScalePercent(String input) {
      try {
         float percent=Float.parseFloat(input.strip());
         return Float.isFinite(percent) && percent>=50F && percent<=200F?percent/100F:null;
      } catch(NumberFormatException invalid) { return null; }
   }

   static boolean scaleFits(float current,float proposed,float x,float y,float w,float h,
                            float viewportWidth,float viewportHeight,boolean anchored) {
      if(!Float.isFinite(current) || current<=0 || !Float.isFinite(proposed)
         || proposed<0.5F || proposed>2F) return false;
      float ratio=proposed/current;
      return new HudPositionInput(anchored?0:x,anchored?0:y)
         .fits(viewportWidth,viewportHeight,w*ratio,h*ratio);
   }
}
