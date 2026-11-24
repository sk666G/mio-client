package dev.schizo.client.modules.combat;

/** Deterministic X -> Y -> Z cursor for bounded incremental cube scans. */
final class IncrementalCubeScanCursor {
   record Offset(int x, int y, int z) {
   }

   private final int minimum;
   private final int maximumExclusive;
   private int x;
   private int y;
   private int z;
   private boolean complete;

   IncrementalCubeScanCursor(int minimum, int maximumExclusive) {
      if (maximumExclusive <= minimum) {
         throw new IllegalArgumentException("Scan range must contain at least one offset");
      }
      this.minimum = minimum;
      this.maximumExclusive = maximumExclusive;
      this.x = minimum;
      this.y = minimum;
      this.z = minimum;
   }

   Offset next() {
      if (this.complete) {
         return null;
      }

      Offset current = new Offset(this.x, this.y, this.z);
      this.z++;
      if (this.z >= this.maximumExclusive) {
         this.z = this.minimum;
         this.y++;
         if (this.y >= this.maximumExclusive) {
            this.y = this.minimum;
            this.x++;
            if (this.x >= this.maximumExclusive) {
               this.complete = true;
            }
         }
      }
      return current;
   }

   boolean isComplete() {
      return this.complete;
   }
}
