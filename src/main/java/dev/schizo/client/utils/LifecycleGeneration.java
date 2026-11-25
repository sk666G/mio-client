/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo lifecycle-generation primitive.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Small cancellation primitive for work that is valid only within one lifecycle generation.
 * Invalidating the owner makes every previously captured token stale.
 */
public final class LifecycleGeneration {
   private final AtomicLong generation = new AtomicLong();

   public Token capture() {
      return new Token(this, this.generation.get());
   }

   public long invalidate() {
      return this.generation.incrementAndGet();
   }

   public Runnable guard(Runnable task) {
      Objects.requireNonNull(task, "task");
      Token token = this.capture();
      return () -> {
         if (token.isCurrent()) {
            task.run();
         }
      };
   }

   public static final class Token {
      private final LifecycleGeneration owner;
      private final long generation;

      private Token(LifecycleGeneration owner, long generation) {
         this.owner = owner;
         this.generation = generation;
      }

      public boolean isCurrent() {
         return this.owner.generation.get() == this.generation;
      }
   }
}
