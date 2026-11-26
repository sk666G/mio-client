package dev.schizo.client.gui.screens;

import java.util.Objects;

/** A preview may restore only the exact layout it installed. */
final class HudLayoutPreview<T> {
   private final T before;
   private final T expected;
   private boolean finished;

   HudLayoutPreview(T before, T expected) {
      this.before = Objects.requireNonNull(before);
      this.expected = Objects.requireNonNull(expected);
   }

   boolean commit(T current, HudLayoutHistory<T> history) {
      if (this.finished) return false;
      this.finished = true;
      if (!this.expected.equals(current)) return false;
      if (!this.before.equals(current)) history.record(this.before);
      return true;
   }

   T cancel(T current) {
      if (this.finished) return null;
      this.finished = true;
      return this.expected.equals(current) ? this.before : null;
   }
}
