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
}
