package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TickRateControllerTest {
   private static final float EPSILON = 0.0001F;

   @Test
   void releasingOlderRequestDoesNotClearNewerRequest() {
      TickRateController.Requests requests = new TickRateController.Requests();
      Object olderOwner = new Object();
      Object newerOwner = new Object();

      requests.request(olderOwner, 0.75F);
      requests.request(newerOwner, 1.5F);
      requests.release(olderOwner);

      assertEquals(1.5F, requests.getMultiplier(), EPSILON);
   }

   @Test
   void releasingNewestRequestRestoresMostRecentRemainingOwner() {
      TickRateController.Requests requests = new TickRateController.Requests();
      Object olderOwner = new Object();
      Object newerOwner = new Object();

      requests.request(olderOwner, 0.75F);
      requests.request(newerOwner, 1.5F);
      requests.release(newerOwner);

      assertEquals(0.75F, requests.getMultiplier(), EPSILON);
   }

   @Test
   void refreshingOwnerMovesItToMostRecentPosition() {
      TickRateController.Requests requests = new TickRateController.Requests();
      Object firstOwner = new Object();
      Object secondOwner = new Object();

      requests.request(firstOwner, 0.8F);
      requests.request(secondOwner, 1.4F);
      requests.request(firstOwner, 0.9F);
      requests.release(firstOwner);

      assertEquals(1.4F, requests.getMultiplier(), EPSILON);
   }

   @Test
   void releasingLastRequestRestoresNormalTickRate() {
      TickRateController.Requests requests = new TickRateController.Requests();
      Object owner = new Object();

      requests.request(owner, 1.25F);
      requests.release(owner);

      assertEquals(1.0F, requests.getMultiplier(), EPSILON);
   }

   @Test
   void disconnectClearsEveryActiveRequest() {
      TickRateController.Requests requests = new TickRateController.Requests();

      requests.request(new Object(), 0.5F);
      requests.request(new Object(), 2.0F);
      requests.clear();

      assertEquals(1.0F, requests.getMultiplier(), EPSILON);
   }

   @Test
   void rejectsNonFiniteRequests() {
      TickRateController.Requests requests = new TickRateController.Requests();
      Object owner = new Object();

      assertThrows(IllegalArgumentException.class, () -> requests.request(owner, Float.NaN));
      assertThrows(IllegalArgumentException.class, () -> requests.request(owner, Float.POSITIVE_INFINITY));
   }

   @Test
   void rejectsMissingOwner() {
      TickRateController.Requests requests = new TickRateController.Requests();
      assertThrows(NullPointerException.class, () -> requests.request(null, 1.0F));
   }
}
