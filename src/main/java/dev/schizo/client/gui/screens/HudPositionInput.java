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
}
