package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandPrefixTest {
   @Test
   void preservesSinglePrintablePrefixAndKeepsLegacyFirstCharacterBehavior() {
      assertEquals(";", CommandPrefix.normalize(";"));
      assertEquals(".", CommandPrefix.normalize(".anything"));
   }

   @Test
   void rejectsBlankControlAndSurrogatePairPrefixes() {
      assertThrows(IllegalArgumentException.class, () -> CommandPrefix.normalize(""));
      assertThrows(IllegalArgumentException.class, () -> CommandPrefix.normalize("   "));
      assertThrows(IllegalArgumentException.class, () -> CommandPrefix.normalize("\n"));
      assertThrows(IllegalArgumentException.class, () -> CommandPrefix.normalize("😀"));
   }
}
