package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PhaseESPBlockAnimationTest {
   @Test
   void fadesInThenStaysOpaqueWhileFresh() {
      long start = 1_000_000_000L;
      var animation = PhaseESPModule.BlockAnimation.create(start);

      assertEquals(0.0F, animation.getAlpha(start), 0.0001F);
      assertEquals(0.5F, animation.getAlpha(start + PhaseESPModule.BlockAnimation.FADE_NANOS / 2), 0.0001F);
      assertEquals(1.0F, animation.getAlpha(start + PhaseESPModule.BlockAnimation.FADE_NANOS), 0.0001F);
      assertEquals(1.0F, animation.getAlpha(start + 100_000_000L), 0.0001F);
   }

   @Test
   void refreshExtendsExpiryWithoutRestartingFadeIn() {
      long start = 2_000_000_000L;
      var animation = PhaseESPModule.BlockAnimation.create(start);
      long refreshedAt = start + 150_000_000L;
      var refreshed = animation.refresh(refreshedAt);

      assertEquals(start, refreshed.createdAtNanos());
      assertEquals(
         refreshedAt + PhaseESPModule.BlockAnimation.RETENTION_NANOS,
         refreshed.expiresAtNanos()
      );
      assertEquals(1.0F, refreshed.getAlpha(refreshedAt), 0.0001F);
   }

   @Test
   void fadesOutNearExpiryAndExpiresAfterDeadline() {
      long start = 3_000_000_000L;
      var animation = PhaseESPModule.BlockAnimation.create(start);
      long expiry = animation.expiresAtNanos();

      assertEquals(0.5F, animation.getAlpha(expiry - PhaseESPModule.BlockAnimation.FADE_NANOS / 2), 0.0001F);
      assertEquals(0.0F, animation.getAlpha(expiry), 0.0001F);
      assertFalse(animation.isExpired(expiry));
      assertTrue(animation.isExpired(expiry + 1L));
   }

   @Test
   void clockValuesBeforeCreationClampToZeroAge() {
      long start = 4_000_000_000L;
      var animation = PhaseESPModule.BlockAnimation.create(start);

      assertEquals(0.0F, animation.getAlpha(start - 1_000_000L), 0.0001F);
   }
}
