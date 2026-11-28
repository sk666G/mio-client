package dev.schizo.client.utils.inventory;

/** A selection returning to the same numeric slot does not restore an earlier owner's authority. */
public final class SelectionRevision {
   private long revision;

   public synchronized void changed() {
      this.revision++;
   }

   public synchronized long current() {
      return this.revision;
   }

   public synchronized boolean owns(long revision) {
      return this.revision == revision;
   }
}
