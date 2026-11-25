package dev.schizo.client.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommandPrefixTest {
   @Test
   void preservesFallbackForBlankControlAndWhitespaceInput() {
      assertEquals(";", CommandPrefix.normalize(null, ";"));
      assertEquals(";", CommandPrefix.normalize("", ";"));
      assertEquals(";", CommandPrefix.normalize(" ", ";"));
      assertEquals(";", CommandPrefix.normalize("\n", ";"));
   }

   @Test
   void selectsExactlyOneUnicodeCodePoint() {
      assertEquals("!", CommandPrefix.normalize("!rest", ";"));
      assertEquals("§", CommandPrefix.normalize("§abc", ";"));
      assertEquals("😀", CommandPrefix.normalize("😀suffix", ";"));
   }
}
