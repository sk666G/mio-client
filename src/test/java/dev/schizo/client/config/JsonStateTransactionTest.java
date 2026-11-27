package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JsonStateTransactionTest {
   @Test
   void successfulApplyPublishesDesiredState() {
      AtomicReference<JsonObject> state = new AtomicReference<>(json("before"));

      JsonStateTransaction.apply(
         () -> state.get().deepCopy(),
         value -> state.set(value.getAsJsonObject().deepCopy()),
         json("after")
      );

      assertEquals("after", state.get().get("value").getAsString());
   }

   @Test
   void failedApplyRestoresPreviousSnapshot() {
      AtomicReference<JsonObject> state = new AtomicReference<>(json("before"));
      AtomicInteger calls = new AtomicInteger();

      RuntimeException failure = assertThrows(RuntimeException.class, () ->
         JsonStateTransaction.apply(
            () -> state.get().deepCopy(),
            value -> {
               state.set(value.getAsJsonObject().deepCopy());
               if (calls.getAndIncrement() == 0) {
                  throw new IllegalStateException("apply failed");
               }
            },
            json("after")
         )
      );

      assertEquals("apply failed", failure.getMessage());
      assertEquals("before", state.get().get("value").getAsString());
      assertEquals(2, calls.get());
   }

   @Test
   void rollbackFailureIsSuppressedOnOriginalFailure() {
      AtomicInteger calls = new AtomicInteger();

      RuntimeException failure = assertThrows(RuntimeException.class, () ->
         JsonStateTransaction.apply(
            () -> json("before"),
            value -> {
               if (calls.getAndIncrement() == 0) {
                  throw new IllegalStateException("apply failed");
               }
               throw new IllegalArgumentException("rollback failed");
            },
            json("after")
         )
      );

      assertEquals(1, failure.getSuppressed().length);
      assertEquals("rollback failed", failure.getSuppressed()[0].getMessage());
   }

   @Test
   void repeatedFailureInstancePreservesOriginalDiagnostic() {
      IllegalStateException failure = new IllegalStateException("storage unavailable");
      AtomicInteger attempts = new AtomicInteger();

      RuntimeException thrown = assertThrows(RuntimeException.class, () ->
         JsonStateTransaction.apply(
            () -> json("before"),
            value -> {
               attempts.incrementAndGet();
               throw failure;
            },
            json("after")
         )
      );

      assertSame(failure, thrown);
      assertEquals(2, attempts.get());
      assertEquals(0, failure.getSuppressed().length);
   }

   @Test
   void desiredPayloadIsDefensivelyCopied() {
      JsonObject desired = json("after");

      JsonStateTransaction.apply(
         () -> json("before"),
         value -> value.getAsJsonObject().addProperty("value", "mutated"),
         desired
      );

      assertEquals("after", desired.get("value").getAsString());
   }

   @Test
   void rejectsMissingTransactionInputs() {
      JsonObject desired = json("after");
      assertThrows(IllegalArgumentException.class, () -> JsonStateTransaction.apply(null, value -> { }, desired));
      assertThrows(IllegalArgumentException.class, () -> JsonStateTransaction.apply(() -> json("before"), null, desired));
      assertThrows(IllegalArgumentException.class, () -> JsonStateTransaction.apply(() -> json("before"), value -> { }, null));
      assertThrows(IllegalStateException.class, () -> JsonStateTransaction.apply(() -> null, value -> { }, desired));
   }

   private static JsonObject json(String value) {
      JsonObject object = new JsonObject();
      object.addProperty("value", value);
      return object;
   }
}
