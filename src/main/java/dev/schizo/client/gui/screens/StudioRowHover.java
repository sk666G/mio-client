package dev.schizo.client.gui.screens;

/** Identity-based hover dwell; moving to another row always starts a new delay. */
final class StudioRowHover {
   static final long DELAY_NANOS = 450_000_000L;
   private Object row;
   private long entered;

   boolean ready(Object candidate, long now) {
      if (candidate == null || candidate != row) {
         row = candidate;
         entered = now;
         return false;
      }
      return now - entered >= DELAY_NANOS;
   }

   void clear() { row = null; }
}
