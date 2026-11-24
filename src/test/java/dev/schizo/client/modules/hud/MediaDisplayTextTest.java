package dev.schizo.client.modules.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.schizo.client.media.MediaSnapshot;
import org.junit.jupiter.api.Test;

class MediaDisplayTextTest {
   @Test
   void formatsOneImmutableSnapshotWithoutCrossUpdateReads() {
      MediaSnapshot snapshot = new MediaSnapshot(MediaSnapshot.PLAYING, "Artist", "Track", 10L, 20L);
      assertEquals("Artist - Track", MediaSessionBridge.getDisplayText(snapshot));
   }

   @Test
   void preservesEmptySnapshotFormatting() {
      assertEquals(" - ", MediaSessionBridge.getDisplayText(MediaSnapshot.empty()));
   }
}
