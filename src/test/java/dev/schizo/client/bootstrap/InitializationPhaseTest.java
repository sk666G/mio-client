package dev.schizo.client.bootstrap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InitializationPhaseTest {
   @Test void completionPublishesOnlyOnce() {
      var phase=new InitializationPhase("test");
      var calls=new AtomicInteger();
      assertThrows(IllegalStateException.class,phase::requireComplete);
      phase.run(calls::incrementAndGet);
      phase.run(calls::incrementAndGet);
      phase.requireComplete();
      assertEquals(1,calls.get());
   }

   @Test void partialFailureRetainsOriginalCauseAndCannotRetry() {
      var phase=new InitializationPhase("test");
      var failure=new IllegalArgumentException("failure");
      assertSame(failure,assertThrows(IllegalArgumentException.class,()->phase.run(()->{throw failure;})));
      var calls=new AtomicInteger();
      assertSame(failure,assertThrows(IllegalStateException.class,()->phase.run(calls::incrementAndGet)).getCause());
      assertSame(failure,assertThrows(IllegalStateException.class,phase::requireComplete).getCause());
      assertEquals(0,calls.get());
   }

   @Test void recursiveStartupDoesNotRunSecondInitializer() {
      var phase=new InitializationPhase("test");
      var calls=new AtomicInteger();
      var failure=assertThrows(IllegalStateException.class,()->phase.run(()->phase.run(calls::incrementAndGet)));
      assertSame(failure,assertThrows(IllegalStateException.class,phase::requireComplete).getCause());
      assertEquals(0,calls.get());
   }

   @Test void concurrentStartupIsRejectedWithoutBlockingOnCallback() throws Exception {
      var phase=new InitializationPhase("test");
      var entered=new CountDownLatch(1);
      var release=new CountDownLatch(1);
      var executor=Executors.newFixedThreadPool(2);
      try {
         var first=executor.submit(()->phase.run(()->{
            entered.countDown();
            try {
               if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
            } catch(InterruptedException e) {
               Thread.currentThread().interrupt();
               throw new IllegalStateException(e);
            }
         }));
         assertTrue(entered.await(5,TimeUnit.SECONDS));
         var second=executor.submit(()->assertThrows(IllegalStateException.class,()->phase.run(()->fail("duplicate"))));
         second.get(2,TimeUnit.SECONDS);
         release.countDown();
         first.get(5,TimeUnit.SECONDS);
         phase.requireComplete();
      } finally {
         release.countDown();
         executor.shutdownNow();
      }
   }
}
