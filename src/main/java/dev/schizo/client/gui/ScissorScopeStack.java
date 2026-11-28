package dev.schizo.client.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Render-thread-confined clipping ownership. Each frame remembers its actual predecessor. */
final class ScissorScopeStack {
   record Snapshot(boolean enabled, ScissorBounds bounds) {}
   private record Entry(ScissorBounds guiBounds, Snapshot previous) {}

   private final Deque<Entry> entries = new ArrayDeque<>();
   private final Supplier<Snapshot> capture;
   private final Consumer<Snapshot> apply;
   private final Function<ScissorBounds, ScissorBounds> convert;

   ScissorScopeStack(Supplier<Snapshot> capture, Consumer<Snapshot> apply,
                     Function<ScissorBounds, ScissorBounds> convert) {
      this.capture = capture;
      this.apply = apply;
      this.convert = convert;
   }

   void push(ScissorBounds requested, boolean intersect) {
      Snapshot previous = capture.get();
      Entry parent = entries.peekLast();
      // Intersect in GUI space first to preserve exact empty/touching edges at fractional scale.
      if (intersect && parent != null) requested = requested.intersect(parent.guiBounds());
      ScissorBounds framebuffer = convert.apply(requested);
      if (intersect && previous.enabled()) framebuffer = framebuffer.intersect(previous.bounds());
      Entry entry = new Entry(requested, previous);
      Snapshot next = new Snapshot(true, framebuffer);
      entries.addLast(entry);
      try {
         apply.accept(next);
      } catch (RuntimeException | Error failure) {
         entries.removeLast();
         try {
            apply.accept(previous);
         } catch (RuntimeException | Error cleanupFailure) {
            if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
         }
         throw failure;
      }
   }

   void pop() {
      Entry entry = entries.getLast();
      // Retain the predecessor if restoration throws, allowing a deliberate retry.
      apply.accept(entry.previous());
      entries.removeLast();
   }
}
