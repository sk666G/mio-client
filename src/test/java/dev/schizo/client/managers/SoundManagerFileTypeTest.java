package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SoundManagerFileTypeTest {
   @Test
   void acceptsOggExtensionRegardlessOfCase() {
      assertTrue(SoundManager.isOggFile(Path.of("sound.ogg")));
      assertTrue(SoundManager.isOggFile(Path.of("sound.OGG")));
      assertTrue(SoundManager.isOggFile(Path.of("sound.OgG")));
   }

   @Test
   void rejectsNonOggAndMisleadingSuffixes() {
      assertFalse(SoundManager.isOggFile(Path.of("sound.ogg.txt")));
      assertFalse(SoundManager.isOggFile(Path.of("soundogg")));
      assertFalse(SoundManager.isOggFile(Path.of("sound.wav")));
      assertFalse(SoundManager.isOggFile(null));
   }
}
