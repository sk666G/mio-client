package dev.schizo.client.gui;

/** Immutable GUI-space clipping rectangle with nonnegative extents. */
record ScissorBounds(int x, int y, int width, int height) {
   ScissorBounds {
      width = Math.max(0, width);
      height = Math.max(0, height);
   }

   ScissorBounds intersect(ScissorBounds other) {
      int left = Math.max(this.x, other.x);
      int top = Math.max(this.y, other.y);
      long right = Math.min((long)this.x + this.width, (long)other.x + other.width);
      long bottom = Math.min((long)this.y + this.height, (long)other.y + other.height);
      return new ScissorBounds(left, top,
         (int)Math.max(0L, right - left),
         (int)Math.max(0L, bottom - top));
   }

   /** Convert top-left GUI coordinates to clipped, bottom-left framebuffer coordinates. */
   ScissorBounds toFramebuffer(double scale,int framebufferWidth,int framebufferHeight) {
      if(!Double.isFinite(scale)||scale<=0||framebufferWidth<=0||framebufferHeight<=0)
         return new ScissorBounds(0,0,0,0);
      int left=clamp(Math.floor(x*scale),framebufferWidth);
      int right=width==0?left:clamp(Math.ceil(((long)x+width)*scale),framebufferWidth);
      int top=clamp(Math.floor(y*scale),framebufferHeight);
      int bottom=height==0?top:clamp(Math.ceil(((long)y+height)*scale),framebufferHeight);
      return new ScissorBounds(left,framebufferHeight-bottom,right-left,bottom-top);
   }

   private static int clamp(double coordinate,int limit) {
      return (int)Math.max(0,Math.min(limit,coordinate));
   }
}
