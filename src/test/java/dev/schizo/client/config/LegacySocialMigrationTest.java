package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LegacySocialMigrationTest {
   @Test
   void parsesValidFriendsWithoutChangingTheirSpelling() {
      assertEquals(
         List.of("Alice", "Example_123", "Friend Name"),
         LegacySocialMigration.parseFriends("{\"friends\":[\"Alice\",\"Example_123\",\"Friend Name\"]}")
      );
   }

   @Test
   void acceptsAnEmptyFriendsArray() {
      assertEquals(List.of(), LegacySocialMigration.parseFriends("{\"friends\":[]}"));
   }

   @Test
   void rejectsNonObjectJson() {
      assertThrows(IllegalArgumentException.class,
         () -> LegacySocialMigration.parseFriends("[\"Alice\"]"));
   }

   @Test
   void rejectsMissingOrNonArrayFriendsMember() {
      assertThrows(IllegalArgumentException.class, () -> LegacySocialMigration.parseFriends("{}"));
      assertThrows(IllegalArgumentException.class,
         () -> LegacySocialMigration.parseFriends("{\"friends\":\"Alice\"}"));
   }

   @Test
   void rejectsNonStringEntriesBeforeReturningAnyNames() {
      assertThrows(IllegalArgumentException.class,
         () -> LegacySocialMigration.parseFriends("{\"friends\":[\"Alice\",42]}"));
   }

   @Test
   void rejectsBlankNames() {
      assertThrows(IllegalArgumentException.class,
         () -> LegacySocialMigration.parseFriends("{\"friends\":[\"Alice\",\" \" ]}"));
   }
}
