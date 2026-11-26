package dev.schizo.client.gui.screens;

import java.util.Objects;

/** A preview may restore only the exact layout it installed. */
final class HudLayoutPreview<T> {
   private final T before;
   private final T expected;
   private final java.util.function.BiPredicate<T, T> equivalent;
   private boolean finished;

   HudLayoutPreview(T before, T expected) {
      this(before, expected, Objects::equals);
   }

   HudLayoutPreview(T before, T expected, java.util.function.BiPredicate<T, T> equivalent) {
      this.before = Objects.requireNonNull(before);
      this.expected = Objects.requireNonNull(expected);
      this.equivalent = Objects.requireNonNull(equivalent);
   }

   boolean commit(T current, HudLayoutHistory<T> history) {
      if (this.finished) return false;
      this.finished = true;
      if (!this.equivalent.test(this.expected, current)) return false;
      if (!this.equivalent.test(this.before, current)) history.record(this.before);
      return true;
   }

   T cancel(T current) {
      if (this.finished) return null;
      this.finished = true;
      return this.equivalent.test(this.expected, current) ? this.before : null;
   }
}
