package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
