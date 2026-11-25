package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.modules.misc.SocialEntry;
import dev.schizo.client.settings.options.SocialRole;
import org.junit.jupiter.api.Test;

class SocialRegistryTest {
   @Test
   void socialNamesUseCaseInsensitiveIdentity() {
      SocialEntry friend = new SocialEntry("Alice", SocialRole.FRIEND);
      SocialEntry enemyWithSameName = new SocialEntry("alice", SocialRole.ENEMY);
      SocialEntry other = new SocialEntry("Bob", SocialRole.ENEMY);

      assertTrue(SocialRegistry.hasSameName(friend, enemyWithSameName));
      assertFalse(SocialRegistry.hasSameName(friend, other));
   }

   @Test
   void nullSocialEntriesNeverMatch() {
      SocialEntry friend = new SocialEntry("Alice", SocialRole.FRIEND);

      assertFalse(SocialRegistry.hasSameName(null, friend));
      assertFalse(SocialRegistry.hasSameName(friend, null));
   }
}
