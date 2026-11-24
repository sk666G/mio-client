package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OwnedFloatRequestsTest {
   @Test
   void latestActiveOwnerWins() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      requests.request("timer", 1.5F);
      requests.request("step", 0.3F);

      assertEquals(0.3F, requests.get(), 0.000001F);
      assertEquals(2, requests.size());
   }

   @Test
   void releasingOlderOwnerDoesNotStompCurrentOwner() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      requests.request("timer", 1.5F);
      requests.request("step", 0.3F);

      requests.release("timer");

      assertEquals(0.3F, requests.get(), 0.000001F);
   }

   @Test
   void releasingCurrentOwnerRestoresNextMostRecentRequest() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      requests.request("timer", 1.5F);
      requests.request("step", 0.3F);
      requests.request("warp", 4.0F);

      requests.release("warp");
      assertEquals(0.3F, requests.get(), 0.000001F);

      requests.release("step");
      assertEquals(1.5F, requests.get(), 0.000001F);
   }

   @Test
   void repeatedRequestRefreshesOwnerPrecedence() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      requests.request("timer", 1.5F);
      requests.request("step", 0.3F);
      requests.request("timer", 2.0F);

      assertEquals(2.0F, requests.get(), 0.000001F);
      requests.release("timer");
      assertEquals(0.3F, requests.get(), 0.000001F);
   }

   @Test
   void clearRestoresFallbackAndUnknownReleaseIsNoOp() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      requests.request("timer", 2.0F);
      requests.release("missing");
      assertEquals(2.0F, requests.get(), 0.000001F);

      requests.clear();
      assertEquals(1.0F, requests.get(), 0.000001F);
      assertEquals(0, requests.size());
   }

   @Test
   void rejectsNonFiniteRequests() {
      OwnedFloatRequests<String> requests = new OwnedFloatRequests<>(1.0F);
      assertThrows(IllegalArgumentException.class, () -> requests.request("timer", Float.NaN));
      assertThrows(IllegalArgumentException.class, () -> requests.request("timer", Float.POSITIVE_INFINITY));
   }
}
