package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MusicDisplayTextTest {
   @Test
   void returnsTrimmedTrackTextWithoutPrefix() {
      assertEquals("Artist - Track", MusicDisplayText.format("  Artist - Track  ", false, "ignored"));
   }

   @Test
   void prefixesTrackTextWithoutRegexReplacement() {
      assertEquals("♪ Artist - Track", MusicDisplayText.format("Artist - Track", true, "♪"));
   }

   @Test
   void preservesRegexSpecialCharactersLiterally() {
      assertEquals("$1\\ Artist - Track", MusicDisplayText.format("Artist - Track", true, "$1\\"));
   }

   @Test
   void preservesExistingOuterTrimBehavior() {
      assertEquals("Prefix   Track", MusicDisplayText.format(" Track ", true, " Prefix "));
   }
}
