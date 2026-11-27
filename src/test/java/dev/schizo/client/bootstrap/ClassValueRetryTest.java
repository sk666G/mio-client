package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ClassValueRetryTest {
   @Test
   void failedClassValueLookupRetriesThenCachesTheCanonicalInstance() {
      AtomicReference<Object> binding = new AtomicReference<>();
      AtomicInteger computations = new AtomicInteger();
      ClassValue<Object> cache = new ClassValue<>() {
         @Override
         protected Object computeValue(Class<?> key) {
            computations.incrementAndGet();
            Object value = binding.get();
            if (value == null) {
               throw new IllegalStateException("unbound");
            }
            return value;
         }
      };

      Class<?> key = ClassValueRetryTest.class;
      assertThrows(IllegalStateException.class, () -> cache.get(key));
      Object canonical = new Object();
      binding.set(canonical);
      assertSame(canonical, cache.get(key));
      assertSame(canonical, cache.get(key));
      assertEquals(2, computations.get());
   }
}
