package dev.schizo.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded, session-local identity history. No settings values are stored. */
public final class RecentVisits<M,S> {
   public record Visit<M,S>(M owner,S item) {}
   private final int limit;
   private final List<Visit<M,S>> visits = new ArrayList<>();

   public RecentVisits(int limit) {
      if (limit < 1) throw new IllegalArgumentException("Positive history limit required");
      this.limit=limit;
   }

   public void record(M owner,S item) {
      Objects.requireNonNull(owner);
      Objects.requireNonNull(item);
      visits.removeIf(v -> v.owner()==owner && v.item()==item);
      visits.addFirst(new Visit<>(owner,item));
      if (visits.size()>limit) visits.removeLast();
   }

   public List<Visit<M,S>> snapshot() { return List.copyOf(visits); }
   public void clear() { visits.clear(); }
}
