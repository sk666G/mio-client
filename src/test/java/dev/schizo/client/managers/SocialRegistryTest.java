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

   @Test
   void reloadReplacesRemovedSocialEntriesAndCollapsesCaseOnlyDuplicates() {
      SocialRegistry registry = new SocialRegistry();
      assertTrue(registry.registerSocial(new SocialEntry("Old", SocialRole.FRIEND)));

      com.google.gson.JsonObject root = new com.google.gson.JsonObject();
      com.google.gson.JsonArray socials = new com.google.gson.JsonArray();

      com.google.gson.JsonObject first = new com.google.gson.JsonObject();
      first.addProperty("name", "Alice");
      first.addProperty("role", "friend");
      socials.add(first);

      com.google.gson.JsonObject duplicate = new com.google.gson.JsonObject();
      duplicate.addProperty("name", "alice");
      duplicate.addProperty("role", "enemy");
      socials.add(duplicate);

      root.add("socials", socials);
      registry.fromJson(root);

      assertNull(registry.getRole("Old"));
      assertEquals(SocialRole.FRIEND, registry.getRole("ALICE"));
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void malformedSocialRootPreservesExistingState() {
      SocialRegistry registry = new SocialRegistry();
      assertTrue(registry.registerSocial(new SocialEntry("Keep", SocialRole.FRIEND)));

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new com.google.gson.JsonObject()));

      assertEquals(SocialRole.FRIEND, registry.getRole("keep"));
      assertEquals(1, registry.getRegistry().size());
   }

}
