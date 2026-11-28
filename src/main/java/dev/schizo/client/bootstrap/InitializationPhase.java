package dev.schizo.client.bootstrap;

import java.util.Objects;

/** A startup phase is published only after success; partial initialization is not retried. */
final class InitializationPhase {
   private enum State { NEW, RUNNING, COMPLETE, FAILED }
   private final String name;
   private State state=State.NEW;
   private Throwable failure;

   InitializationPhase(String name) {this.name=Objects.requireNonNull(name);}

   void run(Runnable operation) {
      Objects.requireNonNull(operation);
      synchronized(this) {
         if(state==State.COMPLETE) return;
         if(state==State.RUNNING) throw new IllegalStateException(name+" initialization already running");
         if(state==State.FAILED) throw new IllegalStateException(name+" initialization previously failed",failure);
         state=State.RUNNING;
      }
      try {
         operation.run();
         synchronized(this) {state=State.COMPLETE;}
      } catch(RuntimeException | Error startupFailure) {
         synchronized(this) {
            failure=startupFailure;
            state=State.FAILED;
         }
         throw startupFailure;
      }
   }

   synchronized void requireComplete() {
      if(state!=State.COMPLETE) throw new IllegalStateException(name+" initialization is not complete",failure);
   }
}
